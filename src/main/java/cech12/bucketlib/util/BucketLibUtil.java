package cech12.bucketlib.util;

import cech12.bucketlib.api.BucketLibTags;
import cech12.bucketlib.config.ServerConfig;
import cech12.bucketlib.api.item.UniversalBucketItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public class BucketLibUtil {

    public static final ResourceLocation MILK_LOCATION = new ResourceLocation("milk");

    private static final Random RANDOM = new Random();

    private BucketLibUtil() {}

    public static boolean notCreative(Entity entity) {
        return !(entity instanceof Player player) || !player.getAbilities().instabuild;
    }

    public static boolean isEmpty(ItemStack itemStack) {
        return !containsFluid(itemStack) && !containsMilk(itemStack) && !containsEntityType(itemStack) && !containsBlock(itemStack);
    }

    public static ItemStack createEmptyResult(ItemStack initialStack, Player player, ItemStack resultStack, InteractionHand hand) {
        if (!BucketLibUtil.notCreative(player)) {
            return initialStack;
        }
        if (resultStack.isEmpty()) {
            //player.broadcastBreakEvent(hand); //does not work here to play the sound, because the hand is empty until this event gotten
            if (!initialStack.isEmpty()) {
                if (!player.isSilent()) {
                    player.getLevel().playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK, player.getSoundSource(), 0.8F, 0.8F + player.getLevel().getRandom().nextFloat() * 0.4F, false);
                }
                player.spawnItemParticles(initialStack, 5);
            }
            player.awardStat(Stats.ITEM_BROKEN.get(initialStack.getItem()));
        }
        return resultStack;
    }

    /**
     * Adds damage to the bucket if damaging is enabled.
     * @param stack item stack which gets damage
     * @param random Random object
     * @param player ServerPlayer object or null if no player is involved
     */
    public static void damageByOne(ItemStack stack, Random random, ServerPlayer player) {
        if (!stack.isEmpty() && stack.isDamageableItem()
                && !BucketLibUtil.isAffectedByInfinityEnchantment(stack)
                && stack.hurt(1, random, player)) {
            stack.shrink(1);
            stack.setDamageValue(0);
        }
    }

    /**
     * Adds damage to the bucket if damaging is enabled.
     * If there is a player context, please use {@link #damageByOne(ItemStack, Random, ServerPlayer)}
     * @param stack item stack which gets damage
     */
    public static void damageByOne(ItemStack stack) {
        damageByOne(stack, RANDOM, null);
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
            Fluid fluid = getFluid(itemStack);
            return fluid != Fluids.EMPTY
                    && Objects.requireNonNull(ForgeRegistries.FLUIDS.tags()).getTag(BucketLibTags.Fluids.INFINITY_ENCHANTABLE).contains(fluid)
                    && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, itemStack) > 0
                    && bucket.canHoldFluid(fluid);
        }
        return false;
    }

    private static boolean containsTagContent(ItemStack itemStack, String tagName) {
        CompoundTag nbt = itemStack.getTag();
        return nbt != null && nbt.contains(tagName);
    }

    private static String getTagContent(ItemStack itemStack, String tagName) {
        CompoundTag nbt = itemStack.getTag();
        if (nbt != null && nbt.contains(tagName)) {
            return nbt.getString(tagName);
        }
        return null;
    }

    private static ItemStack setTagContent(ItemStack itemStack, String tagName, String tagContent) {
        ItemStack result = itemStack.copy();
        CompoundTag nbt = result.getOrCreateTag();
        nbt.putString(tagName, tagContent);
        result.setTag(nbt);
        return result;
    }

    private static ItemStack removeTagContent(ItemStack itemStack, String tagName) {
        ItemStack result = itemStack.copy();
        CompoundTag nbt = result.getTag();
        if (nbt != null && nbt.contains(tagName)) {
            nbt.remove(tagName);
            if (nbt.isEmpty()) {
                result.setTag(null);
            } else {
                result.setTag(nbt);
            }
        }
        return result;
    }

    public static boolean containsContent(ItemStack itemStack) {
        return containsTagContent(itemStack, "BucketContent");
    }

    public static ResourceLocation getContent(ItemStack itemStack) {
        String content = getTagContent(itemStack, "BucketContent");
        if (content != null) {
            return new ResourceLocation(content);
        }
        return null;
    }

    public static ItemStack addContent(ItemStack itemStack, ResourceLocation content) {
        return setTagContent(itemStack, "BucketContent", content.toString());
    }

    public static ItemStack removeContent(ItemStack itemStack) {
        return removeContent(itemStack, true);
    }

    private static ItemStack removeContent(ItemStack itemStack, boolean damage) {
        ItemStack emptyStack = removeTagContent(itemStack, "BucketContent");
        if (damage) damageByOne(emptyStack);
        return emptyStack;
    }

    public static boolean containsMilk(ItemStack itemStack) {
        ResourceLocation bucketContent = getContent(itemStack);
        if (bucketContent != null && bucketContent.equals(MILK_LOCATION)) {
            return true;
        }
        if (ForgeMod.MILK.isPresent()) {
            return getFluid(itemStack) == ForgeMod.MILK.get();
        }
        return false;
    }

    public static ItemStack addMilk(ItemStack itemStack) {
        ItemStack filledStack = itemStack;
        if (ForgeMod.MILK.isPresent()) {
            filledStack = addFluid(filledStack, ForgeMod.MILK.get());
        }
        return addContent(filledStack, MILK_LOCATION);
    }

    public static ItemStack removeMilk(ItemStack itemStack) {
        return removeFluid(itemStack);
    }

    public static boolean containsFluid(ItemStack itemStack) {
        return getFluid(itemStack) != Fluids.EMPTY;
    }

    public static Fluid getFluid(ItemStack itemStack) {
        return FluidUtil.getFluidHandler(itemStack).map(fluidHandler -> fluidHandler.getFluidInTank(0)).orElse(FluidStack.EMPTY).getFluid();
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
        if (containsMilk(itemStack)) {
            resultItemStack.set(removeContent(resultItemStack.get(), !containsFluid(resultItemStack.get())));
        }
        FluidUtil.getFluidHandler(resultItemStack.get()).ifPresent(fluidHandler -> {
            fluidHandler.drain(new FluidStack(fluidHandler.getFluidInTank(0).getFluid(), FluidAttributes.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
            //damaging is done by fluid handler
            resultItemStack.set(fluidHandler.getContainer());
        });
        return resultItemStack.get();
    }

    public static boolean containsEntityType(ItemStack itemStack) {
        return containsTagContent(itemStack, "EntityType");
    }

    public static EntityType<?> getEntityType(ItemStack itemStack) {
        String content = getTagContent(itemStack, "EntityType");
        if (content != null) {
            return ForgeRegistries.ENTITIES.getValue(new ResourceLocation(content));
        }
        return null;
    }

    public static ItemStack addEntityType(ItemStack itemStack, EntityType<?> entityType) {
        return setTagContent(itemStack, "EntityType", entityType.getRegistryName().toString());
    }

    public static ItemStack removeEntityType(ItemStack itemStack, boolean damage) {
        ItemStack emptyStack = removeTagContent(itemStack, "EntityType");
        if (damage) damageByOne(emptyStack);
        return emptyStack;
    }

    public static boolean containsBlock(ItemStack itemStack) {
        return containsContent(itemStack) && !containsMilk(itemStack);
    }

    public static Block getBlock(ItemStack itemStack) {
        if (!containsMilk(itemStack)) {
            ResourceLocation content = getContent(itemStack);
            if (content != null) {
                return ForgeRegistries.BLOCKS.getValue(content);
            }
        }
        return null;
    }

    public static ItemStack addBlock(ItemStack itemStack, Block block) {
        if (block.getRegistryName() != null) {
            return addContent(itemStack, block.getRegistryName());
        }
        return itemStack.copy();
    }

    public static ItemStack removeBlock(ItemStack itemStack) {
        if (!containsMilk(itemStack)) {
            return removeContent(itemStack);
        }
        return itemStack.copy();
    }

}
