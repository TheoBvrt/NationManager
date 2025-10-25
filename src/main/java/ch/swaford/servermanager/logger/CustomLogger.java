package ch.swaford.servermanager.logger;

import ch.swaford.servermanager.ClaimManager;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;
import org.slf4j.Logger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CustomLogger {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void newLog(String message, String source, String time) {
        LOGGER.info("(SERVER_MANAGER)[" + time + "][>>" + source.toUpperCase() + "<<] -> " + message);
    }

    public static void logPayload(String message, String source) {
        LocalDateTime now = LocalDateTime.now();
        String formatted = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LOGGER.info("(PAYLOAD_LOGGER)[" + formatted + "][>>" + source.toUpperCase() + "<<] -> " + message);
    }

    public static void missileLogger(BlockPos launch, BlockPos target) {
        LocalDateTime now = LocalDateTime.now();
        String formatted = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        ChunkPos launchChunk = new ChunkPos(launch);
        ChunkPos targetChunk = new ChunkPos(target);

        String launchClaimOwner = ClaimManager.getClaimOwner(launchChunk.x, launchChunk.z);
        if (launchClaimOwner.equals("server")) {
            launchClaimOwner = launch.toShortString();
        }

        String targetClaimOwner = ClaimManager.getClaimOwner(targetChunk.x, targetChunk.z);
        if (targetClaimOwner.equals("server")) {
            targetClaimOwner = target.toShortString();
        }

        LOGGER.info("MISSILE_LOGGER[" + formatted + "][FROM > " + launchClaimOwner + " TARGET > " + targetClaimOwner + "]") ;
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        String command = event.getParseResults().getReader().getString();

        var source = event.getParseResults().getContext().getSource();

        if (source.getEntity() instanceof ServerPlayer player) {
            LocalDateTime now = LocalDateTime.now();
            String formatted = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            newLog(command, player.getGameProfile().getName(), formatted);
        }
    }
}
