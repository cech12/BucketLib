package de.cech12.bucketlib.util;

import de.cech12.bucketlib.api.BucketLibComponents;
import de.cech12.bucketlib.api.BucketLibTags;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.mixin.LivingEntityAccessor;
import de.cech12.bucketlib.platform.Services;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicReference;

public class BucketLibUtil {

    public static final ResourceLocation MILK_LOCATION = ResourceLocation.withDefaultNamespace("milk");

    private BucketLibUtil() {}

    public static boolean notCreative(Entity entity) {
        return !(entity instanceof Player player) || !player.getAbilities().instabuild;
    }

    public static boolean isEmpty(ItemStack itemStack) {
        return !containsFluid(itemStack) && !containsMilk(itemStack) && !containsEntityType(itemStack) && !containsBlock(itemStack);
    }

    public static ItemStack createEmptyResult(ItemStack initialStack, Player player, ItemStack resultStack, InteractionHand hand) {
        return createEmptyResult(initialStack, player, resultStack, hand, false);
    }

    public static ItemStack createEmptyResult(ItemStack initialStack, Player player, ItemStack resultStack, InteractionHand hand, boolean addAdditionalBucketOnInstaBuild) {
        if (!BucketLibUtil.notCreative(player)) {
            if (addAdditionalBucketOnInstaBuild && !player.getInventory().contains(resultStack)) {
                player.getInventory().add(resultStack);
            }
            return initialStack;
        }
        if (resultStack.isEmpty()) {
            //player.broadcastBreakEvent(hand); //does not work here to play the sound, because the hand is empty until this event gotten
            if (!initialStack.isEmpty()) {
                if (!player.isSilent()) {
                    player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK, player.getSoundSource(), 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F, false);
                }
                ((LivingEntityAccessor) player).bucketlib_spawnItemParticles(initialStack, 5);
            }
            player.awardStat(Stats.ITEM_BROKEN.get(initialStack.getItem()));
        }
        return resultStack;
    }

    /**
     * Adds damage to the bucket if damaging is enabled.
     * @param stack item stack which gets damage
     * @param level ServerLevel
     * @param player Player object or null if no player is involved
     */
    public static void damageByOne(ItemStack stack, ServerLevel level, Player player) {
        if (level == null) {
            damageByOne(stack, (player instanceof ServerPlayer) ? (ServerPlayer) player : null); //workaround for contexts without level access (Crafting & fluid handlers)
            return;
        }
        if (!stack.isEmpty() && stack.isDamageableItem() && !BucketLibUtil.isAffectedByInfinityEnchantment(stack)) {
            stack.hurtAndBreak(1, level, (player instanceof ServerPlayer) ? (ServerPlayer) player : null, (item) -> {
                stack.setDamageValue(0);
            });
        }
    }

    /**
     * Adds damage to the bucket if damaging is enabled. This method should only be used if there is no ServerLevel in the calling context.
     * Enchantments have no effect here!
     * It is recommended to use {@link #damageByOne(ItemStack, ServerLevel, Player)}
     * @param stack item stack which gets damage
     */
    @Deprecated //TODO find a way to get server level access in all calling contexts
    public static void damageByOne(ItemStack stack, @Nullable ServerPlayer player) {
        if (!stack.isEmpty() && stack.isDamageableItem() && !BucketLibUtil.isAffectedByInfinityEnchantment(stack)) {
            int newDamageValue = stack.getDamageValue() + 1;
            if (player != null) {
                CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(player, stack, newDamageValue);
            }
            stack.setDamageValue(newDamageValue);
            if (newDamageValue >= stack.getMaxDamage()) {
                stack.shrink(1);
                stack.setDamageValue(0);
            }
        }
    }

    /**
     * Checks if the given bucket is affected by Infinity enchantment.
     * @param itemStack checked item stack
     * @return boolean
     */
    public static boolean isAffectedByInfinityEnchantment(@Nonnull ItemStack itemStack) {
        if (!Services.CONFIG.isInfinityEnchantmentEnabled()) {
            return false;
        }
        if (itemStack.getItem() instanceof UniversalBucketItem bucket) {
            Fluid fluid = getFluid(itemStack);
            return fluid != Fluids.EMPTY
                    && fluid.defaultFluidState().is(BucketLibTags.Fluids.INFINITY_ENCHANTABLE)
                    && EnchantmentHelper.getItemEnchantmentLevel(VanillaRegistries.createLookup().lookup(Registries.ENCHANTMENT).get().getOrThrow(Enchantments.INFINITY), itemStack) > 0
                    && bucket.canHoldFluid(fluid);
        }
        return false;
    }

    private static boolean containsTagContent(ItemStack itemStack, String tagName) {
        CustomData customdata = itemStack.getOrDefault(BucketLibComponents.BUCKET_CONTENT, CustomData.EMPTY);
        return customdata.contains(tagName);
    }

    private static String getTagContent(ItemStack itemStack, String tagName) {
        CustomData customdata = itemStack.getOrDefault(BucketLibComponents.BUCKET_CONTENT, CustomData.EMPTY);
        if (customdata.contains(tagName)) {
            return customdata.copyTag().getString(tagName);
        }
        return null;
    }

    private static ItemStack setTagContent(ItemStack itemStack, String tagName, String tagContent) {
        ItemStack result = itemStack.copy();
        CustomData customdata = result.getOrDefault(BucketLibComponents.BUCKET_CONTENT, CustomData.EMPTY);
        CompoundTag nbt = customdata.copyTag();
        nbt.putString(tagName, tagContent);
        result.set(BucketLibComponents.BUCKET_CONTENT, CustomData.of(nbt));
        return result;
    }

    private static ItemStack removeTagContentNoCopy(ItemStack itemStack, String tagName) {
        CustomData customdata = itemStack.getOrDefault(BucketLibComponents.BUCKET_CONTENT, CustomData.EMPTY);
        CompoundTag nbt = customdata.copyTag();
        if (nbt.contains(tagName)) {
            nbt.remove(tagName);
            if (nbt.isEmpty()) {
                itemStack.remove(BucketLibComponents.BUCKET_CONTENT);
            } else {
                itemStack.set(BucketLibComponents.BUCKET_CONTENT, CustomData.of(nbt));
            }
        }
        return itemStack;
    }

    private static ItemStack removeTagContent(ItemStack itemStack, String tagName) {
        return removeTagContentNoCopy(itemStack.copy(), tagName);
    }

    public static boolean containsContent(ItemStack itemStack) {
        return containsTagContent(itemStack, "BucketContent");
    }

    public static ResourceLocation getContent(ItemStack itemStack) {
        String content = getTagContent(itemStack, "BucketContent");
        if (content != null) {
            return ResourceLocation.parse(content);
        }
        return null;
    }

    public static ItemStack addContent(ItemStack itemStack, ResourceLocation content) {
        return setTagContent(itemStack, "BucketContent", content.toString());
    }

    public static void removeContentNoCopy(ItemStack itemStack, ServerLevel level, @Nullable Player player, boolean damage) {
        ItemStack emptyStack = removeTagContentNoCopy(itemStack, "BucketContent");
        if (damage) damageByOne(emptyStack, level, player);
    }

    private static ItemStack removeContent(ItemStack itemStack, ServerLevel level, @Nullable Player player, boolean damage) {
        ItemStack emptyStack = removeTagContent(itemStack, "BucketContent");
        if (damage) damageByOne(emptyStack, level, player);
        return emptyStack;
    }

    public static boolean containsMilk(ItemStack itemStack) {
        ResourceLocation bucketContent = getContent(itemStack);
        if (bucketContent != null && bucketContent.equals(MILK_LOCATION)) {
            return true;
        }
        if (Services.FLUID.hasMilkFluid()) {
            return getFluid(itemStack) == Services.FLUID.getMilkFluid();
        }
        return false;
    }

    public static ItemStack addMilk(ItemStack itemStack) {
        ItemStack filledStack = itemStack;
        if (Services.FLUID.hasMilkFluid()) {
            filledStack = addFluid(filledStack, Services.FLUID.getMilkFluid());
        }
        return addContent(filledStack, MILK_LOCATION);
    }

    public static ItemStack removeMilk(ItemStack itemStack, ServerLevel level, @Nullable Player player) {
        return removeFluid(itemStack, level, player);
    }

    public static boolean containsFluid(ItemStack itemStack) {
        return getFluid(itemStack) != Fluids.EMPTY;
    }

    public static Fluid getFluid(ItemStack itemStack) {
        return Services.FLUID.getContainedFluid(itemStack);
    }

    public static ItemStack addFluid(ItemStack itemStack, Fluid fluid) {
        return Services.FLUID.addFluid(itemStack, fluid);
    }

    public static ItemStack removeFluid(ItemStack itemStack, ServerLevel level, @Nullable Player player) {
        AtomicReference<ItemStack> resultItemStack = new AtomicReference<>(itemStack.copy());
        if (containsMilk(itemStack)) {
            resultItemStack.set(removeContent(resultItemStack.get(), level, player, !containsFluid(resultItemStack.get())));
        }
        return Services.FLUID.removeFluid(resultItemStack.get(), level, player);
    }

    public static boolean containsEntityType(ItemStack itemStack) {
        return containsTagContent(itemStack, "EntityType");
    }

    public static EntityType<?> getEntityType(ItemStack itemStack) {
        String content = getTagContent(itemStack, "EntityType");
        if (content != null) {
            return Services.REGISTRY.getEntityType(ResourceLocation.parse(content));
        }
        return null;
    }

    public static ItemStack addEntityType(ItemStack itemStack, EntityType<?> entityType) {
        return setTagContent(itemStack, "EntityType", Services.REGISTRY.getEntityTypeLocation(entityType).toString());
    }

    public static ItemStack removeEntityType(ItemStack itemStack, ServerLevel level, @Nullable Player player, boolean damage) {
        ItemStack emptyStack = removeTagContent(itemStack, "EntityType");
        emptyStack.remove(DataComponents.BUCKET_ENTITY_DATA); //remove entity data
        if (damage) damageByOne(emptyStack, level, player);
        return emptyStack;
    }

    public static boolean containsBlock(ItemStack itemStack) {
        return containsContent(itemStack) && !containsMilk(itemStack);
    }

    public static Block getBlock(ItemStack itemStack) {
        if (!containsMilk(itemStack)) {
            ResourceLocation content = getContent(itemStack);
            if (content != null) {
                return Services.REGISTRY.getBlock(content);
            }
        }
        return null;
    }

    public static ItemStack addBlock(ItemStack itemStack, Block block) {
        ResourceLocation blockLocation = Services.REGISTRY.getBlockLocation(block);
        if (blockLocation != null) {
            return addContent(itemStack, blockLocation);
        }
        return itemStack.copy();
    }

    public static ItemStack removeBlock(ItemStack itemStack, ServerLevel level, @Nullable Player player, boolean damage) {
        if (!containsMilk(itemStack)) {
            return removeContent(itemStack, level, player, damage);
        }
        return itemStack.copy();
    }

}
