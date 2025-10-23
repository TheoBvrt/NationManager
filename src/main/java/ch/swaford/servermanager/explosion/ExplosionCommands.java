package ch.swaford.servermanager.explosion;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class ExplosionCommands {
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("explosion")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("log")
                        .executes(ctx -> {

                            return 1;
                        })
                )
                .then(Commands.literal("list")
                        .executes(ctx -> {

                            return 1;
                        })
                )
                .then(Commands.literal("status")
                        .executes(ctx -> {

                            return 1;
                        })
                )
                .then(Commands.literal("restore")
                        .then(Commands.argument("file", StringArgumentType.string())
                                .executes(ctx -> {
                                    String filename = ctx.getArgument("file", String.class);
                                    ExplosionManager.restoreExplosion(ctx.getSource().getServer(), filename);
                                    return 1;
                                })
                        )
                )
        );
    }
}
