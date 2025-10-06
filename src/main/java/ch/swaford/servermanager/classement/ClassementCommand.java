package ch.swaford.servermanager.classement;

import ch.swaford.servermanager.classement.ClassementManager;
import ch.swaford.servermanager.FactionManager;
import ch.swaford.servermanager.Message;
import ch.swaford.servermanager.PlayerDataBase;
import ch.swaford.servermanager.networktransfer.ServerClassementPayload;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public class ClassementCommand {
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("vote")
                .then(Commands.argument("nation_name",  StringArgumentType.string())
                        .executes(ctx -> {
                            if (!ch.swaford.servermanager.classement.ClassementManager.getStatusOfClassement("nation_diplomatie"))
                                return 0;
                            String playerUuid = ctx.getSource().getPlayerOrException().getStringUUID();
                            if (PlayerDataBase.playerHasFaction(playerUuid)) {
                                String playerFaction = PlayerDataBase.getPlayerFaction(playerUuid);
                                if (FactionManager.playerIsOpInFaction(playerUuid, playerFaction)) {
                                    if (!FactionManager.getVote(playerFaction)) {
                                        String targetFaction = StringArgumentType.getString(ctx, "nation_name");
                                        if (!playerFaction.equals(targetFaction)) {
                                            FactionManager.setVoteStatus(true, playerFaction);
                                            ClassementManager.addVote(targetFaction);
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("§eVous avez voté pour " + targetFaction), false
                                            );
                                        } else {
                                            ctx.getSource().sendFailure(
                                                    Component.literal("§cVous ne pouvez pas voter en faveur de votre nation")
                                            );
                                            return 0;
                                        }
                                    } else {
                                        ctx.getSource().sendFailure(
                                                Component.literal("§cVous avez déjà utilisé votre voix")
                                        );
                                        return 0;
                                    }
                                } else {
                                    ctx.getSource().sendFailure(
                                            Component.literal("§cVous n'avez pas les permissions pour voter")
                                    );
                                    return 0;
                                }
                            } else {
                                ctx.getSource().sendFailure(
                                        Component.literal(Message.ERROR_PLAYER_DONT_HAVE_FACTION)
                                );
                                return 0;
                            }
                            return 1;
                        })
                )
        );
        dispatcher.register(Commands.literal("classement")
                .then(Commands.literal("create")
                        .requires(c -> c.hasPermission(2))
                        .executes(ctx -> {
                            ClassementData classementData = new ClassementData();
                            ClassementManager.createClassementData(classementData);
                            return 1;
                        })
                )
        );
    }
}
