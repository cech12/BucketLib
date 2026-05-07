package de.cech12.bucketlib.mixin;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.DyeRecipe;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingRecipe.class)
public interface CraftingRecipeMixin {

    @Inject(at = @At("RETURN"), method = "getRemainingItems", cancellable = true)
    default void getRemainingItemsProxy(CraftingInput input, CallbackInfoReturnable<NonNullList<ItemStack>> cir) {
        if (this instanceof DyeRecipe || this instanceof RepairItemRecipe) {
            NonNullList<ItemStack> list = cir.getReturnValue();
            boolean changed = false;
            for (int i = 0; i < list.size(); i++) {
                ItemStack stack = list.get(i);
                if (stack.getItem() instanceof UniversalBucketItem) {
                    list.set(i, ItemStack.EMPTY); //for bucket coloring we don't want to return the remainder
                    changed = true;
                }
            }
            if (changed) {
                cir.setReturnValue(list);
            }
        }
    }
}
