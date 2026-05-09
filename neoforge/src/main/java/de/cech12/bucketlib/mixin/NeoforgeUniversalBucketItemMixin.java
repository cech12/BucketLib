package de.cech12.bucketlib.mixin;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = UniversalBucketItem.class, remap = false)
public class NeoforgeUniversalBucketItemMixin extends Item {

    public NeoforgeUniversalBucketItemMixin(Properties properties) {
        super(properties);
    }

    @Override
    public int getBurnTime(@NotNull ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
        if (itemStack.getItem() instanceof UniversalBucketItem universalBucketItem) {
            return universalBucketItem.getBucketBurnTime(itemStack, recipeType);
        }
        return super.getBurnTime(itemStack, recipeType);
    }

    @Override
    public boolean isPrimaryItemFor(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        if (enchantment.is(Enchantments.INFINITY) && BucketLibUtil.isInfinityEnchantmentAllowed(stack)) {
            return true;
        }
        return super.isPrimaryItemFor(stack, enchantment);
    }

    @Override
    public boolean isDamageable(@NotNull ItemStack stack) {
        return stack.isDamageableItem(); //handled by mixin
    }

    @Override
    public int getMaxDamage(@NotNull ItemStack stack) {
        return stack.getMaxDamage(); //handled by mixin
    }
}
