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
    private static Enchantment infinityEnchantment;

    @Unique
    private boolean isInfinityEnchantment() {
        if (infinityEnchantment == null) {
            infinityEnchantment = VanillaRegistries.createLookup().lookup(Registries.ENCHANTMENT).get().getOrThrow(Enchantments.INFINITY).value();
        }
        return ((Enchantment)(Object)this).description().equals(infinityEnchantment.description());
    }

    @Inject(method = "canEnchant", at = @At("RETURN"), cancellable = true)
    private void getBurnDurationProxy(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (isInfinityEnchantment() && BucketLibUtil.isInfinityEnchantmentAllowed(itemStack)) {
            cir.setReturnValue(true);
        }
    }

}
