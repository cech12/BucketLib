package de.cech12.bucketlib.mixin;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DataComponentHolder.class)
public interface DataComponentHolderMixin {

    @Unique
    default Item getItem() {
        if ((Object) this instanceof ItemStack stack) {
            return stack.getItem();
        }
        return null;
    }

    @Inject(at = @At("HEAD"), method = "get", cancellable = true)
    default <T> void getProxy(DataComponentType<? extends T> type, CallbackInfoReturnable<T> cir) {
        if (type == DataComponents.MAX_DAMAGE && this.getItem() instanceof UniversalBucketItem bucketItem && bucketItem.getDurability() > 0) {
            cir.setReturnValue((T) Integer.valueOf(bucketItem.getDurability()));
        }
    }

    @Inject(at = @At("HEAD"), method = "getOrDefault", cancellable = true)
    default <T> void getOrDefaultProxy(DataComponentType<? extends T> type, T defaultValue, CallbackInfoReturnable<T> cir) {
        if (type == DataComponents.MAX_DAMAGE && this.getItem() instanceof UniversalBucketItem bucketItem && bucketItem.getDurability() > 0) {
            cir.setReturnValue((T) Integer.valueOf(bucketItem.getDurability()));
        }
    }

    @Inject(at = @At("HEAD"), method = "has", cancellable = true)
    default void hasProxy(DataComponentType<?> type, CallbackInfoReturnable<Boolean> cir) {
        if ((type == DataComponents.MAX_DAMAGE || type == DataComponents.DAMAGE) && this.getItem() instanceof UniversalBucketItem bucketItem) {
            cir.setReturnValue(bucketItem.getDurability() > 0);
        }
    }

}
