package de.cech12.bucketlib.mixin;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for the Item class to enable the possibility to configure the item's durability and hook into other hidden functionality.
 */
@Mixin(Item.class)
public class ItemMixin {

    @Unique
    private boolean bucketLib$isUniversalBucket() {
        return ((Object) this) instanceof UniversalBucketItem;
    }

    @Unique
    private int bucketLib$getDurability() {
        if (((Object) this) instanceof UniversalBucketItem bucketItem) {
            return bucketItem.getDurability();
        }
        return ((Item) (Object) this).getMaxDamage();
    }

    /**
     * Injection method to initialize the maxDamage value.
     * @param cir CallbackInfoReturnable
     */
    @Inject(at = @At("HEAD"), method = "getMaxDamage", cancellable = true)
    public void getMaxDamageProxy(CallbackInfoReturnable<Integer> cir) {
        if (bucketLib$isUniversalBucket()) {
            cir.setReturnValue(bucketLib$getDurability());
            cir.cancel();
        }
    }

    /**
     * Injection method to initialize the maxDamage value.
     * @param cir CallbackInfoReturnable
     */
    @Inject(at = @At("HEAD"), method = "canBeDepleted", cancellable = true)
    public void canBeDepletedProxy(CallbackInfoReturnable<Boolean> cir) {
        if (bucketLib$isUniversalBucket()) {
            cir.setReturnValue(bucketLib$getDurability() > 0);
            cir.cancel();
        }
    }

    /**
     * Injection method to initialize the maxDamage value.
     * @param stack ItemStack
     * @param cir CallbackInfoReturnable
     */
    @Inject(at = @At("HEAD"), method = "getBarWidth", cancellable = true)
    public void getBarWidthProxy(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (bucketLib$isUniversalBucket()) {
            cir.setReturnValue(Math.round(13.0F - (float)stack.getDamageValue() * 13.0F / (float)bucketLib$getDurability()));
            cir.cancel();
        }
    }

    /**
     * Injection method to initialize the maxDamage value.
     * @param stack ItemStack
     * @param cir CallbackInfoReturnable
     */
    @Inject(at = @At("HEAD"), method = "getBarColor")
    public void getBarColorProxy(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (bucketLib$isUniversalBucket()) {
            int durability = bucketLib$getDurability();
            float x = Math.max(0.0F, ((float)durability - (float)stack.getDamageValue()) / (float)durability);
            cir.setReturnValue(Mth.hsvToRgb(x / 3.0F, 1.0F, 1.0F));
            cir.cancel();
        }
    }

}
