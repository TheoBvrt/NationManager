package ch.swaford.servermanager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public class ClaimInteractionProtection {
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (!player.serverLevel().dimension().equals(Level.OVERWORLD)) {
            return ;
        }

        BlockPos pos = event.getPos();
        ChunkPos currentChunk = new ChunkPos(pos);
        String claimFaction = ClaimManager.getClaimOwner(currentChunk.x, currentChunk.z);

        BlockState state = event.getLevel().getBlockState(pos);
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        System.out.println(blockId);
        if (blockId.getNamespace().equals("ballistix") && claimFaction.equals("server") && !player.hasPermissions(2)) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("§cAccès refusé : cet équipement militaire ne se trouve pas sur votre territoire."));
        }

        //if (claimFaction.equals("server")) return;

        String playerFaction = PlayerDataBase.getPlayerFaction(player.getStringUUID());
        if (!claimFaction.equals(playerFaction) && !player.hasPermissions(2)) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("§cZone claimée par " + claimFaction));
        }
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.serverLevel().dimension().equals(Level.OVERWORLD)) {
            return ;
        }

        BlockPos pos = event.getPos();
        ChunkPos currentChunk = new ChunkPos(pos);

        String claimFaction = ClaimManager.getClaimOwner(currentChunk.x, currentChunk.z);
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock());
        if (blockId.getNamespace().equals("ballistix") && claimFaction.equals("server") && !player.hasPermissions(2)) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("§cVous ne pouvez pas déployer d’équipement militaire en dehors de votre territoire."));
        }

        //if (claimFaction.equals("server")) return;

        String playerFaction = PlayerDataBase.getPlayerFaction(player.getStringUUID());
        if (!claimFaction.equals(playerFaction) && !player.hasPermissions(2)) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("§cZone claimée par " + claimFaction));
        }
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.serverLevel().dimension().equals(Level.OVERWORLD)) {
            return ;
        }


        BlockPos pos = event.getPos();
        ChunkPos currentChunk = new ChunkPos(pos);
        String claimFaction = ClaimManager.getClaimOwner(currentChunk.x, currentChunk.z);

        //if (claimFaction.equals("server")) return;

        String playerFaction = PlayerDataBase.getPlayerFaction(player.getStringUUID());
        if (!claimFaction.equals(playerFaction) && !player.hasPermissions(2)) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("§cZone claimée par " + claimFaction));
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        ServerPlayer player = (ServerPlayer) event.getPlayer();
        if (!player.serverLevel().dimension().equals(Level.OVERWORLD)) {
            return ;
        }

        BlockPos pos = event.getPos();
        ChunkPos currentChunk = new ChunkPos(event.getPos());

        String claimFaction = ClaimManager.getClaimOwner(currentChunk.x, currentChunk.z);

        //if (claimFaction.equals("server")) return;

        String playerFaction = PlayerDataBase.getPlayerFaction(player.getStringUUID());
        if (!claimFaction.equals(playerFaction) && !player.hasPermissions(2)) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("§cZone claimée par " + claimFaction));
        }
    }
}
