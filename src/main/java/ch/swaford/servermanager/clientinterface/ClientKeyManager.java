package ch.swaford.servermanager.clientinterface;

import ch.swaford.servermanager.networktransfer.RequestFactionData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;

public class ClientKeyManager {
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event){
        if (ClientSetup.OPEN_FACTION_MENU != null && ClientSetup.OPEN_FACTION_MENU.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.getConnection() != null) {
                mc.getConnection().send(new RequestFactionData("server", 0));
            }
        }
    }
}
