package ch.swaford.servermanager.explosion;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ExplosionCommands {
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("explosion")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("inspect")
                        .then(Commands.argument("range", IntegerArgumentType.integer(5))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    Integer range = IntegerArgumentType.getInteger(ctx, "range");
                                    String[][] list = ExplosionManager.inspectExplosion(ctx.getSource().getLevel(), player.getOnPos(), range);
                                    if (list.length == 0) {
                                        ctx.getSource().sendFailure(
                                                Component.literal("§cAucune donnee")
                                        );
                                        return 1;
                                    }
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("§8§m──────────── §r§7[Résultats des explosions] §8§m────────────"),
                                            false
                                    );
                                    for (String[] entry : list) {
                                        String readable = entry[0]; // [date](heure) -> coordonnées
                                        String fileName = entry[1]; // EXPL_....

                                        Component base = Component.literal( readable + " ")
                                                .withStyle(ChatFormatting.AQUA);

                                        Component clickable = Component.literal("[CPY])")
                                                .withStyle(style -> style
                                                        .withColor(ChatFormatting.YELLOW)
                                                        .withClickEvent(new ClickEvent(
                                                                ClickEvent.Action.COPY_TO_CLIPBOARD, // action = copier
                                                                fileName // contenu copié
                                                        ))
                                                        .withHoverEvent(new HoverEvent(
                                                                HoverEvent.Action.SHOW_TEXT,
                                                                Component.literal("Clique pour copier : " + fileName)
                                                                        .withStyle(ChatFormatting.GRAY)
                                                        ))
                                                );
                                        ctx.getSource().sendSuccess(() -> base.copy().append(clickable), false);
                                    }
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("restore")
                        .then(Commands.argument("range", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    Integer range = IntegerArgumentType.getInteger(ctx, "range");
                                    ExplosionManager.restoreInRange(ctx.getSource().getLevel(), player.getOnPos(), range, ctx.getSource().getServer());
                                    return 1;
                                })
                        )
                        .then(Commands.argument("file", StringArgumentType.string())
                                .executes(ctx -> {
                                    String filename = ctx.getArgument("file", String.class);
                                    ExplosionManager.restoreExplosion(ctx.getSource().getServer(), filename, true);
                                    return 1;
                                })
                        )
                )
        );
    }
}
