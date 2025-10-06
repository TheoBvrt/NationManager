package ch.swaford.servermanager.clientinterface;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UITools {
    public static List<String> TextFormatter(String description, int maxChar) {
        List<String> list = new ArrayList<>();
        int index = maxChar;

        if (description.length() <= maxChar) {
            list.add(description);
            return list;
        }

        while (description.charAt(index) != ' ' && index > 0) {
            index --;
        }
        if (index == 0) {
            index = maxChar;
        }
        list.add(description.substring(0, index + 1));
        if (description.length() - index <= maxChar) {
            list.add(description.substring(index + 1));
            return list;
        }
        int newIndex = index + 1 + maxChar;
        while (description.charAt(newIndex) != ' ' && newIndex > index + 1) {
            newIndex --;
        }
        if (newIndex == index + 1) {
            newIndex = index + 1 + maxChar;
        }
        list.add(description.substring(index + 1, newIndex + 1));
        list.add(description.substring(newIndex + 1));
        return list;
    }

    public static ItemStack stackFromString(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return new ItemStack(item);
    }

    public static String formatPrice (int price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        DecimalFormat format = new DecimalFormat("#,###.0", symbols);
        return format.format(price) + "€";
    }

    public static int logicalH(double percent) {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        int imgH = 660;
        return (int) Math.round(imgH * percent / guiScale);
    }

    public static int logicalW(double percent) {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        int imgW = 1056;
        return (int) Math.round(imgW * percent / guiScale);
    }
}
