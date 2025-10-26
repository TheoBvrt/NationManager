package ch.swaford.servermanager;

import ch.swaford.servermanager.classement.ClassementManager;
import ch.swaford.servermanager.classement.ClassementCache;
import ch.swaford.servermanager.explosion.ExplosionManager;
import ch.swaford.servermanager.explosion.StoredBlock;
import ch.swaford.servermanager.explosion.StoredExplosion;
import ch.swaford.servermanager.shop.ShopManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.apache.commons.lang3.RandomUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TimeManager {
    private static List<RestorationState> restorationStates = new ArrayList<>();
    private final static DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    LocalDateTime lastUpdate = null;

    @SubscribeEvent
    public void OnServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 200 == 0) {
            LocalDateTime time = LocalDateTime.now();
            if (lastUpdate == null) return;

            if (time.isAfter(lastUpdate)) {
                action(event.getServer());
                LocalDateTime nextTime = LocalDate.now().plusDays(1).atStartOfDay();
                String parsedTime = nextTime.format(fmt);
                ServerDataManager.setNextUpdate(parsedTime);
                lastUpdate = nextTime;
            }
        }

        if (event.getServer().getTickCount() % 500 == 0) {
            if (restorationStates.isEmpty()) {
                return;
            }

            Iterator<RestorationState> iterator = restorationStates.iterator();

            while (iterator.hasNext()) {
                RestorationState restorationState = iterator.next();

                boolean isFinished = restorationState.restoreNextBlock(event.getServer());

                if (isFinished) {
                    iterator.remove();
                }
            }
        }

        //toutes les 20 secondes
        if (event.getServer().getTickCount() % 400 == 0) {
            //ShopManager.priceFluctuation();
            ClassementCache.SaveClassement();
            ClassementManager.updateNationScore();
            ClassementManager.updateNationList();
        }
    }

    @SubscribeEvent
    public void OnServerStart(ServerStartedEvent event) {

       // ExplosionManager.restoreAll(event.getServer());

        String stored = ServerDataManager.getLastUpdate();
        if (stored == null || stored.equals("0")) {
            LocalDateTime nextTime = LocalDate.now().plusDays(1).atStartOfDay();

            String parsedTime = nextTime.format(fmt);
            ServerDataManager.setNextUpdate(parsedTime);
            lastUpdate = nextTime;
        } else {
            String lastUpdateString = ServerDataManager.getLastUpdate();
            LocalDateTime lastUpdateParsed = LocalDateTime.parse(lastUpdateString, fmt);
            LocalDateTime now =  LocalDateTime.now();
            if (now.isAfter(lastUpdateParsed)) {
                action(event.getServer());
                LocalDateTime nextTime = LocalDate.now().plusDays(1).atStartOfDay();
                String parsedTime = nextTime.format(fmt);
                ServerDataManager.setNextUpdate(parsedTime);
                lastUpdate = nextTime;
            }
        }
        lastUpdate = LocalDateTime.parse(ServerDataManager.getLastUpdate(), fmt);

        List<String> explFile = ExplosionManager.loadAllExplFiles();
        for (String file : explFile) {
            Path path = Path.of("explosions", file);
            try {
                StoredExplosion explosion = ExplosionManager.loadExplosion(path);
                queueExplosion(explosion, file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //tous les jours à minuit
    private void action(MinecraftServer server) {
        ClassementManager.applyScoreReductionRate();
        ServerDataManager.giveSalary(server);
        ShopManager.updateShopItem();
        PlayerDataBase.resetVoteStatus();
        ServerDataManager.clearSession();
    }

    public static void queueExplosion(StoredExplosion explosion, String file) {
        System.out.println("Ajout en queue, d'une nouvelle explosions");
        restorationStates.add(new RestorationState(file, explosion));
    }

    private static class RestorationState {
        private Path path;
        private Path restoredPath;
        private final StoredExplosion explosion;
        int index = 0;

        public RestorationState(String file, StoredExplosion explosion) {
            this.restoredPath = Path.of("explosions", "restored", file);
            this.path = Path.of("explosions", file);
            this.explosion = explosion;
        }

        public boolean restoreNextBlock(MinecraftServer server) {
            List<StoredBlock> blocks = explosion.blockList();
            try {
                if (explosion.palette().isEmpty() || blocks.isEmpty() || index >= blocks.size()) {
                    if (Files.exists(path)) {
                        Files.move(path, restoredPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    return true;
                }

                ServerLevel level = server.getLevel(explosion.dimension());
                if (level == null) {
                    return false;
                }

                while (index < blocks.size()) {
                    StoredBlock current = blocks.get(index);
                    BlockPos pos = new BlockPos(current.x(), current.y(), current.z());

                    if (!level.isLoaded(pos)) return false;

                    BlockState existing = level.getBlockState(pos);
                    if (!existing.isAir() && existing.getBlock() != Blocks.WATER) {
                        index++;
                        continue;
                    }

                    BlockState state = explosion.palette().get(current.paletteId());
                    level.setBlock(pos, state, 3);

                    if (current.nbt() != null) {
                        BlockEntity be = level.getBlockEntity(pos);
                        if (be != null) be.loadWithComponents(current.nbt(), level.registryAccess());
                    }

                    index++;
                    break;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return false;
        }
    }
}
