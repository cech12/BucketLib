package de.cech12.bucketlib.mixin;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for the Item class to enable the possibility to configure the item's durability.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements DataComponentHolder {

    @Shadow public abstract Item getItem();

    @Inject(at = @At("HEAD"), method = "getMaxDamage", cancellable = true)
    public void getMaxDamageProxy(CallbackInfoReturnable<Integer> cir) {
        if (this.getItem() instanceof UniversalBucketItem bucketItem) {
            cir.setReturnValue(bucketItem.getDurability());
        }
    }

    @Inject(at = @At("HEAD"), method = "isDamageableItem", cancellable = true)
    public void isDamageableItemProxy(CallbackInfoReturnable<Boolean> cir) {
        if (this.getItem() instanceof UniversalBucketItem bucketItem) {
            cir.setReturnValue(bucketItem.getDurability() > 0 && !this.has(DataComponents.UNBREAKABLE));
        }
    }

    @Inject(at = @At("HEAD"), method = "getMaxStackSize", cancellable = true)
    public void getMaxStackSizeProxy(CallbackInfoReturnable<Integer> cir) {
        if (this.getItem() instanceof UniversalBucketItem bucketItem) {
            cir.setReturnValue(bucketItem.getMaxStackSize((ItemStack) (Object) this));
        }
    }

}
