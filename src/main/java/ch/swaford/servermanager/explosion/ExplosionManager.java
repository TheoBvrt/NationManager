package ch.swaford.servermanager.explosion;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

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

    public static void restoreExplosion(MinecraftServer server, String file) {
        Path path = Path.of("explosions", file);
        try {
            StoredExplosion storedExplosion = ExplosionManager.loadExplosion(path);
            System.out.println(storedExplosion);
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
                if (check.isAir())
                    level.setBlock(blockPos, state, 3);
            }
            Path restoredPath = Path.of("explosions", "restored", file);
            Files.move(path, restoredPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
            Path path = Path.of("explosions", file);
            restoreExplosion(server, file);
        }
    }

    public static void createExplosion(List<BlockState> palette, List<StoredBlock> blocks, ResourceKey<Level> dimension)
    {
        StoredExplosion storedExplosion = new StoredExplosion(palette, blocks, dimension);
        Path folder = Path.of("explosions");
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            String fileName = "EXPL_1" + System.currentTimeMillis() + ".exp";
            Path file = folder.resolve(fileName);
            ExplosionManager.saveExplosion(storedExplosion, file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
