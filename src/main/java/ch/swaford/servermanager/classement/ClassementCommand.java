package ch.swaford.servermanager.classement;

import ch.swaford.servermanager.*;
import ch.swaford.servermanager.classement.ClassementManager;
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
                            String playerFaction = PlayerDataBase.getPlayerFaction(playerUuid);
                            if (!PlayerDataBase.getVote(playerUuid)) {
                                String targetFaction = StringArgumentType.getString(ctx, "nation_name");
                                if (!FactionManager.factionExist(targetFaction)) {
                                    ctx.getSource().sendFailure(
                                            Component.literal("§cNation introuvable")
                                    );
                                    return 0;
                                }
                                if (!playerFaction.equals(targetFaction)) {
                                    PlayerDataBase.setVoteStatus(true, playerUuid);
                                    ClassementManager.addVote(targetFaction);
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("§eVous avez voté pour " + targetFaction), false
                                    );
                                    EconomyManager.addMoney(playerUuid, 150);
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("§eVous avez reçu 150€"), false
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
