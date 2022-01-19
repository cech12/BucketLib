package cech12.bucketlib.util;

import cech12.bucketlib.api.BucketLibTags;
import cech12.bucketlib.config.ServerConfig;
import cech12.bucketlib.item.UniversalBucketItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class BucketLibUtil {

    public static final ResourceLocation MILK_LOCATION = new ResourceLocation("milk");

    private BucketLibUtil() {}

    public static boolean isEmpty(ItemStack itemStack) {
        return !containsFluid(itemStack) && !containsMilk(itemStack);
    }

    public static boolean containsFluid(ItemStack itemStack) {
        AtomicBoolean containsFluid = new AtomicBoolean(false);
        FluidUtil.getFluidContained(itemStack).ifPresent(fluidStack -> containsFluid.set(!fluidStack.isEmpty()));
        return containsFluid.get();
    }

    /**
     * Checks if the given bucket is affected by Infinity enchantment.
     * @param itemStack checked item stack
     * @return boolean
     */
    public static boolean isAffectedByInfinityEnchantment(@Nonnull ItemStack itemStack) {
        if (!ServerConfig.INFINITY_ENCHANTMENT_ENABLED.get()) {
            return false;
        }
        if (itemStack.getItem() instanceof UniversalBucketItem bucket) {
            Fluid fluid = FluidUtil.getFluidContained(itemStack).orElse(FluidStack.EMPTY).getFluid();
            return fluid.is(BucketLibTags.Fluids.INFINITY_ENCHANTABLE)
                    && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, itemStack) > 0
                    && bucket.canHoldFluid(fluid);
        }
        return false;
    }

    public static ResourceLocation getContent(ItemStack itemStack) {
        CompoundTag nbt = itemStack.getTag();
        if (nbt != null && nbt.contains("BucketContent")) {
            return new ResourceLocation(nbt.getString("BucketContent"));
        }
        return null;
    }

    public static ItemStack addContent(ItemStack itemStack, ResourceLocation content) {
        ItemStack result = itemStack.copy();
        CompoundTag nbt = result.getTag();
        if (nbt == null) {
            nbt = new CompoundTag();
        }
        if (!nbt.contains("BucketContent")) {
            nbt.putString("BucketContent", content.toString());
        }
        result.setTag(nbt);
        return result;
    }

    public static ItemStack removeContent(ItemStack itemStack) {
        ItemStack result = itemStack.copy();
        CompoundTag nbt = result.getTag();
        if (nbt != null && nbt.contains("BucketContent")) {
            nbt.remove("BucketContent");
            result.setTag(nbt);
        }
        return result;
    }

    public static InteractionResult tryMilkLivingEntity(ItemStack itemStack, LivingEntity entity, Player player, InteractionHand interactionHand) {
        if (!BucketLibTags.EntityTypes.MILKABLE.contains(entity.getType())) {
            return InteractionResult.PASS;
        }
        player.setItemInHand(interactionHand, new ItemStack(Items.BUCKET));
        InteractionResult result = player.interactOn(entity, interactionHand);
        if (result.consumesAction()) {
            itemStack = BucketLibUtil.addMilk(itemStack);
        }
        player.setItemInHand(interactionHand, itemStack);
        return result;
    }

    public static boolean containsMilk(ItemStack itemStack) {
        CompoundTag nbt = itemStack.getTag();
        if (nbt != null && nbt.contains("BucketContent") && nbt.getString("BucketContent").equals(MILK_LOCATION.toString())) {
            return true;
        }
        if (ForgeMod.MILK.isPresent()) {
            return FluidUtil.getFluidContained(itemStack).orElse(FluidStack.EMPTY).getFluid() == ForgeMod.MILK.get();
        }
        return false;
    }

    public static ItemStack addMilk(ItemStack itemStack) {
        if (ForgeMod.MILK.isPresent()) {
            return addFluid(itemStack, ForgeMod.MILK.get());
        }
        return addContent(itemStack, MILK_LOCATION);
    }

    public static ItemStack removeMilk(ItemStack itemStack) {
        CompoundTag nbt = itemStack.getTag();
        if (nbt != null && nbt.contains("BucketContent")) {
            return removeContent(itemStack);
        }
        return removeFluid(itemStack);
    }

    public static ItemStack addFluid(ItemStack itemStack, Fluid fluid) {
        AtomicReference<ItemStack> resultItemStack = new AtomicReference<>(itemStack.copy());
        FluidUtil.getFluidHandler(resultItemStack.get()).ifPresent(fluidHandler -> {
            fluidHandler.fill(new FluidStack(fluid, FluidAttributes.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
            resultItemStack.set(fluidHandler.getContainer());
        });
        return resultItemStack.get();
    }

    public static ItemStack removeFluid(ItemStack itemStack) {
        AtomicReference<ItemStack> resultItemStack = new AtomicReference<>(itemStack.copy());
        FluidUtil.getFluidHandler(resultItemStack.get()).ifPresent(fluidHandler -> {
            fluidHandler.drain(new FluidStack(fluidHandler.getFluidInTank(0).getFluid(), FluidAttributes.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
            resultItemStack.set(fluidHandler.getContainer());
        });
        return resultItemStack.get();
    }

}
