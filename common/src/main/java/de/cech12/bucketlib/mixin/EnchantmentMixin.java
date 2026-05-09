package de.cech12.bucketlib.mixin;

import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Enchantment.class})
public class EnchantmentMixin {

    @Unique
    private static Enchantment bucketLib$infinityEnchantment;

    @Unique
    private boolean bucketLib$isInfinityEnchantment() {
        if (bucketLib$infinityEnchantment == null) {
            bucketLib$infinityEnchantment = VanillaRegistries.createLookup().lookup(Registries.ENCHANTMENT).get().getOrThrow(Enchantments.INFINITY).value();
        }
        return ((Enchantment)(Object)this).description().equals(bucketLib$infinityEnchantment.description());
    }

    @Inject(method = "isSupportedItem", at = @At("RETURN"), cancellable = true)
    private void isSupportedItemProxy(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && bucketLib$isInfinityEnchantment() && BucketLibUtil.isInfinityEnchantmentAllowed(itemStack)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "canEnchant", at = @At("RETURN"), cancellable = true)
    private void canEnchantProxy(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && bucketLib$isInfinityEnchantment() && BucketLibUtil.isInfinityEnchantmentAllowed(itemStack)) {
            cir.setReturnValue(true);
        }
    }

}