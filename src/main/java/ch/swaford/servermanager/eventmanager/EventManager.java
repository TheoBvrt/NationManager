package ch.swaford.servermanager.eventmanager;

import ballistix.api.event.BlastDamageEvent;
import ballistix.api.event.BlastEvent;
import ballistix.api.event.LaunchEvent;
import ch.swaford.servermanager.ClaimManager;
import ch.swaford.servermanager.FactionManager;
import ch.swaford.servermanager.PlayerDataBase;
import ch.swaford.servermanager.ServerVariable;
import ch.swaford.servermanager.explosion.ExplosionManager;
import ch.swaford.servermanager.logger.CustomLogger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class EventManager {

    //Missile
    @SubscribeEvent
    public void onBlastEvent(BlastEvent.PostBlastEvent event) {
        if (event.world.isClientSide) return;
        ChunkPos chunkPos = new ChunkPos(event.iExplosion.position);
        String claimOwner = ClaimManager.getClaimOwner(chunkPos.x, chunkPos.z);

        ResourceLocation id = event.iExplosion.getBlastType().id();

        if (id.toString().equals("ballistix:obsidian")) {
            ExplosionManager.performExplosion(ServerVariable.obsidianBlastBlockDamageRadius, (ServerLevel) event.world, event.iExplosion.position); ;
        } else if (id.toString().equals("ballistix:thermobaric")) {
            ExplosionManager.performExplosion(ServerVariable.thermobaricBlastBlockDamageRadius, (ServerLevel) event.world, event.iExplosion.position) ;
        }

//        if (claimOwner.equals("server")) {
//            return;
//        }
//        int balance = FactionManager.getBalance(claimOwner);
//        int amount = 0;
//        if (id.toString().equals("ballistix:obsidian")) {
//            amount = (int)(balance * ServerVariable.obsidianBlastMalusPercent);
//        } else if (id.toString().equals("ballistix:thermobaric")) {
//            amount = (int)(balance * ServerVariable.thermobaricBlastMalusPercent);
//        }
//        if (balance >= amount && balance > 0) {
//            FactionManager.subMoney(claimOwner, amount);
//        }
    }

    @SubscribeEvent
    public void onLaunch(LaunchEvent.LaunchMissileEvent event) {
        MinecraftServer server = event.world.getServer();
        ChunkPos chunkPos = new ChunkPos(event.launchPos);
        String claimOwner = ClaimManager.getClaimOwner(chunkPos.x, chunkPos.z);
        CustomLogger.missileLogger(event.launchPos, event.hitPos);
        server.getPlayerList().broadcastSystemMessage(
                Component.literal(String.format(
                        "§7Lancement de missile détecté depuis §e[%s]§7.",
                        claimOwner
                )),
                false // false = message non "system" (pas dans la console)
        );
    }

    @SubscribeEvent
    public void onBlastKill(BlastDamageEvent.BlastDamageMissileEvent event) {
        LivingEntity livingEntity = event.entity;

        if (!(livingEntity instanceof Player target)) return;
        if (PlayerDataBase.playerHasFaction(target.getStringUUID())) {
            String factionName = PlayerDataBase.getPlayerFaction(target.getStringUUID());
            int balance = FactionManager.getBalance(factionName);
            int amount = (int)(balance * ServerVariable.killPlayerMalusPercent);
            if (balance >= amount && balance > 0) {
                FactionManager.subMoney(factionName, amount);
            }
        }
    }

    //Player kill
    @SubscribeEvent
    public void onEntityKilled(LivingDeathEvent event) {
        LivingEntity livingEntity = event.getEntity();
        DamageSource damageSource = event.getSource();
        if (!(damageSource.getEntity() instanceof Player)) return;

        if (!(livingEntity instanceof Player target)) return;

        if (PlayerDataBase.playerHasFaction(target.getStringUUID())) {
            int balance = FactionManager.getBalance(target.getStringUUID());
            int amount = (int)(balance * ServerVariable.killPlayerMalusPercent);
            if (balance >= amount && balance > 0) {
                FactionManager.subMoney(PlayerDataBase.getPlayerFaction(target.getStringUUID()), amount);
            }
        }
    }

    //Player

    private static final Map<UUID, ChunkPos> lastChunkMap = new HashMap<>();
    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        if (!player.serverLevel().dimension().equals(Level.OVERWORLD))
            return;

        ChunkPos currentChunk = player.chunkPosition();

        ChunkPos lastChunk = lastChunkMap.get(player.getUUID());

        if (currentChunk.equals(lastChunk)) {
            return;
        }
        lastChunkMap.put(player.getUUID(), currentChunk);
        updateChunkOwner(player, currentChunk);
    }

    private static void updateChunkOwner(ServerPlayer player, ChunkPos currentChunk) {
        ItemStack item = player.getMainHandItem();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item.getItem());

        if (id.equals(ResourceLocation.parse("ballistix:radargun"))) {
            return;
        }

        if (ClaimManager.checkIfChunkIsClaimed(currentChunk.x, currentChunk.z)) {
            String playerFactionName = PlayerDataBase.getPlayerFaction(player.getStringUUID());
            String chunkFactionName = ClaimManager.getClaimOwner(currentChunk.x, currentChunk.z);

            int powerFaction = FactionManager.getPower(playerFactionName);
            int targetFactionPower = FactionManager.getPower(chunkFactionName);
            int newClaimPrice = ServerVariable.claimBasePrice * (powerFaction / Math.max(1, targetFactionPower));

            if (playerFactionName.equals(chunkFactionName)) {
                player.displayClientMessage(Component.literal("§e" + chunkFactionName), true);
            } else {
                if (targetFactionPower > powerFaction) {
                    player.displayClientMessage(Component.literal("§cClaim impossible"), true);
                } else {
                    player.displayClientMessage(Component.literal("§e" + chunkFactionName
                            + " prix de surclaim : "
                            + NumberFormat.getInstance(Locale.FRENCH).format(newClaimPrice) + "€"), true);
                }
            }
        } else {
            String playerFactionName = PlayerDataBase.getPlayerFaction(player.getStringUUID());
            if (playerFactionName.equals("server"))
                player.displayClientMessage(Component.literal("§flibre"), true);
            else
                player.displayClientMessage(Component.literal("§flibre : "
                        + NumberFormat.getInstance(Locale.FRENCH)
                        .format(FactionManager.getClaimPrice(playerFactionName)) + "€"), true);
        }
    }

}
