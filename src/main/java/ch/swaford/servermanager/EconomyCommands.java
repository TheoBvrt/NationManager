package ch.swaford.servermanager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Map;

/*
 * Classe EconomyCommands
 *
 * Cette classe enregistre et gère les commandes liées à l’économie du serveur.
 * Les commandes sont enregistrées via l’événement RegisterCommandsEvent.
 *
 * Commandes principales :
 * - /pay <player> <amount>
 *      → Permet à un joueur d’envoyer de l’argent à un autre joueur.
 *        Vérifie que le joueur a assez de fonds et empêche l’auto-transfert.
 *
 * - /baltop
 *      → Affiche le classement des 10 premiers joueurs en fonction de leur solde.
 *
 * - /balance
 *      → Affiche le solde du joueur exécutant la commande.
 * - /balance <player>
 *      → Affiche le solde d’un autre joueur (permission requise).
 * - /balance set <player> <amount>
 *      → Définit directement le solde d’un joueur (permission niveau 2).
 *
 * - /economy update
 *      → Déclenche une mise à jour de l’économie du serveur :
 *        versement des salaires aux joueurs connectés, via ServerDataManager.giveSalary().
 *
 * Notes :
 * - La logique de gestion des soldes est déléguée à EconomyManager.
 * - Les messages de feedback utilisent des codes de couleur (§).
 * - Les permissions sont contrôlées :
 *      -> /balance set et /economy update nécessitent un niveau d’autorisation élevé (≥ 2).
 */


public class EconomyCommands {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("pay")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                                    ServerPlayer targetPlayer = EntityArgument.getPlayer(ctx, "player");
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    int playerBalance = EconomyManager.getPlayerBalance(sourcePlayer.getStringUUID());
                                    if (sourcePlayer.getStringUUID().equals(targetPlayer.getStringUUID())) {
                                        ctx.getSource().sendFailure(
                                                Component.literal("§cVous ne pouvez pas vous transférer de l’argent")
                                        );
                                        return 0;
                                    }
                                    if (playerBalance < amount) {
                                        ctx.getSource().sendFailure(
                                                Component.literal("§cVous ne disposez pas d’assez d’argent !")
                                        );
                                        return 0;
                                    }
                                    EconomyManager.subMoney(sourcePlayer.getStringUUID(), amount);
                                    EconomyManager.addMoney(targetPlayer.getStringUUID(), amount);
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("§eVous avez envoyé " + amount + "€ à " + targetPlayer.getName().getString()),
                                            false
                                    );
                                    targetPlayer.sendSystemMessage(
                                            Component.literal("§eVous avez reçu ; " + amount + "€ de " + sourcePlayer.getName().getString())
                                    );
                                    return 1;
                                })
                        )
                )
        );

        dispatcher.register(Commands.literal("baltop")
                .requires(src -> src.hasPermission(0))
                .executes(ctx -> {
                    int i = 0;
                    Map<String, Integer> balanceMap = EconomyManager.baltop();
                    String message = "";
                    for (Map.Entry<String, Integer> entry : balanceMap.entrySet()) {
                        if (i >= 10) break;
                        message += entry.getKey() + " : " + entry.getValue() + "\n";
                        i++;
                    }
                    String finalMessage = message;
                    ctx.getSource().sendSuccess(
                            () -> Component.literal("§e" + finalMessage), false
                    );
                    return 1;
                })
        );

        dispatcher.register(Commands.literal("balance")
                .requires(src -> src.hasPermission(0))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    int bal = EconomyManager.getPlayerBalance(player.getStringUUID());
                    ctx.getSource().sendSuccess(
                            () -> Component.literal("§eVotre solde : " + bal + "€"), false
                    );
                    return 1;
                })
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer targetPlayer = EntityArgument.getPlayer(ctx, "player");
                            int bal = EconomyManager.getPlayerBalance(targetPlayer.getStringUUID());
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("§e Solde de " + targetPlayer.getName().getString() + " : " + bal + "€"), false
                            );
                            return 1;
                        })
                )
                .then(Commands.literal("set")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            ServerPlayer targetPlayer = EntityArgument.getPlayer(ctx, "player");
                                            int  amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            EconomyManager.setMoney(targetPlayer.getStringUUID(), amount);
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("§eSolde de " + targetPlayer.getName().getString() + " mis à jour"), false
                                            );
                                            targetPlayer.sendSystemMessage(
                                                    Component.literal("§eVotre solde à été mis à jour : " + EconomyManager.getPlayerBalance(targetPlayer.getStringUUID()) + " euros")
                                            );
                                            return (1);
                                        })
                                )
                        )
                )
        );
        dispatcher.register(Commands.literal("economy")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("update")
                        .executes(ctx -> {
                            MinecraftServer server = ctx.getSource().getServer();
                            ServerDataManager.giveSalary(server);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("update de l'économie"), false
                            );
                            return 1;
                        })
                )
        );
    }
}
