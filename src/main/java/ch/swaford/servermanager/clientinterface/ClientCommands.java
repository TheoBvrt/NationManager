package ch.swaford.servermanager.clientinterface;

import ch.swaford.servermanager.classement.classementinterface.PuissanceInterface;
import ch.swaford.servermanager.networktransfer.RequestClassementData;
import ch.swaford.servermanager.networktransfer.RequestFactionData;
import ch.swaford.servermanager.networktransfer.RequestPlayerData;
import ch.swaford.servermanager.shop.BuyShopInterface;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

public class ClientCommands {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("cnation")
                        .executes(ctx -> {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc.player != null && mc.getConnection() != null) {
                                mc.getConnection().send(new RequestFactionData("server", 0));
                            }
                            return 1;
                        })
                        .then(Commands.argument("nation_name", StringArgumentType.string())
                                .executes(ctx -> {
                                    String factionName = StringArgumentType.getString(ctx, "nation_name");
                                    Minecraft mc = Minecraft.getInstance();
                                    if (mc.player != null && mc.getConnection() != null) {
                                        mc.getConnection().send(new RequestFactionData(factionName, 0));
                                    }
                                    return 1;
                                })
                        )
        );

        event.getDispatcher().register(
                Commands.literal("shop")
                        .executes(ctx -> {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc.player != null && mc.getConnection() != null) {
                                mc.getConnection().send(new RequestPlayerData(3));
                            }
                            return 1;
                        })
        );

        event.getDispatcher().register(
                Commands.literal("classement")
                        .executes(ctx -> {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc.player != null && mc.getConnection() != null) {
                                mc.getConnection().send(new RequestClassementData());
                            }
                            return 1;
                        })
        );
    }
}
