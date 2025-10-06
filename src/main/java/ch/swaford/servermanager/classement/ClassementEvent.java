package ch.swaford.servermanager.classement;

import ch.swaford.servermanager.PlayerDataBase;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public class ClassementEvent {
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!PlayerDataBase.playerHasFaction(player.getStringUUID())) return;

        BlockState blockState = event.getState();
        Block block = blockState.getBlock();
        if (block instanceof CropBlock cropBlock) {
            if (cropBlock.isMaxAge(blockState)) {
                ClassementCache.agricultureCache.merge(player.getStringUUID(), 1, Integer::sum);
            }
        }

        ItemStack itemStack = event.getPlayer().getMainHandItem();
        if (itemStack.isEnchanted()) {
            ItemEnchantments enchantments = itemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            HolderLookup.RegistryLookup<Enchantment> enchRegistry =
                    event.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

            Holder<Enchantment> silkHolder = enchRegistry.getOrThrow(Enchantments.SILK_TOUCH);

            if (enchantments.getLevel(silkHolder) > 0) {
                System.out.println("error");
                return;
            }
        }

        if (blockState.is(Blocks.COAL_ORE) || blockState.is(Blocks.DEEPSLATE_COAL_ORE)) {
            ClassementCache.minageCache.merge(player.getStringUUID(), 1, Integer::sum);
        }
        else if (blockState.is(Blocks.COPPER_ORE) || blockState.is(Blocks.DEEPSLATE_COPPER_ORE)) {
            ClassementCache.minageCache.merge(player.getStringUUID(), 2, Integer::sum);
        }
        else if (blockState.is(Blocks.IRON_ORE) || blockState.is(Blocks.DEEPSLATE_IRON_ORE)) {
            ClassementCache.minageCache.merge(player.getStringUUID(), 3, Integer::sum);
        }
        else if (blockState.is(Blocks.LAPIS_ORE) || blockState.is(Blocks.DEEPSLATE_LAPIS_ORE)) {
            ClassementCache.minageCache.merge(player.getStringUUID(), 4, Integer::sum);
        }
        else if (blockState.is(Blocks.REDSTONE_ORE) || blockState.is(Blocks.DEEPSLATE_REDSTONE_ORE)) {
            ClassementCache.minageCache.merge(player.getStringUUID(), 5, Integer::sum);
        }
        else if (blockState.is(Blocks.GOLD_ORE) || blockState.is(Blocks.DEEPSLATE_GOLD_ORE) || blockState.is(Blocks.NETHER_GOLD_ORE)) {
            ClassementCache.minageCache.merge(player.getStringUUID(), 6, Integer::sum);
        }
        else if (blockState.is(Blocks.NETHER_QUARTZ_ORE)) {
            ClassementCache.minageCache.merge(player.getStringUUID(), 6, Integer::sum);
        }
        else if (blockState.is(Blocks.EMERALD_ORE) || blockState.is(Blocks.DEEPSLATE_EMERALD_ORE)) {
            ClassementCache.minageCache.merge(player.getStringUUID(), 8, Integer::sum);
        }
        else if (blockState.is(Blocks.DIAMOND_ORE) || blockState.is(Blocks.DEEPSLATE_DIAMOND_ORE)) {
            ClassementCache.minageCache.merge(player.getStringUUID(), 10, Integer::sum);
        }
        else if (blockState.is(Blocks.ANCIENT_DEBRIS)) {
            ClassementCache.minageCache.merge(player.getStringUUID(), 20, Integer::sum);
        }
    }

    public static void onSell(ServerPlayer player, int score) {
        if (!PlayerDataBase.playerHasFaction(player.getStringUUID())) return;

        ClassementCache.commerceCache.merge(player.getStringUUID(), score, Integer::sum);
    }
}
