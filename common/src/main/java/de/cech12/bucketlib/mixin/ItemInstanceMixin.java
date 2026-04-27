package de.cech12.bucketlib.mixin;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemInstance.class)
public interface ItemInstanceMixin {

    @Inject(at = @At("HEAD"), method = "getMaxStackSize", cancellable = true)
    default void getMaxStackSizeProxy(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof ItemStack stack) {
            if (stack.getItem() instanceof UniversalBucketItem bucketItem) {
                cir.setReturnValue(bucketItem.getMaxStackSize(stack));
            }
        } else if ((Object) this instanceof ItemStackTemplate template) {
            if (template.item().value() instanceof UniversalBucketItem bucketItem) {
                cir.setReturnValue(bucketItem.getMaxStackSize(template.create()));
            }
        }
    }

}
