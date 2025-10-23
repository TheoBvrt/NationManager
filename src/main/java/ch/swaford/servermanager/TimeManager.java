package ch.swaford.servermanager;

import ch.swaford.servermanager.classement.ClassementManager;
import ch.swaford.servermanager.classement.ClassementCache;
import ch.swaford.servermanager.explosion.ExplosionManager;
import ch.swaford.servermanager.shop.ShopManager;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.apache.commons.lang3.RandomUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeManager {

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

        if (event.getServer().getTickCount() % 100 == 0) {

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

        ExplosionManager.restoreAll(event.getServer());

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
    }

    //tous les jours à minuit
    private void action(MinecraftServer server) {
        ClassementManager.applyScoreReductionRate();
        ServerDataManager.giveSalary(server);
        ShopManager.updateShopItem();
        PlayerDataBase.resetVoteStatus();
        ServerDataManager.clearSession();
    }
}
