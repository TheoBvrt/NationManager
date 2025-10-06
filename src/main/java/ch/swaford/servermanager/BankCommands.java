package ch.swaford.servermanager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.core.jmx.Server;

import java.util.List;
import java.util.UUID;

/*
 * Classe BankCommands
 *
 * Cette classe gère l’enregistrement et la logique des commandes liées aux comptes bancaires
 * dans le jeu. Les commandes sont enregistrées avec Brigadier via l’évènement RegisterCommandsEvent.
 *
 * Commandes disponibles :
 * - /bank account create <nom>         : Crée un nouveau compte bancaire (max. 2 comptes par joueur).
 * - /bank account delete <nom>         : Supprime un compte appartenant au joueur.
 * - /bank account list                 : Affiche la liste des comptes du joueur et des comptes partagés.
 * - /bank account add <nom> <joueur>   : Ajoute un joueur autorisé sur le compte.
 * - /bank account remove <nom> <joueur>: Retire un joueur d’un compte.
 * - /bank account balance <nom>        : Affiche le solde du compte si le joueur y a accès.
 * - /bank account deposit <nom> <montant>  : Dépose de l’argent sur le compte.
 * - /bank account withdraw <nom> <montant> : Retire de l’argent du compte.
 * - /bank account setowner <nom> <joueur>  : Transfère la propriété d’un compte à un autre joueur.
 *
 * Notes :
 * - Chaque commande retourne un code d’état (succès, échec, ou erreurs spécifiques).
 * - Les accès sont vérifiés via BankManager afin d’assurer que seuls les propriétaires
 *   ou joueurs autorisés peuvent manipuler un compte.
 * - Les messages sont renvoyés au joueur via sendSuccess() ou sendFailure().
 */

public class BankCommands {
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event){
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("bank")
                .requires(source -> source.hasPermission(0))
                .then(Commands.literal("account")
                        .then(Commands.literal("create")
                                .then(Commands.argument("account_name", StringArgumentType.string())
                                        .executes(ctx -> {
                                            String accountName = StringArgumentType.getString(ctx, "account_name");
                                            BankAccountData newBankAccountData = new BankAccountData(
                                                    accountName,
                                                    ctx.getSource().getPlayerOrException().getStringUUID()
                                            );
                                            int state = BankManager.createBankAccount(newBankAccountData);
                                            if (state == 1) {
                                                ctx.getSource().sendSuccess(
                                                        () -> Component.literal("§eVous venez de créer le compte : " + accountName), false
                                                );
                                            } else if (state == 0) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cLa création du compte en banque a échoué")
                                                );
                                            } else if (state == -1) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cVous ne pouvez pas détenir plus de 2 comptes bancaires")
                                                );
                                            }
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("delete")
                                .then(Commands.argument("account_name", StringArgumentType.string())
                                        .executes(ctx -> {
                                            String accountName = StringArgumentType.getString(ctx, "account_name");
                                            ServerPlayer serverPlayer = ctx.getSource().getPlayerOrException();
                                            if (BankManager.deleteBankAccount(accountName, serverPlayer.getStringUUID()) == 1) {
                                                ctx.getSource().sendSuccess(
                                                        () -> Component.literal("§eVous avez supprimé le compte " + accountName), false
                                                );
                                            }
                                            else
                                            {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cError")
                                                );
                                            }
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    List<String> listPlayersAccounts = BankManager.listPlayersAccounts(ctx.getSource().getPlayerOrException().getStringUUID());
                                    List<String> listOtherAccounts = BankManager.listOthersAccounts(ctx.getSource().getPlayerOrException().getStringUUID());
                                    StringBuilder builder = new StringBuilder();
                                    builder.append("§eVos comptes :\n");
                                    for (String accountName : listPlayersAccounts) {
                                        builder.append("- ").append(accountName).append(" : " + BankManager.getAccountBalance(accountName) + "€\n");
                                    }
                                    builder.append("\n§eAutres comptes :\n");
                                    for (String accountName : listOtherAccounts) {
                                        builder.append("- ").append(accountName).append(" : " + BankManager.getAccountBalance(accountName) + "€\n");
                                    }
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal(builder.toString()), false
                                    );
                                    return 1;
                                })
                        )
                        .then(Commands.literal("add")
                                .then(Commands.argument("account_name", StringArgumentType.string())
                                        .then(Commands.argument("player",  EntityArgument.player())
                                                .executes(ctx -> {
                                                    String accountName = StringArgumentType.getString(ctx, "account_name");
                                                    ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                                                    ServerPlayer targetPlayer = EntityArgument.getPlayer(ctx, "player");
                                                    if (BankManager.addAccountMember(
                                                            accountName,
                                                            sourcePlayer.getStringUUID(),
                                                            targetPlayer.getStringUUID()) == 1) {
                                                        ctx.getSource().sendSuccess(
                                                                () -> Component.literal("§eVous avez ajouté " + targetPlayer.getName().getString() + " sur le compte : " + accountName), false
                                                        );
                                                    }
                                                    else {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("§cVous ne pouvez pas gérer un compte dont vous n’êtes pas le propriétaire")
                                                        );
                                                    }
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("account_name", StringArgumentType.string())
                                        .then(Commands.argument("player",  EntityArgument.player())
                                                .executes(ctx -> {
                                                    String accountName = StringArgumentType.getString(ctx, "account_name");
                                                    ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                                                    ServerPlayer targetPlayer = EntityArgument.getPlayer(ctx, "player");
                                                    int state = BankManager.removeAccountMember(accountName, sourcePlayer.getStringUUID(), targetPlayer.getStringUUID());
                                                    if (state == 1) {
                                                        ctx.getSource().sendSuccess(
                                                                () -> Component.literal("§eVous avez retiré " + targetPlayer.getName().getString() + " sur le compte : " + accountName), false
                                                        );
                                                    } else if (state == -1) {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("§cLe joueur n'a pas accès au compte")
                                                        );
                                                    } else {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("§cVous ne pouvez pas gérer un compte dont vous n’êtes pas le propriétaire")
                                                        );
                                                    }
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(Commands.literal("balance")
                                .then(Commands.argument("account_name", StringArgumentType.string())
                                        .executes(ctx -> {
                                            String accountName = StringArgumentType.getString(ctx, "account_name");
                                            ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                                            int balance = BankManager.checkAccountBalance(accountName, sourcePlayer.getStringUUID());
                                            if (balance >= 0) {
                                                ctx.getSource().sendSuccess(
                                                        () -> Component.literal("§eSolde du compte " + accountName + " : " + balance + "€"), false
                                                );
                                            }
                                            else {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cVous ne pouvez pas gérer un compte dont vous n’êtes pas le propriétaire")
                                                );
                                            }
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("deposit")
                                .then(Commands.argument("account_name", StringArgumentType.string())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(ctx -> {
                                                    String accountName = StringArgumentType.getString(ctx, "account_name");
                                                    ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                                    if (BankManager.checkPlayerAccess(accountName, sourcePlayer.getStringUUID())) {
                                                        int state =  BankManager.deposit(accountName, sourcePlayer.getStringUUID(), amount);
                                                        if (state == 1) {
                                                            ctx.getSource().sendSuccess(
                                                                    () -> Component.literal("§eVous venez de déposer " + amount + "€ sur le compte " + accountName), false
                                                            );
                                                        } else if (state == -1) {
                                                            ctx.getSource().sendFailure(
                                                                    Component.literal("§eVous n'avez pas assez d'argent !")
                                                            );
                                                        } else {
                                                            ctx.getSource().sendFailure(
                                                                    Component.literal("§cVous ne pouvez pas gérer un compte dont vous n’êtes pas le propriétaire")
                                                            );
                                                        }
                                                    } else {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("§cVous ne pouvez pas gérer un compte dont vous n’êtes pas le propriétaire")
                                                        );
                                                    }
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(Commands.literal("withdraw")
                                .then(Commands.argument("account_name", StringArgumentType.string())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(ctx -> {
                                                    String accountName = StringArgumentType.getString(ctx, "account_name");
                                                    ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                                    if (BankManager.checkPlayerAccess(accountName, sourcePlayer.getStringUUID())) {
                                                        int state =  BankManager.withdraw(accountName, sourcePlayer.getStringUUID(), amount);
                                                        if (state == 1) {
                                                            ctx.getSource().sendSuccess(
                                                                    () -> Component.literal("§eVous venez de retirer " + amount + "€ sur le compte " + accountName), false
                                                            );
                                                        } else if (state == -1) {
                                                            ctx.getSource().sendFailure(
                                                                    Component.literal("§cLe compte ne dispose pas de suffisamment d’argent !")
                                                            );
                                                        } else {
                                                            ctx.getSource().sendFailure(
                                                                    Component.literal("§cVous ne pouvez pas gérer un compte dont vous n’êtes pas le propriétaire")
                                                            );
                                                        }
                                                    } else {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("§cVous ne pouvez pas gérer un compte dont vous n’êtes pas le propriétaire")
                                                        );
                                                    }
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(Commands.literal("setowner")
                                .then(Commands.argument("account_name", StringArgumentType.string())
                                        .then(Commands.argument("player",  EntityArgument.player())
                                                .executes(ctx -> {
                                                    String accountName = StringArgumentType.getString(ctx, "account_name");
                                                    ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                                                    ServerPlayer targetPlayer = EntityArgument.getPlayer(ctx, "player");
                                                    int state = BankManager.setOwner(accountName, sourcePlayer.getStringUUID(), targetPlayer.getStringUUID());
                                                    if (state == 1) {
                                                        ctx.getSource().sendSuccess(
                                                                () -> Component.literal("§cVous avez transféré la propriété du compte"), false
                                                        );
                                                    } else if (state == 0) {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("§cVous ne pouvez pas gérer un compte dont vous n’êtes pas le propriétaire")
                                                        );
                                                    }
                                                    return 1;
                                                })
                                        )
                                )
                        )
                )
        );
    }
}
