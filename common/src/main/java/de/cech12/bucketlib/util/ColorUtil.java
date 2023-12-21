package de.cech12.bucketlib.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ColorUtil {

    private ColorUtil() {}

    public static int getColorFromRGB(int red, int green, int blue) {
        int rgb = Math.max(Math.min(0xFF, red), 0);
        rgb = (rgb << 8) + Math.max(Math.min(0xFF, green), 0);
        rgb = (rgb << 8) + Math.max(Math.min(0xFF, blue), 0);
        return rgb;
    }

    public static boolean hasColor(ItemStack stack) {
        CompoundTag compoundTag = stack.getTagElement("display");
        return compoundTag != null && compoundTag.contains("color", 99);
    }

    public static int getColor(ItemStack stack) {
        return getColor(stack, 0);
    }

    public static int getColor(ItemStack stack, int defaultColor) {
        CompoundTag compoundTag = stack.getTagElement("display");
        return compoundTag != null && compoundTag.contains("color", 99) ? compoundTag.getInt("color") : defaultColor;
    }

    public static void removeColor(ItemStack stack) {
        CompoundTag compoundTag = stack.getTagElement("display");
        if (compoundTag != null && compoundTag.contains("color")) {
            compoundTag.remove("color");
        }
    }

    public static void setColor(ItemStack stack, int color) {
        stack.getOrCreateTagElement("display").putInt("color", color);
    }

    public static ItemStack dyeItem(ItemStack stack, List<DyeItem> dyes) {
        int[] color = new int[3];
        int i = 0;
        int j = 0;
        ItemStack resultStack = stack.copy();
        resultStack.setCount(1);
        if (hasColor(stack)) {
            int k = getColor(resultStack);
            float f = (float)(k >> 16 & 255) / 255.0F;
            float f1 = (float)(k >> 8 & 255) / 255.0F;
            float f2 = (float)(k & 255) / 255.0F;
            i = (int)((float)i + Math.max(f, Math.max(f1, f2)) * 255.0F);
            color[0] = (int)((float)color[0] + f * 255.0F);
            color[1] = (int)((float)color[1] + f1 * 255.0F);
            color[2] = (int)((float)color[2] + f2 * 255.0F);
            ++j;
        }

        for (DyeItem dyeitem : dyes) {
            float[] afloat = dyeitem.getDyeColor().getTextureDiffuseColors();
            int i2 = (int)(afloat[0] * 255.0F);
            int l = (int)(afloat[1] * 255.0F);
            int i1 = (int)(afloat[2] * 255.0F);
            i += Math.max(i2, Math.max(l, i1));
            color[0] += i2;
            color[1] += l;
            color[2] += i1;
            ++j;
        }

        int j1 = color[0] / j;
        int k1 = color[1] / j;
        int l1 = color[2] / j;
        float f3 = (float)i / (float)j;
        float f4 = (float)Math.max(j1, Math.max(k1, l1));
        j1 = (int)((float)j1 * f3 / f4);
        k1 = (int)((float)k1 * f3 / f4);
        l1 = (int)((float)l1 * f3 / f4);
        int j2 = (j1 << 8) + k1;
        j2 = (j2 << 8) + l1;
        setColor(resultStack, j2);
        return resultStack;
    }
}
