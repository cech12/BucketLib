package de.cech12.bucketlib.mixin;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RepairItemRecipe.class)
public class RepairItemRecipeMixin {

    @Inject(at = @At("RETURN"), method = "assemble", cancellable = true)
    public void getProxy(CraftingInput input, HolderLookup.Provider provider, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack itemstack = cir.getReturnValue();
        if (!itemstack.isEmpty() && itemstack.getItem() instanceof UniversalBucketItem bucketItem && bucketItem.getDurability() > 0) {
            ItemStack newItemstack;
            ItemStack inputBucket = input.getItem(0).getItem() instanceof UniversalBucketItem ? input.getItem(0) : input.getItem(1);
            if (inputBucket.getItem() instanceof UniversalBucketItem) {
                newItemstack = inputBucket.copy();
            } else {
                newItemstack = itemstack.copy();
            }
            newItemstack.remove(DataComponents.MAX_DAMAGE);
            Integer damage = itemstack.getOrDefault(DataComponents.DAMAGE, 0);
            if (damage == 0) {
                newItemstack.remove(DataComponents.DAMAGE);
            } else {
                newItemstack.set(DataComponents.DAMAGE, damage);
            }
            if (itemstack.has(DataComponents.ENCHANTMENTS)) {
                newItemstack.set(DataComponents.ENCHANTMENTS, itemstack.get(DataComponents.ENCHANTMENTS));
            }
            cir.setReturnValue(newItemstack);
        }
    }

}
