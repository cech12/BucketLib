package de.cech12.bucketlib.mixin;

import de.cech12.bucketlib.api.BucketLibTags;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.item.UniversalBucketFluidHandler;
import de.cech12.bucketlib.platform.Services;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.items.wrapper.ShulkerItemStackInvWrapper;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = IItemExtension.class, remap = false)
public interface NeoforgeItemMixin {

    /**
     * @author Cech12
     * @reason BucketLib class re-usability
     */
    @Overwrite
    default int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
        if (this instanceof UniversalBucketItem) {
            //entity buckets should not use the burn time of its fluid
            if (!BucketLibUtil.containsEntityType(itemStack)) {
                Fluid fluid = Services.FLUID.getContainedFluid(itemStack);
                if (fluid != Fluids.EMPTY) {
                    //all fluids have their burn time in their bucket item.
                    //get the burn time via ForgeHooks.getBurnTime to let other mods change burn times of buckets of vanilla and other fluids.
                    return CommonHooks.getBurnTime(new ItemStack(fluid.getBucket()), recipeType);
                }
            }
        }
        return -1;
    }

    /**
     * @author Cech12
     * @reason BucketLib class re-usability
     */
    @Overwrite
    default boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        if (this instanceof UniversalBucketItem) {
            if (enchantment == Enchantments.INFINITY_ARROWS
                    && Services.CONFIG.isInfinityEnchantmentEnabled()
                    && EnchantmentHelper.getTagEnchantmentLevel(Enchantments.INFINITY_ARROWS, stack) <= 0
                    && Services.FLUID.getContainedFluid(stack).defaultFluidState().is(BucketLibTags.Fluids.INFINITY_ENCHANTABLE)) {
                return true;
            }
        }
        return enchantment.category.canEnchant(stack.getItem());
    }

    /**
     * @author Cech12
     * @reason BucketLib class re-usability
     */
    @Overwrite
    default @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        if (this instanceof UniversalBucketItem) {
            return new UniversalBucketFluidHandler(stack);
        }
        return ShulkerItemStackInvWrapper.createDefaultProvider(stack);
    }

}
