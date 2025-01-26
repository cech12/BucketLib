package de.cech12.bucketlib.mixin;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.FuelValues;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({FuelValues.class})
public abstract class FuelValuesMixin {

    @Inject(method = "burnDuration", at = @At("RETURN"), cancellable = true)
    private void getBurnDurationProxy(ItemStack itemStack, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValue() <= 0 && itemStack.getItem() instanceof UniversalBucketItem universalBucketItem)  {
            cir.setReturnValue(universalBucketItem.getBucketBurnTime(itemStack, null));
        }
    }

    @Inject(method = "isFuel", at = @At("RETURN"), cancellable = true)
    private void isFuelProxy(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && itemStack.getItem() instanceof UniversalBucketItem universalBucketItem)  {
            cir.setReturnValue(universalBucketItem.getBucketBurnTime(itemStack, null) > 0);
        }
    }

}
