package ch.swaford.servermanager.explosion;

import ch.swaford.servermanager.TimeManager;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ExplosionManager {
    public static void saveExplosion(StoredExplosion snapshot, Path file) throws IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(tmp)))) {

            out.writeUTF("EXP");
            out.writeInt(snapshot.palette().size());
            for (BlockState state : snapshot.palette()) {
                int id = Block.BLOCK_STATE_REGISTRY.getId(state); // état complet (bloc + props)
                out.writeInt(id);
            }

            out.writeInt(snapshot.blockList().size());
            for (StoredBlock block : snapshot.blockList()) {
                out.writeInt(block.x());
                out.writeInt(block.y());
                out.writeInt(block.z());
                out.writeInt(block.paletteId());
                if (block.nbt() != null) {
                    out.writeBoolean(true);
                    NbtIo.write(block.nbt(), out);
                } else {
                    out.writeBoolean(false);
                }

            }

            String dim = snapshot.dimension().location().toString();
            out.writeUTF(dim);
        }
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public static StoredExplosion loadExplosion(Path file) throws IOException {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(file)))) {

            String magic = in.readUTF();
            if (!"EXP".equals(magic)) throw new IOException("Fichier .exp invalide");

            int paletteSize = in.readInt();
            List<BlockState> palette = new ArrayList<>(paletteSize);
            for (int i = 0; i < paletteSize; i++) {
                int id = in.readInt();
                BlockState blockState = Block.BLOCK_STATE_REGISTRY.byId(id);
                if (blockState == null) blockState = Blocks.AIR.defaultBlockState();
                palette.add(blockState);
            }

            int blockCount = in.readInt();
            List<StoredBlock> blocks = new ArrayList<>(blockCount);
            for (int i = 0; i < blockCount; i++) {
                int x = in.readInt();
                int y = in.readInt();
                int z = in.readInt();
                int paletteId = in.readInt();

                CompoundTag nbt = null;
                if (in.readBoolean()) {
                    nbt = NbtIo.read(in);
                }

                blocks.add(new StoredBlock(x, y, z, paletteId, nbt));
            }

            String dimId = in.readUTF();
            ResourceKey<Level> dimension =
                    ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimId));

            return new StoredExplosion(palette, blocks, dimension);
        }
    }

    public static void performExplosion(int radius, ServerLevel level, BlockPos position)
    {
        List<BlockState> palette = new ArrayList<>();
        List<StoredBlock> blocks = new ArrayList<>();
        Set<BlockPos> blockToDestroy = new HashSet<>();

        BlockPos center = position;
        float randomness = 0.85f;
        int maxDepth = 3;
        Set<Block> blacklist = Set.of(
                Blocks.BEDROCK,
                Blocks.END_PORTAL_FRAME,
                Blocks.COMMAND_BLOCK,
                Blocks.BEACON,
                Blocks.ENDER_CHEST,
                Blocks.AIR,
                Blocks.WATER
        );
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dy < -maxDepth) continue;
                    double distanceSq = dx * dx + dy * dy + dz * dz;
                    if (distanceSq <= radius * radius) {

                        BlockPos targetPos = center.offset(dx, dy, dz);
                        BlockState state = level.getBlockState(targetPos);

                        if (!blacklist.contains(state.getBlock())) {
                            if (!palette.contains(state)) {
                                palette.add(state);
                            }
                            int id = palette.indexOf(state);

                            CompoundTag nbt = null;
                            BlockEntity be = level.getBlockEntity(targetPos);
                            if (be != null) {
                                nbt = be.saveWithFullMetadata(level.registryAccess());
                            }

                            blockToDestroy.add(targetPos);
                            blocks.add(new StoredBlock(
                                    targetPos.getX(),
                                    targetPos.getY(),
                                    targetPos.getZ(),
                                    id,
                                    nbt
                            ));
                        }
                    }
                }
            }
        }

        Set<StoredBlock> extraCheck = new HashSet<>();

        for (StoredBlock block : blocks) {
            BlockPos pos = new BlockPos(block.x(),  block.y(), block.z());
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);

                if (blockToDestroy.contains(neighborPos)) continue;

                BlockState state = level.getBlockState(neighborPos);

                if (blacklist.contains(state.getBlock())) continue;

                if (!palette.contains(state)) {
                    palette.add(state);
                }

                int id = palette.indexOf(state);
                CompoundTag nbt = null;
                BlockEntity be = level.getBlockEntity(neighborPos);
                if (be != null) {
                    nbt = be.saveWithFullMetadata(level.registryAccess());
                }

                StoredBlock stored = new StoredBlock(
                        neighborPos.getX(),
                        neighborPos.getY(),
                        neighborPos.getZ(),
                        id,
                        nbt
                );

                extraCheck.add(stored);
            }
        }

        for (StoredBlock block : blocks) {
            BlockPos targetPos = new BlockPos(block.x(), block.y(), block.z());
            if (!blacklist.contains(level.getBlockState(targetPos).getBlock())) {
                level.destroyBlock(targetPos, false);
            }
        }

        for (StoredBlock block : extraCheck) {
            BlockPos targetPos = new BlockPos(block.x(), block.y(), block.z());
            BlockState blockstate = level.getBlockState(targetPos);

            if (blockstate.isAir()) {
                boolean already = blocks.stream().anyMatch(
                        b -> b.x() == targetPos.getX() && b.y() == targetPos.getY() && b.z() == targetPos.getZ()
                );
                if (!already) {
                    blocks.add(block);
                }
            }
        }
        ExplosionManager.createExplosion(palette, blocks, level.dimension(), center);
    }

    public static void restoreExplosion(MinecraftServer server, String file, boolean restoreNbt) {
        Path restoredPath = Path.of("explosions", "restored", file);
        Path path = Path.of("explosions", file);
        try {
            StoredExplosion storedExplosion = ExplosionManager.loadExplosion(path);
            if (storedExplosion.blockList().isEmpty() || storedExplosion.palette().isEmpty()) {
                Files.move(path, restoredPath, StandardCopyOption.REPLACE_EXISTING);
                return;
            }

            BlockPos tmp = new BlockPos(storedExplosion.blockList().getFirst().x(),
                    storedExplosion.blockList().getFirst().y(), storedExplosion.blockList().getFirst().z());

            ServerLevel level = server.getLevel(storedExplosion.dimension());


            int radius = 1;
            ChunkPos center = new ChunkPos(tmp);

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    ChunkPos target = new ChunkPos(center.x + dx, center.z + dz);
                    level.getChunk(target.x, target.z);
                }
            }

            for (StoredBlock block : storedExplosion.blockList()) {
                BlockPos blockPos = new BlockPos(block.x(), block.y(), block.z());
                BlockState check = level.getBlockState(blockPos);
                BlockState state = storedExplosion.palette().get(block.paletteId());
                if (check.isAir() || check.getBlock() == Blocks.WATER)
                    level.setBlock(blockPos, state, 3);
                if (restoreNbt && block.nbt() != null)
                {
                    BlockEntity be = level.getBlockEntity(blockPos);
                    if (be != null) {
                        be.loadWithComponents(block.nbt(), level.registryAccess());
                    }
                }
            }
            Files.move(path, restoredPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String[][] inspectExplosion(Level level, BlockPos pos, int range) {
        List<String[]> list = new ArrayList<>();
        Path folder = Path.of("explosions");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.exp")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String noPrefix = fileName.substring(5, fileName.length() - 4);
                String[] parts = noPrefix.split("_");
                String[] coords = parts[0].split("\\.");
                int x = Integer.parseInt(coords[0]);
                int y = Integer.parseInt(coords[1]);
                int z = Integer.parseInt(coords[2]);

                double dx = x - pos.getX();
                double dy = y - pos.getY();
                double dz = z - pos.getZ();
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

                if (distance > range) continue;

                String position = x + "," + y + "," + z;
                String date = parts[1];
                String time = parts[2] + ":" + parts[3] + ":" + parts[4].substring(0, 2);
                String tmp = "[" + date + "](" + time + ") -> " + position;
                list.add(new String[]{tmp, fileName});
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return list.toArray(new String[list.size()][2]);
    }

    public static void restoreInRange(Level level, BlockPos pos, int range, MinecraftServer server) {
        List<String> list = new ArrayList<>();
        Path folder = Path.of("explosions");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.exp")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String noPrefix = fileName.substring(5, fileName.length() - 4);
                String[] parts = noPrefix.split("_");
                String[] coords = parts[0].split("\\.");
                int x = Integer.parseInt(coords[0]);
                int y = Integer.parseInt(coords[1]);
                int z = Integer.parseInt(coords[2]);

                double dx = x - pos.getX();
                double dy = y - pos.getY();
                double dz = z - pos.getZ();
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

                if (distance > range) continue;
                list.add(fileName);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (String fileName : list) {
            restoreExplosion(server, fileName, true);
        }
    }

    public static void restoreAll(MinecraftServer server)
    {
        List<String> files = new ArrayList<>();

        Path folder = Path.of("explosions");

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.exp")) {
            for (Path path : stream) {
                files.add(path.getFileName().toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String file : files) {
            restoreExplosion(server, file, true);
        }
    }

    public static void createExplosion(List<BlockState> palette, List<StoredBlock> blocks, ResourceKey<Level> dimension, BlockPos center)
    {
        StoredExplosion storedExplosion = new StoredExplosion(palette, blocks, dimension);
        Path folder = Path.of("explosions");
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm_ss");
            String id = center.getX() + "." + center.getY() + "." + center.getZ() + "_" + formatter.format(now) + UUID.randomUUID();
            String fileName = "EXPL_" + id + "_.exp";
            Path file = folder.resolve(fileName);
            ExplosionManager.saveExplosion(storedExplosion, file);
            TimeManager.queueExplosion(storedExplosion, fileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> loadAllExplFiles() {
        List<String> files = new ArrayList<>();

        Path folder = Path.of("explosions");

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.exp")) {
            for (Path path : stream) {
                files.add(path.getFileName().toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return files;
    }

}
