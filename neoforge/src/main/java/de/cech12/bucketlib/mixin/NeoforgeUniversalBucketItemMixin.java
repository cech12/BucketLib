package de.cech12.bucketlib.mixin;

import de.cech12.bucketlib.api.BucketLibTags;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.platform.Services;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
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
        //entity buckets should not use the burn time of its fluid
        if (!BucketLibUtil.containsEntityType(itemStack)) {
            Fluid fluid = Services.FLUID.getContainedFluid(itemStack);
            if (fluid != Fluids.EMPTY) {
                return new ItemStack(fluid.getBucket()).getBurnTime(recipeType);
            }
        }
        return super.getBurnTime(itemStack, recipeType);
    }

    @Override
    public boolean canApplyAtEnchantingTable(@NotNull ItemStack stack, @NotNull Enchantment enchantment) {
        if (enchantment == Enchantments.INFINITY
                && Services.CONFIG.isInfinityEnchantmentEnabled()
                && stack.getEnchantmentLevel(Enchantments.INFINITY) <= 0
                && Services.FLUID.getContainedFluid(stack).defaultFluidState().is(BucketLibTags.Fluids.INFINITY_ENCHANTABLE)) {
            return true;
        }
        return super.canApplyAtEnchantingTable(stack, enchantment);
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
