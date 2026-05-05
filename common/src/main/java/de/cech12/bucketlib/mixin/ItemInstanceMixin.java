package de.cech12.bucketlib.mixin;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.world.item.ItemInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemInstance.class)
public interface ItemInstanceMixin {

    @Inject(at = @At("HEAD"), method = "getMaxStackSize", cancellable = true)
    default void getMaxStackSizeProxy(CallbackInfoReturnable<Integer> cir) {
        if (BucketLibUtil.getItem((ItemInstance) this) instanceof UniversalBucketItem bucketItem) {
            cir.setReturnValue(bucketItem.getMaxStackSize((ItemInstance) this));
        }
    }

}
