package de.cech12.bucketlib.api.item;

import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.platform.Services;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.ItemStackUtil;
import de.cech12.bucketlib.util.RegistryUtil;
import de.cech12.bucketlib.util.WorldInteractionUtil;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FastColor;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class UniversalBucketItem extends Item {

    private final Properties properties;

    public UniversalBucketItem(Properties properties) {
        super((new Item.Properties().stacksTo(1)));
        this.properties = properties;
    }

    @Override
    @Nonnull
    public Component getName(@Nonnull ItemStack stack) {
        String descriptionId = this.getDescriptionId(stack);
        Component argument;
        if (BucketLibUtil.containsEntityType(stack)) {
            descriptionId += ".entity";
            EntityType<?> entityType = BucketLibUtil.getEntityType(stack);
            argument = (entityType != null) ? entityType.getDescription() : Component.literal("?");
        } else if (BucketLibUtil.containsFluid(stack)) {
            descriptionId += ".filled";
            Fluid fluid = BucketLibUtil.getFluid(stack);
            argument = Services.FLUID.getFluidDescription(fluid);
        } else if (BucketLibUtil.containsBlock(stack)) {
            descriptionId += ".filled";
            Block block = BucketLibUtil.getBlock(stack);
            argument = (block != null) ? block.getName() : Component.literal("?");
        } else if (BucketLibUtil.containsMilk(stack)) {
            descriptionId += ".filled";
            argument = Component.translatable(Services.PLATFORM.getMilkTranslationKey());
        } else {
            //is empty
            return Component.translatable(descriptionId);
        }
        return Component.translatable(descriptionId, argument);
    }

    public boolean isCracked(ItemStack stack) {
        Fluid fluid = BucketLibUtil.getFluid(stack);
        if (fluid != Fluids.EMPTY) {
            int fluidTemperature = Services.FLUID.getFluidTemperature(fluid);
            Integer upperCrackingTemperature = getUpperBreakTemperature();
            Integer lowerCrackingTemperature = getLowerBreakTemperature();
            return isCrackingFluid(fluid)
                    || (upperCrackingTemperature != null && fluidTemperature >= upperCrackingTemperature)
                    || (lowerCrackingTemperature != null && fluidTemperature <= lowerCrackingTemperature)
                    && !BucketLibUtil.isAffectedByInfinityEnchantment(stack);
        }
        return false;
    }

    public boolean canHoldFluid(Fluid fluid) {
        if (fluid == Fluids.EMPTY) {
            return true;
        }
        Item bucket;
        try {
            bucket = fluid.getBucket();
        } catch (IllegalArgumentException ex) {
            //workaround to avoid game crash caused by getBucket() method of "noBucket" fluids of Registrate (tterrag1098) mod: https://github.com/tterrag1098/Registrate/issues/69
            BucketLib.LOG.error("IllegalArgumentException occurred while trying to get the bucket item of fluid '" + Services.FLUID.getFluidDescription(fluid) + "' [fluid.getBucket()]. BucketLib is not compatible with this fluid. Please contact the mod developer of the mod which adds this fluid!", ex);
            return false;
        }
        if (bucket instanceof MilkBucketItem && fluid != Services.FLUID.getMilkFluid()) {
            return false;
        }
        if (!(bucket instanceof MilkBucketItem) && (!(bucket instanceof BucketItem) || Services.BUCKET.getFluidOfBucketItem((BucketItem) bucket) != fluid)) {
            return false;
        }
        if (this.properties.allowedFluidsTag != null || this.properties.allowedFluids != null) {
            return isAllowedFluid(fluid);
        }
        if (this.properties.deniedFluidsTag != null || this.properties.deniedFluids != null) {
            return !isDeniedFluid(fluid);
        }
        int fluidTemperature = Services.FLUID.getFluidTemperature(fluid);
        Integer maxTemperature = getMaxTemperature();
        Integer minTemperature = getMinTemperature();
        return (maxTemperature == null || fluidTemperature <= maxTemperature)
                && (minTemperature == null || fluidTemperature >= minTemperature);
    }

    public boolean canHoldEntity(EntityType<?> entityType) {
        if (this.canObtainEntities()) {
            if (this.properties.allowedEntitiesTag != null || this.properties.allowedEntities != null) {
                return isAllowedEntity(entityType);
            }
            if (this.properties.deniedEntitiesTag != null || this.properties.deniedEntities != null) {
                return !isDeniedEntity(entityType);
            }
            return true;
        }
        return false;
    }

    public boolean canHoldBlock(Block block) {
        if (this.canObtainBlocks()) {
            if (this.properties.allowedBlocksTag != null || this.properties.allowedBlocks != null) {
                return isAllowedBlock(block);
            }
            if (this.properties.deniedBlocksTag != null || this.properties.deniedBlocks != null) {
                return !isDeniedBlock(block);
            }
            return true;
        }
        return false;
    }

    //@Override //overrides the neoforge implementation //used by mixin
    public int getMaxStackSize(ItemStack stack) {
        return BucketLibUtil.isEmpty(stack) ? this.properties.maxStackSize : 1;
    }

    //used by mixins
    public int getBucketBurnTime(ItemStack stack, RecipeType<?> recipeType) {
        //entity buckets should not use the burn time of its fluid
        if (stack.getItem() instanceof UniversalBucketItem && !BucketLibUtil.containsEntityType(stack)) {
            Fluid fluid = Services.FLUID.getContainedFluid(stack);
            if (fluid != Fluids.EMPTY) {
                return Services.PLATFORM.getBurnTime(new ItemStack(fluid.getBucket()), recipeType);
            }
        }
        return Services.PLATFORM.getBurnTime(stack, recipeType);
    }

    @Override
    public void inventoryTick(@Nonnull ItemStack itemStack, @Nonnull Level level, @Nonnull Entity entity, int position, boolean selected) {
        if (!level.isClientSide) {
            if (!entity.fireImmune() && this.hasBurningContent(itemStack)) {
                entity.setTicksFrozen(0); //avoid extinguish sounds
                entity.setRemainingFireTicks(100);
                if (BucketLibUtil.notCreative(entity) && entity.tickCount % 20 == 0) {
                    BucketLibUtil.damageByOne(itemStack, (ServerLevel) level, (entity instanceof Player) ? (Player) entity : null);
                }
            } else if (!entity.isOnFire() && entity.canFreeze() && this.hasFreezingContent(itemStack)) {
                int ticks = entity.getTicksFrozen() + (entity.isInPowderSnow ? 1 : 3); //2 are subtracted when not in powder snow
                entity.setTicksFrozen(Math.min(entity.getTicksRequiredToFreeze(), ticks));
                //damaging here because, the vanilla mechanic is reducing the freeze ticks below fully freezing
                if (BucketLibUtil.notCreative(entity) && entity.tickCount % 40 == 0 && entity.isFullyFrozen()) {
                    entity.hurt(level.damageSources().freeze(), entity.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES) ? 5 : 1);
                    BucketLibUtil.damageByOne(itemStack, (ServerLevel) level, (entity instanceof Player) ? (Player) entity : null);
                }
            }
        }
    }

    private boolean hasBurningContent(@Nonnull ItemStack itemStack) {
        Integer burningTemperature = this.getBurningTemperature();
        Fluid fluid = BucketLibUtil.getFluid(itemStack);
        return fluid != Fluids.EMPTY && (burningTemperature != null && Services.FLUID.getFluidTemperature(fluid) >= burningTemperature || this.isBurningFluid(fluid))
                || this.isBurningBlock(BucketLibUtil.getBlock(itemStack));
    }

    private boolean hasFreezingContent(@Nonnull ItemStack itemStack) {
        Integer freezingTemperature = this.getFreezingTemperature();
        Fluid fluid = BucketLibUtil.getFluid(itemStack);
        return fluid != Fluids.EMPTY && (freezingTemperature != null && Services.FLUID.getFluidTemperature(fluid) <= freezingTemperature || this.isFreezingFluid(fluid))
                || this.isFreezingBlock(BucketLibUtil.getBlock(itemStack));
    }

    @Override
    @Nonnull
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand interactionHand) {
        ItemStack itemstack = player.getItemInHand(interactionHand);
        boolean isEmpty = BucketLibUtil.isEmpty(itemstack);
        //check hit block
        BlockHitResult blockHitResult = getPlayerPOVHitResult(level, player, isEmpty ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
        if (blockHitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos hitBlockPos = blockHitResult.getBlockPos();
            BlockState hitBlockState = level.getBlockState(hitBlockPos);
            Direction hitDirection = blockHitResult.getDirection();
            BlockPos relativeBlockPos = hitBlockPos.relative(hitDirection);
            ServerLevel serverLevel = (level instanceof ServerLevel) ? (ServerLevel) level : null;
            if (isEmpty) {
                //pickup from cauldron interaction
                InteractionResultHolder<ItemStack> caldronInteractionResult = WorldInteractionUtil.tryPickupFromCauldron(level, player, interactionHand, blockHitResult);
                if (caldronInteractionResult.getResult().consumesAction()) {
                    return caldronInteractionResult;
                }
                Tuple<Boolean, ItemStack> result = Services.FLUID.tryPickUpFluid(BucketLibUtil.removeEntityType(itemstack, serverLevel, player, false), player, level, interactionHand, hitBlockPos, hitDirection);
                if (result.getA()) {
                    return InteractionResultHolder.sidedSuccess(ItemUtils.createFilledResult(itemstack, player, result.getB()), level.isClientSide());
                }
                //pickup block interaction
                RegistryUtil.BucketBlock bucketBlock = RegistryUtil.getBucketBlock(hitBlockState.getBlock());
                if (bucketBlock != null && canHoldBlock(bucketBlock.block())) {
                    //fake vanilla bucket use
                    ItemStack fakeStack = new ItemStack(Items.BUCKET);
                    player.setItemInHand(interactionHand, fakeStack);
                    InteractionResultHolder<ItemStack> interactionResult = fakeStack.use(level, player, interactionHand);
                    player.setItemInHand(interactionHand, itemstack);
                    if (interactionResult.getResult().consumesAction()) {
                        return new InteractionResultHolder<>(interactionResult.getResult(), ItemUtils.createFilledResult(itemstack.copy(), player, BucketLibUtil.addBlock(ItemStackUtil.copyStackWithSize(itemstack, 1), bucketBlock.block())));
                    }
                }
            } else {
                //place into cauldron interaction
                InteractionResultHolder<ItemStack> caldronInteractionResult = WorldInteractionUtil.tryPlaceIntoCauldron(level, player, interactionHand, blockHitResult);
                if (caldronInteractionResult.getResult().consumesAction()) {
                    return caldronInteractionResult;
                }
                if (BucketLibUtil.containsFluid(itemstack)) {
                    //try to place fluid at hit block and then at the relative block
                    for (BlockPos pos : Arrays.asList(hitBlockPos, relativeBlockPos)) {
                        //remove entity to be able to use tryPlaceFluid method
                        Tuple<Boolean, ItemStack> result = Services.FLUID.tryPlaceFluid(BucketLibUtil.removeEntityType(itemstack, serverLevel, player, false), player, level, interactionHand, pos);
                        if (result.getA()) {
                            if (BucketLibUtil.containsEntityType(itemstack)) {
                                //place entity if exists
                                spawnEntityFromBucket(player, level, itemstack, pos, false);
                            }
                            return InteractionResultHolder.sidedSuccess(BucketLibUtil.createEmptyResult(itemstack, player, result.getB(), interactionHand), level.isClientSide());
                        }
                    }
                } else if (BucketLibUtil.containsEntityType(itemstack)) {
                    //place entity interaction
                    ItemStack emptyBucket = spawnEntityFromBucket(player, level, itemstack, relativeBlockPos, true);
                    return InteractionResultHolder.sidedSuccess(BucketLibUtil.createEmptyResult(itemstack, player, emptyBucket, interactionHand), level.isClientSide());
                } else if (BucketLibUtil.containsBlock(itemstack)) {
                    //place block interaction
                    Block block = BucketLibUtil.getBlock(itemstack);
                    RegistryUtil.BucketBlock bucketBlock = RegistryUtil.getBucketBlock(block);
                    if (block != null && bucketBlock != null) {
                        //fake vanilla bucket use
                        ItemStack fakeStack = new ItemStack(bucketBlock.bucketItem());
                        player.setItemInHand(interactionHand, fakeStack);
                        InteractionResult interactionResult = fakeStack.useOn(new UseOnContext(player, interactionHand, blockHitResult));
                        player.setItemInHand(interactionHand, itemstack);
                        if (interactionResult.consumesAction()) {
                            return new InteractionResultHolder<>(interactionResult, BucketLibUtil.createEmptyResult(itemstack, player, BucketLibUtil.removeBlock(itemstack, serverLevel, player, true), interactionHand));
                        }
                    }
                }
            }
        }
        if (BucketLibUtil.containsMilk(itemstack)) {
            return ItemUtils.startUsingInstantly(level, player, interactionHand);
        }
        return InteractionResultHolder.pass(itemstack);
    }

    public ItemStack spawnEntityFromBucket(@Nullable Player player, Level level, ItemStack itemStack, BlockPos pos, boolean damage) {
        if (level instanceof ServerLevel serverLevel) {
            EntityType<?> entityType = BucketLibUtil.getEntityType(itemStack);
            if (entityType != null) {
                Entity entity = entityType.spawn(serverLevel, itemStack, null, pos, MobSpawnType.BUCKET, true, false);
                if (entity instanceof Bucketable bucketable) {
                    CustomData customdata = itemStack.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY);
                    bucketable.loadFromBucketTag(customdata.copyTag());
                    bucketable.setFromBucket(true);
                }
                if (player != null) {
                    serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
                }
                return BucketLibUtil.removeEntityType(itemStack, serverLevel, player, damage);
            }
        }
        return itemStack.copy();
    }

    @Override
    @Nonnull
    public InteractionResult interactLivingEntity(@Nonnull ItemStack itemStack, @Nonnull Player player, @Nonnull LivingEntity entity, @Nonnull InteractionHand interactionHand) {
        if (entity instanceof Bucketable && !BucketLibUtil.containsEntityType(itemStack) && canHoldEntity(entity.getType())) {
            InteractionResult result = this.pickupEntityWithBucket(player, interactionHand, (LivingEntity & Bucketable) entity);
            if (result.consumesAction()) {
                return result;
            }
        }
        if (this.canMilkEntities() && BucketLibUtil.isEmpty(itemStack)) {
            return WorldInteractionUtil.tryMilkLivingEntity(itemStack, entity, player, interactionHand);
        }
        //feeding axolotl is done by mixins
        return super.interactLivingEntity(itemStack, player, entity, interactionHand);
    }

    private <T extends LivingEntity & Bucketable> InteractionResult pickupEntityWithBucket(Player player, InteractionHand interactionHand, T entity) {
        ItemStack itemStack = player.getItemInHand(interactionHand).copy(); //copy to avoid changing the real item stack
        Fluid containedFluid = Services.FLUID.getContainedFluid(itemStack);
        Fluid entityBucketFluid = Services.BUCKET.getFluidOfBucketItem((BucketItem) entity.getBucketItemStack().getItem());
        if (itemStack.getItem() instanceof UniversalBucketItem
                && entity.isAlive()
                && entityBucketFluid == containedFluid) {
            entity.playSound(entity.getPickupSound(), 1.0F, 1.0F);
            ItemStack filledItemStack = BucketLibUtil.addEntityType(itemStack, entity.getType());
            entity.saveToBucketTag(filledItemStack);
            Level level = entity.level();
            ItemStack handItemStack = ItemUtils.createFilledResult(itemStack, player, filledItemStack, false);
            player.setItemInHand(interactionHand, handItemStack);
            if (!level.isClientSide) {
                CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer)player, new ItemStack(entity.getBucketItemStack().getItem()));
            }
            entity.discard();
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    @Nonnull
    public ItemStack finishUsingItem(@Nonnull ItemStack itemStack, @Nonnull Level level, @Nonnull LivingEntity player) {
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, new ItemStack(Items.MILK_BUCKET));
            serverPlayer.awardStat(Stats.ITEM_USED.get(Items.MILK_BUCKET));
        }
        if (!level.isClientSide) {
            Services.FLUID.curePotionEffects(player, new ItemStack(Items.MILK_BUCKET));
            if (BucketLibUtil.notCreative(player)) {
                return BucketLibUtil.removeMilk(itemStack, (ServerLevel) level, (player instanceof Player) ? (Player) player : null);
            }
        }
        return itemStack;
    }

    @Override
    public int getUseDuration(@Nonnull ItemStack itemStack, @Nonnull LivingEntity livingEntity) {
        if (BucketLibUtil.containsMilk(itemStack)) {
            return 32;
        }
        return super.getUseDuration(itemStack, livingEntity);
    }

    @Override
    @Nonnull
    public UseAnim getUseAnimation(@Nonnull ItemStack itemStack) {
        if (BucketLibUtil.containsMilk(itemStack)) {
            return UseAnim.DRINK;
        }
        return super.getUseAnimation(itemStack);
    }

    //@Override //overrides the (neo)forge implementation
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        //for using a filled bucket as fuel or in crafting recipes, an empty bucket should remain
        return !BucketLibUtil.isEmpty(stack) && !this.isCracked(stack);
    }

    //@Override //overrides the (neo)forge implementation
    public ItemStack getCraftingRemainingItem(ItemStack itemStack) {
        if (!hasCraftingRemainingItem(itemStack)) {
            return ItemStack.EMPTY;
        }
        if (BucketLibUtil.isAffectedByInfinityEnchantment(itemStack)) {
            return itemStack.copy();
        }
        //remove everything from bucket
        ItemStack result = itemStack.copy();
        boolean damaged = BucketLibUtil.containsFluid(result); //damaging is done by fluid handler
        if (BucketLibUtil.containsBlock(result)) {
            result = BucketLibUtil.removeBlock(result, null, null, !damaged); //TODO get ServerLevel!
            damaged = true;
        }
        if (BucketLibUtil.containsEntityType(result)) {
            result = BucketLibUtil.removeEntityType(result, null, null, !damaged); //TODO get ServerLevel!
        }
        if (BucketLibUtil.containsFluid(result) || BucketLibUtil.containsMilk(result)) {
            result = BucketLibUtil.removeFluid(result, null, null); //TODO get ServerLevel!
        }
        return result;
    }

    //@Override //overrides the fabric implementation
    public ItemStack getRecipeRemainder(ItemStack itemStack) {
        return getCraftingRemainingItem(itemStack);
    }

    private boolean getBooleanProperty(Supplier<Boolean> config, boolean defaultValue) {
        if (config != null) {
            return config.get();
        }
        return defaultValue;
    }

    private Integer getIntProperty(Supplier<Integer> config, Integer defaultValue) {
        if (config != null) {
            return config.get();
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private <T> boolean isElementListedInProperty(T element, TagKey<T> tag, List<T> defaultList) {
        if (tag != null) {
            if (element instanceof Block block) {
                return block.defaultBlockState().is((TagKey<Block>) tag);
            } else if (element instanceof Fluid fluid) {
                return fluid.defaultFluidState().is((TagKey<Fluid>) tag);
            } else if (element instanceof EntityType<?> entityType) {
                return entityType.is((TagKey<EntityType<?>>) tag);
            }
        }
        return defaultList != null && defaultList.contains(element);
    }

    public ResourceKey<CreativeModeTab> getCreativeTab() {
        if (this.properties.tab == null) {
            this.properties.tab = Services.PLATFORM.getToolsAndUtilitiesTab();
        }
        return this.properties.tab;
    }

    public int getDurability() {
        return getIntProperty(this.properties.durabilityConfig, this.properties.durability);
    }

    public boolean isDyeable() {
        return this.properties.dyeable;
    }

    public int getDefaultColor() {
        return this.properties.defaultColor;
    }

    public Integer getMaxTemperature() {
        return getIntProperty(this.properties.maxTemperatureConfig, this.properties.maxTemperature);
    }

    public Integer getUpperBreakTemperature() {
        return getIntProperty(this.properties.upperBreakTemperatureConfig, this.properties.upperCrackingTemperature);
    }

    public Integer getLowerBreakTemperature() {
        return getIntProperty(this.properties.lowerCrackingTemperatureConfig, this.properties.lowerCrackingTemperature);
    }

    public Integer getMinTemperature() {
        return getIntProperty(this.properties.minTemperatureConfig, this.properties.minTemperature);
    }

    private boolean isCrackingFluid(Fluid fluid) {
        return isElementListedInProperty(fluid, this.properties.crackingFluidsTag, this.properties.crackingFluids);
    }

    public Integer getBurningTemperature() {
        return getIntProperty(this.properties.burningTemperatureConfig, this.properties.burningTemperature);
    }

    public boolean isBurningFluid(Fluid fluid) {
        return isElementListedInProperty(fluid, this.properties.burningFluidsTag, this.properties.burningFluids);
    }

    public boolean isBurningBlock(Block block) {
        return isElementListedInProperty(block, this.properties.burningBlocksTag, this.properties.burningBlocks);
    }

    public Integer getFreezingTemperature() {
        return getIntProperty(this.properties.freezingTemperatureConfig, this.properties.freezingTemperature);
    }

    public boolean isFreezingFluid(Fluid fluid) {
        return isElementListedInProperty(fluid, this.properties.freezingFluidsTag, this.properties.freezingFluids);
    }

    public boolean isFreezingBlock(Block block) {
        return isElementListedInProperty(block, this.properties.freezingBlocksTag, this.properties.freezingBlocks);
    }

    private boolean isDeniedFluid(Fluid fluid) {
        return isElementListedInProperty(fluid, this.properties.deniedFluidsTag, this.properties.deniedFluids);
    }

    private boolean isAllowedFluid(Fluid fluid) {
        return isElementListedInProperty(fluid, this.properties.allowedFluidsTag, this.properties.allowedFluids);
    }

    public boolean canMilkEntities() {
        return getBooleanProperty(this.properties.milkingConfig, this.properties.milking);
    }

    private boolean canObtainEntities() {
        return getBooleanProperty(this.properties.entityObtainingConfig, this.properties.entityObtaining);
    }

    private boolean isDeniedEntity(EntityType<?> entityType) {
        return isElementListedInProperty(entityType, this.properties.deniedEntitiesTag, this.properties.deniedEntities);
    }

    private boolean isAllowedEntity(EntityType<?> entityType) {
        return isElementListedInProperty(entityType, this.properties.allowedEntitiesTag, this.properties.allowedEntities);
    }

    private boolean canObtainBlocks() {
        return getBooleanProperty(this.properties.blockObtainingConfig, this.properties.blockObtaining);
    }

    private boolean isDeniedBlock(Block block) {
        return isElementListedInProperty(block, this.properties.deniedBlocksTag, this.properties.deniedBlocks);
    }

    private boolean isAllowedBlock(Block block) {
        return isElementListedInProperty(block, this.properties.allowedBlocksTag, this.properties.allowedBlocks);
    }

    public static class Properties {

        ResourceKey<CreativeModeTab> tab = null;
        int maxStackSize = 16;

        int durability = 0;
        Supplier<Integer> durabilityConfig = null;

        boolean dyeable = false;
        int defaultColor = -1;

        Integer maxTemperature = null;
        Supplier<Integer> maxTemperatureConfig = null;
        Integer upperCrackingTemperature = null;
        Supplier<Integer> upperBreakTemperatureConfig = null;
        Integer lowerCrackingTemperature = null;
        Supplier<Integer> lowerCrackingTemperatureConfig = null;
        Integer minTemperature = null;
        Supplier<Integer> minTemperatureConfig = null;

        List<Fluid> crackingFluids = null;
        TagKey<Fluid> crackingFluidsTag = null;
        List<Fluid> deniedFluids = null;
        TagKey<Fluid> deniedFluidsTag = null;
        List<Fluid> allowedFluids = null;
        TagKey<Fluid> allowedFluidsTag = null;

        Integer burningTemperature = null;
        Supplier<Integer> burningTemperatureConfig = null;
        List<Fluid> burningFluids = null;
        TagKey<Fluid> burningFluidsTag = null;
        List<Block> burningBlocks = null;
        TagKey<Block> burningBlocksTag = null;

        Integer freezingTemperature = null;
        Supplier<Integer> freezingTemperatureConfig = null;
        List<Fluid> freezingFluids = null;
        TagKey<Fluid> freezingFluidsTag = null;
        List<Block> freezingBlocks = null;
        TagKey<Block> freezingBlocksTag = null;

        boolean milking = true;
        Supplier<Boolean> milkingConfig = null;

        boolean entityObtaining = true;
        Supplier<Boolean> entityObtainingConfig = null;
        List<EntityType<?>> deniedEntities = null;
        TagKey<EntityType<?>> deniedEntitiesTag = null;
        List<EntityType<?>> allowedEntities = null;
        TagKey<EntityType<?>> allowedEntitiesTag = null;

        boolean blockObtaining = true;
        Supplier<Boolean> blockObtainingConfig = null;
        List<Block> deniedBlocks = null;
        TagKey<Block> deniedBlocksTag = null;
        List<Block> allowedBlocks = null;
        TagKey<Block> allowedBlocksTag = null;

        public Properties tab(ResourceKey<CreativeModeTab> tab) {
            this.tab = tab;
            return this;
        }

        public Properties stacksTo(int maxStackSize) {
            if (maxStackSize < 1) {
                throw new RuntimeException("Unable to have stack size lower than 1.");
            }
            this.maxStackSize = maxStackSize;
            return this;
        }

        /**
         * Sets the default durability as a constant value.
         * Don't forget to add your bucket to the item tag "minecraft:enchantable/durability" to enable the Unbreaking enchanting.
         *
         * @param durability default durability
         * @return Properties object
         */
        public Properties durability(int durability) {
            if (durability < 0) {
                throw new RuntimeException("Unable to have a durability lower than 0.");
            }
            this.durability = durability;
            return this;
        }

        /**
         * Sets the default durability through a config option.
         * Don't forget to add your bucket to the item tag "minecraft:enchantable/durability" to enable the Unbreaking enchanting.
         *
         * @param durabilityConfig supplier of the configuration value
         * @return Properties object
         */
        public Properties durability(Supplier<Integer> durabilityConfig) {
            this.durabilityConfig = durabilityConfig;
            return this;
        }

        /**
         * Sets a default color of the bucket and enables colored rendering.
         * Don't forget to add your bucket to the item tag "minecraft:dyeable" to enable the dye recipe.
         *
         * @param defaultColor color value {@link FastColor.ARGB32}
         * @return Properties object
         */
        public Properties dyeable(int defaultColor) {
            this.dyeable = true;
            this.defaultColor = defaultColor;
            return this;
        }

        /**
         * Sets a default color of the bucket and enables colored rendering.
         * Don't forget to add your bucket to the item tag "minecraft:dyeable" to enable the dye recipe.
         *
         * @param red red value (0-255)
         * @param green green value (0-255)
         * @param blue blue value (0-255)
         * @return Properties object
         */
        public Properties dyeable(int red, int green, int blue) {
            this.dyeable = true;
            this.defaultColor = FastColor.ARGB32.color(red, green, blue);
            return this;
        }

        public Properties maxTemperature(int maxTemperature) {
            this.maxTemperature = maxTemperature;
            return this;
        }

        public Properties maxTemperature(Supplier<Integer> maxTemperatureConfig) {
            this.maxTemperatureConfig = maxTemperatureConfig;
            return this;
        }

        public Properties upperCrackingTemperature(int upperCrackingTemperature) {
            this.upperCrackingTemperature = upperCrackingTemperature;
            return this;
        }

        public Properties upperCrackingTemperature(Supplier<Integer> upperCrackingTemperatureConfig) {
            this.upperBreakTemperatureConfig = upperCrackingTemperatureConfig;
            return this;
        }

        public Properties lowerCrackingTemperature(int lowerBreakTemperature) {
            this.lowerCrackingTemperature = lowerBreakTemperature;
            return this;
        }

        public Properties lowerCrackingTemperature(Supplier<Integer> lowerCrackingTemperatureConfig) {
            this.lowerCrackingTemperatureConfig = lowerCrackingTemperatureConfig;
            return this;
        }

        public Properties minTemperature(int minTemperature) {
            this.minTemperature = minTemperature;
            return this;
        }

        public Properties minTemperature(Supplier<Integer> minTemperatureConfig) {
            this.minTemperatureConfig = minTemperatureConfig;
            return this;
        }

        public Properties crackingFluids(List<Fluid> crackingFluids) {
            this.crackingFluids = crackingFluids;
            return this;
        }

        public Properties crackingFluids(TagKey<Fluid> crackingFluidsTag) {
            this.crackingFluidsTag = crackingFluidsTag;
            return this;
        }

        public Properties burningTemperature(int burningTemperature) {
            this.burningTemperature = burningTemperature;
            return this;
        }

        public Properties burningTemperature(Supplier<Integer> burningTemperatureConfig) {
            this.burningTemperatureConfig = burningTemperatureConfig;
            return this;
        }

        public Properties burningFluids(List<Fluid> burningFluids) {
            this.burningFluids = burningFluids;
            return this;
        }

        public Properties burningFluids(TagKey<Fluid> burningFluidsTag) {
            this.burningFluidsTag = burningFluidsTag;
            return this;
        }

        public Properties burningBlocks(List<Block> burningBlocks) {
            this.burningBlocks = burningBlocks;
            return this;
        }

        public Properties burningBlocks(TagKey<Block> burningBlocksTag) {
            this.burningBlocksTag = burningBlocksTag;
            return this;
        }

        public Properties freezingTemperature(int freezingTemperature) {
            this.freezingTemperature = freezingTemperature;
            return this;
        }

        public Properties freezingTemperature(Supplier<Integer> freezingTemperatureConfig) {
            this.freezingTemperatureConfig = freezingTemperatureConfig;
            return this;
        }

        public Properties freezingFluids(List<Fluid> freezingFluids) {
            this.freezingFluids = freezingFluids;
            return this;
        }

        public Properties freezingFluids(TagKey<Fluid> freezingFluidsTag) {
            this.freezingFluidsTag = freezingFluidsTag;
            return this;
        }

        public Properties freezingBlocks(List<Block> freezingBlocks) {
            this.freezingBlocks = freezingBlocks;
            return this;
        }

        public Properties freezingBlocks(TagKey<Block> freezingBlocksTag) {
            this.freezingBlocksTag = freezingBlocksTag;
            return this;
        }

        public Properties deniedFluids(List<Fluid> deniedFluids) {
            this.deniedFluids = deniedFluids;
            return this;
        }

        public Properties deniedFluids(TagKey<Fluid> blockedFluidsTag) {
            this.deniedFluidsTag = blockedFluidsTag;
            return this;
        }

        public Properties allowedFluids(List<Fluid> allowedFluids) {
            this.allowedFluids = allowedFluids;
            return this;
        }

        public Properties allowedFluids(TagKey<Fluid> allowedFluidsTag) {
            this.allowedFluidsTag = allowedFluidsTag;
            return this;
        }

        public Properties disableMilking() {
            this.milking = false;
            return this;
        }

        public Properties milking(Supplier<Boolean> milkingConfig) {
            this.milkingConfig = milkingConfig;
            return this;
        }

        public Properties disableEntityObtaining() {
            this.entityObtaining = false;
            return this;
        }

        public Properties entityObtaining(Supplier<Boolean> entityObtainingConfig) {
            this.entityObtainingConfig = entityObtainingConfig;
            return this;
        }

        public Properties deniedEntities(List<EntityType<?>> deniedEntities) {
            this.deniedEntities = deniedEntities;
            return this;
        }

        public Properties deniedEntities(TagKey<EntityType<?>> deniedEntitiesTag) {
            this.deniedEntitiesTag = deniedEntitiesTag;
            return this;
        }

        public Properties allowedEntities(List<EntityType<?>> allowedEntities) {
            this.allowedEntities = allowedEntities;
            return this;
        }

        public Properties allowedEntities(TagKey<EntityType<?>> allowedEntitiesTag) {
            this.allowedEntitiesTag = allowedEntitiesTag;
            return this;
        }

        public Properties disableBlockObtaining() {
            this.blockObtaining = false;
            return this;
        }

        public Properties blockObtaining(Supplier<Boolean> blockObtainingConfig) {
            this.blockObtainingConfig = blockObtainingConfig;
            return this;
        }

        public Properties deniedBlocks(List<Block> deniedBlocks) {
            this.deniedBlocks = deniedBlocks;
            return this;
        }

        public Properties deniedBlocks(TagKey<Block> deniedBlocksTag) {
            this.deniedBlocksTag = deniedBlocksTag;
            return this;
        }

        public Properties allowedBlocks(List<Block> allowedBlocks) {
            this.allowedBlocks = allowedBlocks;
            return this;
        }

        public Properties allowedBlocks(TagKey<Block> allowedBlocksTag) {
            this.allowedBlocksTag = allowedBlocksTag;
            return this;
        }

    }

}
