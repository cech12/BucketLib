package de.cech12.bucketlib.mixin;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.world.inventory.FurnaceFuelSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({FurnaceFuelSlot.class})
public class FurnaceFuelSlotMixin {

    @Inject(at = @At("HEAD"), method = "isBucket", cancellable = true)
    private static void isBucketProxy(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (!itemStack.isEmpty() && itemStack.getItem() instanceof UniversalBucketItem bucketItem
                && BucketLibUtil.isEmpty(itemStack) && bucketItem.canHoldFluid(Fluids.WATER)
        ) {
            cir.setReturnValue(true);
        }
    }

}
