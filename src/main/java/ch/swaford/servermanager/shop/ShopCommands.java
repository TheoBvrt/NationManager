package ch.swaford.servermanager.shop;

import ch.swaford.servermanager.EconomyManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;

public class ShopCommands {
    private static final SuggestionProvider<CommandSourceStack> CATEGORY_SUGGESTIONS =
            (context, builder) -> {
                List<String> suggestions = new ArrayList<>(ShopManager.getShopCategories());
                for (String category : suggestions) {
                    builder.suggest(category);
                }
                return builder.buildFuture();
            };

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("shop")
                .then(Commands.literal("add")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("category", StringArgumentType.word())
                                .suggests(CATEGORY_SUGGESTIONS)
                                .then(Commands.argument("base_price", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("quantity", IntegerArgumentType.integer(0))
                                                .executes(ctx -> {
                                                    int basePrice = IntegerArgumentType.getInteger(ctx, "base_price");
                                                    int quantity = IntegerArgumentType.getInteger(ctx, "quantity");
                                                    String category = StringArgumentType.getString(ctx, "category");
                                                    if (!ShopManager.getShopCategories().contains(category)) {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("§eCategory dosen't exist!")
                                                        );
                                                        return 0;
                                                    }
                                                    ItemStack currentItem =  ctx.getSource().getPlayerOrException().getMainHandItem();
                                                    if (!ShopManager.itemIsRegistered(BuiltInRegistries.ITEM.getKey(currentItem.getItem()).toString())) {
                                                        addItem(currentItem, basePrice, category, quantity, false, 0);
                                                        ctx.getSource().sendSuccess(
                                                                () -> Component.literal("§eItem ajouté au shop"),
                                                                false
                                                        );
                                                        return 1;
                                                    } else {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("§eItem déjà enregistré")
                                                        );
                                                        return 0;
                                                    }
                                                })
                                                .then(Commands.argument("limited", BoolArgumentType.bool())
                                                        .then(Commands.argument("max_quantity", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> {
                                                                    int maxQuantity = IntegerArgumentType.getInteger(ctx, "max_quantity");
                                                                    int basePrice = IntegerArgumentType.getInteger(ctx, "base_price");
                                                                    int quantity = IntegerArgumentType.getInteger(ctx, "quantity");
                                                                    String category = StringArgumentType.getString(ctx, "category");
                                                                    if (!ShopManager.getShopCategories().contains(category)) {
                                                                        ctx.getSource().sendFailure(
                                                                                Component.literal("§eCategory dosen't exist!")
                                                                        );
                                                                        return 0;
                                                                    }
                                                                    ItemStack currentItem =  ctx.getSource().getPlayerOrException().getMainHandItem();
                                                                    if (!ShopManager.itemIsRegistered(BuiltInRegistries.ITEM.getKey(currentItem.getItem()).toString())) {
                                                                        addItem(currentItem, basePrice, category, quantity, true, maxQuantity);
                                                                        ctx.getSource().sendSuccess(
                                                                                () -> Component.literal("§eItem ajouté au shop"),
                                                                                false
                                                                        );
                                                                        return 1;
                                                                    } else {
                                                                        ctx.getSource().sendFailure(
                                                                                Component.literal("§eItem déjà enregistré")
                                                                        );
                                                                        return 0;
                                                                    }
                                                                })
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            ShopManager.updateShopItem();
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("§eReload su shop"),
                                    false
                            );
                            return 1;
                        })
                )
                .then(Commands.literal("reloadmili")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            ShopManager.reloadShopItemCategory("militaire");
                            return 1;
                        })
                )
                .then(Commands.literal("sell")
                        .then(Commands.argument("item", StringArgumentType.string())
                                .then(Commands.argument("quantity", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            int quantity = IntegerArgumentType.getInteger(ctx, "quantity");
                                            String itemName =  StringArgumentType.getString(ctx, "item");
                                            ResourceLocation id = ResourceLocation.tryParse(itemName);
                                            Item item = BuiltInRegistries.ITEM.get(id);
                                            if (item == Items.AIR) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§eErreur")
                                                );
                                                return 0;
                                            }
                                            if (!ShopManager.isItemAvailableForSell(item)) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§eCette item n'est actuellement pas vendable")
                                                );
                                                return 0;
                                            }
                                            int maxQuantity = ShopManager.getQuantity(item);
                                            if (quantity > maxQuantity || (quantity != 10 && quantity != maxQuantity)) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cQuantité invalide")
                                                );
                                                return 0;
                                            }
                                            if (ShopManager.getCountOfItem(player, item) < quantity) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§eVous ne posséder pas assez d'item à vendre")
                                                );
                                                return 0;
                                            }
                                            if (ShopManager.getSellPrice(item, quantity) <= 0) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cQuantité invalide")
                                                );
                                                return 0;
                                            }
                                            ShopManager.sellItem(player, item, quantity);
                                            return 1;
                                        })
                                )
                        )
                )
                .then(Commands.literal("buy")
                        .then(Commands.argument("item", StringArgumentType.string())
                                .then(Commands.argument("quantity", IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String itemName =  StringArgumentType.getString(ctx, "item");
                                            int  quantity = IntegerArgumentType.getInteger(ctx, "quantity");
                                            ResourceLocation id = ResourceLocation.tryParse(itemName);
                                            Item item = BuiltInRegistries.ITEM.get(id);
                                            if (item == Items.AIR) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§eErreur")
                                                );
                                                return 0;
                                            }
                                            if (!ShopManager.isItemAvailableForBuy(item)) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§eCette item n'est actuellement pas achetable")
                                                );
                                                return 0;
                                            }
                                            int maxQuantity = ShopManager.getQuantity(item);
                                            if (quantity > maxQuantity || (quantity != 1 && quantity != 10 && quantity != maxQuantity)) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cQuantité invalide")
                                                );
                                                return 0;
                                            }
                                            if (EconomyManager.getPlayerBalance(player.getStringUUID()) < ShopManager.getBuyPrice(item, quantity)) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§eVous n'avez pas assez d'argent pour acheter'")
                                                );
                                                return 0;
                                            }
                                            ShopManager.buyItem(player, item, quantity);
                                            return 1;
                                        })
                                )
                        )
                )
                .then(Commands.literal("resetprice")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            ShopManager.resetPrice();
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("§eReset du shop"),
                                    false
                            );
                            return 1;
                        })
                )
        );
    }
    
    private void addItem(ItemStack newItem, int basePrice, String category, int quantity, boolean limited, int maxQuantity) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(newItem.getItem());
        ShopItemData shopItemData = new ShopItemData(itemId.toString(), basePrice,  category, quantity, limited, maxQuantity);
        ShopManager.addShopItem(shopItemData);
    }
}
