package de.cech12.bucketlib.util;

import net.minecraft.world.item.ItemStack;

public class ItemStackUtil {

    public static ItemStack copyStackWithSize(ItemStack stack, int size) {
        if (size == 0) {
            return ItemStack.EMPTY;
        } else {
            ItemStack copy = stack.copy();
            copy.setCount(size);
            return copy;
        }
    }

}
