package de.cech12.bucketlib.mixin;

import net.minecraft.world.item.crafting.CraftingRecipe;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CraftingRecipe.class)
public interface CraftingRecipeMixin {

    /* TODO
    @Inject(at = @At("RETURN"), method = "getRemainingItems", cancellable = true)
    default void getRemainingItems(CraftingInput craftingInput, CallbackInfoReturnable<NonNullList<ItemStack>> cir) {
        if ((Object)this instanceof ArmorDyeRecipe) {
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
     */
}
