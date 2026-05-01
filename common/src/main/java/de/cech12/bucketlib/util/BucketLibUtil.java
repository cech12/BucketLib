package de.cech12.bucketlib.util;

import de.cech12.bucketlib.api.BucketLibComponents;
import de.cech12.bucketlib.api.BucketLibTags;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.mixin.LivingEntityAccessor;
import de.cech12.bucketlib.platform.Services;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class BucketLibUtil {

    public static final Identifier MILK_LOCATION = Identifier.withDefaultNamespace("milk");

    private BucketLibUtil() {}

    public static boolean notCreative(Entity entity) {
        return !(entity instanceof Player player) || !player.getAbilities().instabuild;
    }

    public static Item getItem(ItemInstance itemInstance) {
        if (itemInstance instanceof ItemStack stack) {
            return stack.getItem();
        } else if (itemInstance instanceof ItemStackTemplate template) {
            return template.item().value();
        }
        throw new IllegalArgumentException("Invalid item instance");
    }

    public static ItemStack getItemStack(ItemInstance itemInstance) {
        if (itemInstance instanceof ItemStack stack) {
            return stack;
        } else if (itemInstance instanceof ItemStackTemplate template) {
            return template.create();
        }
        throw new IllegalArgumentException("Invalid item instance");
    }

    public static ItemStackTemplate getItemStackTemplate(ItemInstance itemInstance) {
        if (itemInstance instanceof ItemStack stack) {
            return ItemStackTemplate.fromNonEmptyStack(stack);
        } else if (itemInstance instanceof ItemStackTemplate template) {
            return template;
        }
        throw new IllegalArgumentException("Invalid item instance");
    }

    public static boolean isEmpty(ItemInstance itemInstance) {
        return !containsFluid(itemInstance) && !containsMilk(itemInstance) && !containsEntityType(itemInstance) && !containsBlock(itemInstance);
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
                    player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK.value(), player.getSoundSource(), 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F, false);
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
     * @param itemInstance checked item instance
     * @return boolean
     */
    public static boolean isAffectedByInfinityEnchantment(@NotNull ItemInstance itemInstance) {
        return isInfinityEnchantmentAllowed(itemInstance) && hasInfinityEnchantment(itemInstance);
    }

    /**
     * Checks if the given bucket is enchanted with the Infinity enchantment.
     * @param itemInstance checked item instance
     * @return boolean
     */
    public static boolean hasInfinityEnchantment(@NotNull ItemInstance itemInstance) {
        return itemInstance.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).keySet().stream()
                .filter(enchantment -> enchantment.is(Enchantments.INFINITY))
                .anyMatch(enchantment -> EnchantmentHelper.getItemEnchantmentLevel(enchantment, itemInstance) > 0);
    }

    /**
     * Checks if the given bucket is allowed to be enchanted with Infinity enchantment.
     * @param itemInstance checked item instance
     * @return boolean
     */
    public static boolean isInfinityEnchantmentAllowed(@NotNull ItemInstance itemInstance) {
        if (!Services.CONFIG.isInfinityEnchantmentEnabled()) {
            return false;
        }
        if (getItem(itemInstance) instanceof UniversalBucketItem bucket) {
            Fluid fluid = getFluid(itemInstance);
            return fluid != Fluids.EMPTY
                    && fluid.defaultFluidState().is(BucketLibTags.Fluids.INFINITY_ENCHANTABLE)
                    && bucket.canHoldFluid(fluid);
        }
        return false;
    }

    private static boolean containsTagContent(DataComponentGetter componentGetter, String tagName) {
        CustomData customdata = componentGetter.getOrDefault(BucketLibComponents.BUCKET_CONTENT, CustomData.EMPTY);
        return customdata.copyTag().contains(tagName);
    }

    private static String getTagContent(DataComponentGetter componentGetter, String tagName) {
        CustomData customdata = componentGetter.getOrDefault(BucketLibComponents.BUCKET_CONTENT, CustomData.EMPTY);
        return customdata.copyTag().getString(tagName).orElse(null);
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

    public static boolean containsContent(DataComponentGetter componentGetter) {
        return containsTagContent(componentGetter, "BucketContent");
    }

    public static Identifier getContent(DataComponentGetter componentGetter) {
        String content = getContentString(componentGetter);
        if (content != null) {
            return Identifier.parse(content);
        }
        return null;
    }

    public static String getContentString(DataComponentGetter componentGetter) {
        return getTagContent(componentGetter, "BucketContent");
    }

    public static ItemStack addContent(ItemStack itemStack, Identifier content) {
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

    public static boolean containsMilk(DataComponentGetter componentGetter) {
        Identifier bucketContent = getContent(componentGetter);
        return bucketContent != null && bucketContent.equals(MILK_LOCATION);
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

    public static boolean containsFluid(ItemInstance itemInstance) {
        return getFluid(itemInstance) != Fluids.EMPTY;
    }

    public static Fluid getFluid(ItemInstance itemInstance) {
        return Services.FLUID.getContainedFluid(getItemStack(itemInstance));
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

    public static boolean containsEntityType(DataComponentGetter componentGetter) {
        return containsTagContent(componentGetter, "EntityType");
    }

    public static EntityType<?> getEntityType(DataComponentGetter componentGetter) {
        String content = getEntityTypeString(componentGetter);
        if (content != null) {
            return Services.REGISTRY.getEntityType(Identifier.parse(content));
        }
        return null;
    }

    public static String getEntityTypeString(DataComponentGetter componentGetter) {
        return getTagContent(componentGetter, "EntityType");
    }

    public static ItemStack addEntityType(ItemStack itemStack, EntityType<?> entityType) {
        return setTagContent(itemStack, "EntityType", Services.REGISTRY.getEntityTypeLocation(entityType).toString());
    }

    public static ItemStack removeEntityData(ItemStack itemStack, ServerLevel level, @Nullable Player player, boolean damage) {
        EntityType<?> entityType;
        if (level != null && (entityType = BucketLibUtil.getEntityType(itemStack)) != null) {
            Entity entity = entityType.create(level, EntitySpawnReason.LOAD);
            ItemStack stack = removeEntityData(itemStack, level, player, entity, damage);
            if (entity != null) {
                entity.discard();
            }
            return stack;
        }
        return removeEntityData(itemStack, level, player, null, damage);
    }


    public static ItemStack removeEntityData(ItemStack itemStack, ServerLevel level, @Nullable Player player, @Nullable Entity entity, boolean damage) {
        ItemStack emptyStack = removeTagContent(itemStack, "EntityType");
        Set<DataComponentType<?>> types = new HashSet<>();
        types.add(DataComponents.BUCKET_ENTITY_DATA);
        //support custom item stack data components
        if (entity instanceof Bucketable bucketable) {
            ItemStack emptyVanillaStack = new ItemStack(BucketLibUtil.getFluid(itemStack).getBucket());
            ItemStack changedVanillaStack = new ItemStack(BucketLibUtil.getFluid(itemStack).getBucket());
            bucketable.saveToBucketTag(changedVanillaStack);
            changedVanillaStack.getComponents().stream().forEach(typedDataComponent -> {
                AtomicBoolean addType = new AtomicBoolean(true);
                emptyVanillaStack.getComponents().stream().forEach(typedDataComponentVanilla -> {
                    //only types that were changed
                    if (typedDataComponent.type() == typedDataComponentVanilla.type()) {
                        addType.set(false);
                        if (changedVanillaStack.get(typedDataComponent.type()) != null && changedVanillaStack.get(typedDataComponent.type()) != emptyVanillaStack.get(typedDataComponentVanilla.type()) ) {
                            types.add(typedDataComponent.type());
                        }
                    }
                });
                //and added types
                if (addType.get()) {
                    types.add(typedDataComponent.type());
                }
            });
        }
        for (DataComponentType<?> type : types) {
            emptyStack.remove(type);
        }
        if (damage) damageByOne(emptyStack, level, player);
        return emptyStack;
    }

    public static boolean containsBlock(DataComponentGetter componentGetter) {
        return containsContent(componentGetter) && !containsMilk(componentGetter);
    }

    public static Block getBlock(DataComponentGetter componentGetter) {
        if (!containsMilk(componentGetter)) {
            Identifier content = getContent(componentGetter);
            if (content != null) {
                return Services.REGISTRY.getBlock(content);
            }
        }
        return null;
    }

    public static ItemStack addBlock(ItemStack itemStack, Block block) {
        Identifier blockLocation = Services.REGISTRY.getBlockLocation(block);
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
