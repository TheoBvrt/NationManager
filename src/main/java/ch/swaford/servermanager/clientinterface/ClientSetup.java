package ch.swaford.servermanager.clientinterface;

import com.mojang.blaze3d.platform.InputConstants;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.model.MapPolygon;
import journeymap.api.v2.client.model.ShapeProperties;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;

public class ClientSetup {
    public static KeyMapping OPEN_FACTION_MENU;

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        Minecraft mc = Minecraft.getInstance();
        long handle = mc.getWindow().getWindow();

        org.lwjgl.glfw.GLFW.glfwSetWindowSizeLimits(
                handle,
                1280, 720,
                org.lwjgl.glfw.GLFW.GLFW_DONT_CARE,
                org.lwjgl.glfw.GLFW.GLFW_DONT_CARE
        );
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        OPEN_FACTION_MENU = new KeyMapping(
                "open_nation_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "NationRP"
        );
        event.register(OPEN_FACTION_MENU);
    }
}
