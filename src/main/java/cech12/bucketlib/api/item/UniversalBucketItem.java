package cech12.bucketlib.api.item;

import cech12.bucketlib.api.BucketLibTags;
import cech12.bucketlib.config.ServerConfig;
import cech12.bucketlib.item.UniversalBucketFluidHandler;
import cech12.bucketlib.util.BucketLibUtil;
import cech12.bucketlib.util.ColorUtil;
import cech12.bucketlib.util.RegistryUtil;
import cech12.bucketlib.util.WorldInteractionUtil;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class UniversalBucketItem extends Item {

    private final Properties properties;

    public UniversalBucketItem(Properties properties) {
        super((new Item.Properties()).tab(properties.tab));
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
            argument = (entityType != null) ? entityType.getDescription() : new TextComponent("?");
        } else if (BucketLibUtil.containsFluid(stack)) {
            descriptionId += ".filled";
            Fluid fluid = BucketLibUtil.getFluid(stack);
            argument = new TranslatableComponent(fluid.getAttributes().getTranslationKey());
        } else if (BucketLibUtil.containsBlock(stack)) {
            descriptionId += ".filled";
            Block block = BucketLibUtil.getBlock(stack);
            argument = (block != null) ? block.getName() : new TextComponent("?");
        } else if (BucketLibUtil.containsMilk(stack)) {
            descriptionId += ".filled";
            argument = new TranslatableComponent("fluid.minecraft.milk");
        } else {
            //is empty
            return new TranslatableComponent(descriptionId);
        }
        return new TranslatableComponent(descriptionId, argument);
    }

    public boolean isCracked(ItemStack stack) {
        FluidStack fluidStack = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        if (!fluidStack.isEmpty()) {
            Fluid fluid = fluidStack.getFluid();
            int fluidTemperature = fluid.getAttributes().getTemperature();
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
        Item bucket = fluid.getBucket();
        if (!(bucket instanceof BucketItem) || ((BucketItem) bucket).getFluid() != fluid) {
            return false;
        }
        if (this.properties.allowedFluidsTag != null || this.properties.allowedFluids != null) {
            return isAllowedFluid(fluid);
        }
        if (this.properties.blockedFluidsTag != null || this.properties.blockedFluids != null) {
            return !isBlockedFluid(fluid);
        }
        int fluidTemperature = fluid.getAttributes().getTemperature();
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
            if (this.properties.blockedEntitiesTag != null || this.properties.blockedEntities != null) {
                return !isBlockedEntity(entityType);
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
            if (this.properties.blockedBlocksTag != null || this.properties.blockedBlocks != null) {
                return !isBlockedBlock(block);
            }
            return true;
        }
        return false;
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return BucketLibUtil.isEmpty(stack) ? this.properties.maxStackSize : 1;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return this.getDurability() > 0;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return this.getDurability();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        //overwrite hardcoded maxDamage
        return Math.round(13.0F - (float)stack.getDamageValue() * 13.0F / (float)this.getMaxDamage(stack));
    }

    @Override
    public int getBarColor(@Nonnull ItemStack stack) {
        //overwrite hardcoded maxDamage
        float f = Math.max(0.0F, ((float)this.getMaxDamage(stack) - (float)stack.getDamageValue()) / (float)this.getMaxDamage(stack));
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void inventoryTick(@Nonnull ItemStack itemStack, @Nonnull Level level, @Nonnull Entity entity, int position, boolean selected) {
        if (!level.isClientSide) {
            if (!entity.fireImmune() && this.hasBurningContent(itemStack)) {
                entity.setTicksFrozen(0); //avoid extinguish sounds
                entity.setSecondsOnFire(5);
                if (BucketLibUtil.notCreative(entity) && entity.tickCount % 20 == 0) {
                    BucketLibUtil.damageByOne(itemStack);
                }
            } else if (!entity.isOnFire() && entity.canFreeze() && this.hasFreezingContent(itemStack)) {
                int ticks = entity.getTicksFrozen() + (entity.isInPowderSnow ? 1 : 3); //2 are subtracted when not in powder snow
                entity.setTicksFrozen(Math.min(entity.getTicksRequiredToFreeze(), ticks));
                //damaging here because, the vanilla mechanic is reducing the freeze ticks below fully freezing
                if (BucketLibUtil.notCreative(entity) && entity.tickCount % 40 == 0 && entity.isFullyFrozen()) {
                    entity.hurt(DamageSource.FREEZE, entity.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES) ? 5 : 1);
                    BucketLibUtil.damageByOne(itemStack);
                }
            }
        }
    }

    private boolean hasBurningContent(@Nonnull ItemStack itemStack) {
        Integer burningTemperature = this.getBurningTemperature();
        Fluid fluid = BucketLibUtil.getFluid(itemStack);
        return fluid != Fluids.EMPTY && (burningTemperature != null && fluid.getAttributes().getTemperature() >= burningTemperature || this.isBurningFluid(fluid))
                || this.isBurningBlock(BucketLibUtil.getBlock(itemStack));
    }

    private boolean hasFreezingContent(@Nonnull ItemStack itemStack) {
        Integer freezingTemperature = this.getFreezingTemperature();
        Fluid fluid = BucketLibUtil.getFluid(itemStack);
        return fluid != Fluids.EMPTY && (freezingTemperature != null && fluid.getAttributes().getTemperature() <= freezingTemperature || this.isFreezingFluid(fluid))
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
            if (isEmpty) {
                //pickup from cauldron interaction
                InteractionResultHolder<ItemStack> caldronInteractionResult = WorldInteractionUtil.tryPickupFromCauldron(level, player, interactionHand, blockHitResult);
                if (caldronInteractionResult.getResult().consumesAction()) {
                    return caldronInteractionResult;
                }
                FluidActionResult fluidActionResult = FluidUtil.tryPickUpFluid(itemstack, player, level, hitBlockPos, hitDirection);
                if (fluidActionResult.isSuccess()) {
                    return InteractionResultHolder.sidedSuccess(ItemUtils.createFilledResult(itemstack, player, fluidActionResult.getResult()), level.isClientSide());
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
                        return new InteractionResultHolder<>(interactionResult.getResult(), ItemUtils.createFilledResult(itemstack, player, BucketLibUtil.addBlock(itemstack, bucketBlock.block())));
                    }
                }
            } else {
                //place into cauldron interaction
                InteractionResultHolder<ItemStack> caldronInteractionResult = WorldInteractionUtil.tryPlaceIntoCauldron(level, player, interactionHand, blockHitResult);
                if (caldronInteractionResult.getResult().consumesAction()) {
                    return caldronInteractionResult;
                }
                if (BucketLibUtil.containsFluid(itemstack)) {
                    //place fluid interaction
                    FluidStack fluidStack = FluidUtil.getFluidContained(itemstack).orElse(FluidStack.EMPTY);
                    FluidActionResult fluidActionResult = FluidUtil.tryPlaceFluid(player, level, interactionHand, relativeBlockPos, itemstack, fluidStack);
                    if (fluidActionResult.isSuccess()) {
                        ItemStack emptyBucket = fluidActionResult.getResult();
                        if (BucketLibUtil.containsEntityType(emptyBucket)) {
                            //place entity if exists
                            emptyBucket = spawnEntityFromBucket(player, level, emptyBucket, relativeBlockPos, false);
                        }
                        return InteractionResultHolder.sidedSuccess(BucketLibUtil.createEmptyResult(itemstack, player, emptyBucket, interactionHand), level.isClientSide());
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
                            return new InteractionResultHolder<>(interactionResult, BucketLibUtil.createEmptyResult(itemstack, player, BucketLibUtil.removeBlock(itemstack), interactionHand));
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
                    bucketable.loadFromBucketTag(itemStack.getOrCreateTag());
                    bucketable.setFromBucket(true);
                }
                if (player != null) {
                    serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
                }
                return BucketLibUtil.removeEntityType(itemStack, damage);
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
        //TODO feed axolotl?
        return super.interactLivingEntity(itemStack, player, entity, interactionHand);
    }

    private <T extends LivingEntity & Bucketable> InteractionResult pickupEntityWithBucket(Player player, InteractionHand interactionHand, T entity) {
        ItemStack itemStack = player.getItemInHand(interactionHand).copy(); //copy to avoid changing the real item stack
        Fluid containedFluid = FluidUtil.getFluidContained(itemStack).orElse(FluidStack.EMPTY).getFluid();
        Fluid entityBucketFluid = ((BucketItem) entity.getBucketItemStack().getItem()).getFluid();
        if (itemStack.getItem() instanceof UniversalBucketItem
                && entity.isAlive()
                && entityBucketFluid == containedFluid) {
            entity.playSound(entity.getPickupSound(), 1.0F, 1.0F);
            ItemStack filledItemStack = BucketLibUtil.addEntityType(itemStack, entity.getType());
            entity.saveToBucketTag(filledItemStack);
            Level level = entity.level;
            ItemStack handItemStack = ItemUtils.createFilledResult(itemStack, player, filledItemStack, false);
            player.setItemInHand(interactionHand, handItemStack);
            if (!level.isClientSide) {
                CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer)player, filledItemStack);
            }
            entity.discard();
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    @Nonnull
    public ItemStack finishUsingItem(@Nonnull ItemStack itemStack, @Nonnull Level level, @Nonnull LivingEntity player) {
        if (!level.isClientSide) {
            player.curePotionEffects(itemStack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, new ItemStack(Items.MILK_BUCKET));
            serverPlayer.awardStat(Stats.ITEM_USED.get(Items.MILK_BUCKET));
        }
        if (BucketLibUtil.notCreative(player)) {
            return BucketLibUtil.removeMilk(itemStack);
        }
        return itemStack;
    }

    @Override
    public int getUseDuration(@Nonnull ItemStack itemStack) {
        if (BucketLibUtil.containsMilk(itemStack)) {
            return 32;
        }
        return super.getUseDuration(itemStack);
    }

    @Override
    @Nonnull
    public UseAnim getUseAnimation(@Nonnull ItemStack itemStack) {
        if (BucketLibUtil.containsMilk(itemStack)) {
            return UseAnim.DRINK;
        }
        return super.getUseAnimation(itemStack);
    }

    /**
     * returns a list of items with the same ID, but different meta (eg: dye returns 16 items)
     */
    @Override
    public void fillItemCategory(@Nonnull CreativeModeTab group, @Nonnull NonNullList<ItemStack> items) {
        if (this.allowdedIn(group)) {
            ItemStack emptyBucket = new ItemStack(this);
            //add empty bucket
            items.add(emptyBucket);
            //add fluid buckets
            for (Fluid fluid : ForgeRegistries.FLUIDS) {
                if (fluid == Fluids.EMPTY) {
                    continue;
                }
                if (ForgeMod.MILK.isPresent() && ForgeMod.MILK.get().isSame(fluid)) {
                    //skip milk fluid
                    continue;
                }
                if (canHoldFluid(fluid)) {
                    items.add(BucketLibUtil.addFluid(emptyBucket, fluid));
                }
            }
            //add milk bucket
            items.add(BucketLibUtil.addMilk(emptyBucket));
            //add entity buckets
            for (RegistryUtil.BucketEntity bucketEntity : RegistryUtil.getBucketEntities()) {
                if (canHoldEntity(bucketEntity.entityType()) && canHoldFluid(bucketEntity.fluid())) {
                    ItemStack filledBucket = BucketLibUtil.addFluid(emptyBucket, bucketEntity.fluid());
                    filledBucket = BucketLibUtil.addEntityType(filledBucket, bucketEntity.entityType());
                    items.add(filledBucket);
                }
            }
            //add block buckets
            for (RegistryUtil.BucketBlock bucketBlock : RegistryUtil.getBucketBlocks()) {
                if (canHoldBlock(bucketBlock.block())) {
                    items.add(BucketLibUtil.addBlock(emptyBucket, bucketBlock.block()));
                }
            }
        }
    }

    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
        //TODO block & entity buckets have no burn time
        Fluid fluid = FluidUtil.getFluidContained(itemStack).orElse(FluidStack.EMPTY).getFluid();
        if (fluid != Fluids.EMPTY) {
            //all fluids have their burn time in their bucket item.
            //get the burn time via ForgeHooks.getBurnTime to let other mods change burn times of buckets of vanilla and other fluids.
            return ForgeHooks.getBurnTime(new ItemStack(fluid.getBucket()), recipeType);
        }
        return super.getBurnTime(itemStack, recipeType);
    }

    @Override
    public boolean hasContainerItem(ItemStack stack) {
        //for using a filled bucket as fuel or in crafting recipes, an empty bucket should remain
        return !this.isCracked(stack);
    }


    @Override
    public ItemStack getContainerItem(ItemStack itemStack) {
        if (BucketLibUtil.isAffectedByInfinityEnchantment(itemStack)) {
            return itemStack.copy();
        }
        //TODO remove other content (Entity, Blocks, ...)
        return BucketLibUtil.removeFluid(itemStack);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        if (enchantment == Enchantments.INFINITY_ARROWS
                && ServerConfig.INFINITY_ENCHANTMENT_ENABLED.get()
                && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, stack) <= 0
                && FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY).getFluid().defaultFluidState().is(BucketLibTags.Fluids.INFINITY_ENCHANTABLE)) {
            return true;
        }
        return super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public ICapabilityProvider initCapabilities(@Nonnull ItemStack stack, @Nullable CompoundTag nbt) {
        return new UniversalBucketFluidHandler(stack);
    }

    private boolean getBooleanProperty(ForgeConfigSpec.BooleanValue config, boolean defaultValue) {
        if (config != null) {
            return config.get();
        }
        return defaultValue;
    }

    private Integer getIntProperty(ForgeConfigSpec.IntValue config, Integer defaultValue) {
        if (config != null) {
            return config.get();
        }
        return defaultValue;
    }

    private <T> boolean isElementListedInProperty(T element, TagKey<T> tag, List<T> defaultList) {
        if (tag != null) {
            if (element instanceof Block block) {
                return block.defaultBlockState().is((TagKey<Block>) tag);
            } else if (element instanceof Fluid fluid) {
                return fluid.is((TagKey<Fluid>) tag);
            } else if (element instanceof EntityType<?> entityType) {
                return entityType.is((TagKey<EntityType<?>>) tag);
            }
        }
        return defaultList != null && defaultList.contains(element);
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

    private boolean isBlockedFluid(Fluid fluid) {
        return isElementListedInProperty(fluid, this.properties.blockedFluidsTag, this.properties.blockedFluids);
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

    private boolean isBlockedEntity(EntityType<?> entityType) {
        return isElementListedInProperty(entityType, this.properties.blockedEntitiesTag, this.properties.blockedEntities);
    }

    private boolean isAllowedEntity(EntityType<?> entityType) {
        return isElementListedInProperty(entityType, this.properties.allowedEntitiesTag, this.properties.allowedEntities);
    }

    private boolean canObtainBlocks() {
        return getBooleanProperty(this.properties.blockObtainingConfig, this.properties.blockObtaining);
    }

    private boolean isBlockedBlock(Block block) {
        return isElementListedInProperty(block, this.properties.blockedBlocksTag, this.properties.blockedBlocks);
    }

    private boolean isAllowedBlock(Block block) {
        return isElementListedInProperty(block, this.properties.allowedBlocksTag, this.properties.allowedBlocks);
    }

    public static class Properties {

        CreativeModeTab tab = CreativeModeTab.TAB_MISC;
        int maxStackSize = 16;

        int durability = 0;
        ForgeConfigSpec.IntValue durabilityConfig = null;

        boolean dyeable = false;
        int defaultColor = -1;

        Integer maxTemperature = null;
        ForgeConfigSpec.IntValue maxTemperatureConfig = null;
        Integer upperCrackingTemperature = null;
        ForgeConfigSpec.IntValue upperBreakTemperatureConfig = null;
        Integer lowerCrackingTemperature = null;
        ForgeConfigSpec.IntValue lowerCrackingTemperatureConfig = null;
        Integer minTemperature = null;
        ForgeConfigSpec.IntValue minTemperatureConfig = null;

        List<Fluid> crackingFluids = null;
        TagKey<Fluid> crackingFluidsTag = null;
        List<Fluid> blockedFluids = null;
        TagKey<Fluid> blockedFluidsTag = null;
        List<Fluid> allowedFluids = null;
        TagKey<Fluid> allowedFluidsTag = null;

        Integer burningTemperature = null;
        ForgeConfigSpec.IntValue burningTemperatureConfig = null;
        List<Fluid> burningFluids = null;
        TagKey<Fluid> burningFluidsTag = null;
        List<Block> burningBlocks = null;
        TagKey<Block> burningBlocksTag = null;

        Integer freezingTemperature = null;
        ForgeConfigSpec.IntValue freezingTemperatureConfig = null;
        List<Fluid> freezingFluids = null;
        TagKey<Fluid> freezingFluidsTag = null;
        List<Block> freezingBlocks = null;
        TagKey<Block> freezingBlocksTag = null;

        boolean milking = true;
        ForgeConfigSpec.BooleanValue milkingConfig = null;

        boolean entityObtaining = true;
        ForgeConfigSpec.BooleanValue entityObtainingConfig = null;
        List<EntityType<?>> blockedEntities = null;
        TagKey<EntityType<?>> blockedEntitiesTag = null;
        List<EntityType<?>> allowedEntities = null;
        TagKey<EntityType<?>> allowedEntitiesTag = null;

        boolean blockObtaining = true;
        ForgeConfigSpec.BooleanValue blockObtainingConfig = null;
        List<Block> blockedBlocks = null;
        TagKey<Block> blockedBlocksTag = null;
        List<Block> allowedBlocks = null;
        TagKey<Block> allowedBlocksTag = null;

        public Properties tab(CreativeModeTab tab) {
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

        public Properties durability(int durability) {
            if (durability < 0) {
                throw new RuntimeException("Unable to have a durability lower than 0.");
            }
            this.durability = durability;
            return this;
        }

        public Properties durability(ForgeConfigSpec.IntValue durabilityConfig) {
            this.durabilityConfig = durabilityConfig;
            return this;
        }

        public Properties dyeable(int defaultColor) {
            this.dyeable = true;
            this.defaultColor = defaultColor;
            return this;
        }

        public Properties dyeable(int red, int green, int blue) {
            this.dyeable = true;
            this.defaultColor = ColorUtil.getColorFromRGB(red, green, blue);
            return this;
        }

        public Properties maxTemperature(int maxTemperature) {
            this.maxTemperature = maxTemperature;
            return this;
        }

        public Properties maxTemperature(ForgeConfigSpec.IntValue maxTemperatureConfig) {
            this.maxTemperatureConfig = maxTemperatureConfig;
            return this;
        }

        public Properties upperCrackingTemperature(int upperCrackingTemperature) {
            this.upperCrackingTemperature = upperCrackingTemperature;
            return this;
        }

        public Properties upperCrackingTemperature(ForgeConfigSpec.IntValue upperCrackingTemperatureConfig) {
            this.upperBreakTemperatureConfig = upperCrackingTemperatureConfig;
            return this;
        }

        public Properties lowerCrackingTemperature(int lowerBreakTemperature) {
            this.lowerCrackingTemperature = lowerBreakTemperature;
            return this;
        }

        public Properties lowerCrackingTemperature(ForgeConfigSpec.IntValue lowerCrackingTemperatureConfig) {
            this.lowerCrackingTemperatureConfig = lowerCrackingTemperatureConfig;
            return this;
        }

        public Properties minTemperature(int minTemperature) {
            this.minTemperature = minTemperature;
            return this;
        }

        public Properties minTemperature(ForgeConfigSpec.IntValue minTemperatureConfig) {
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

        public Properties burningTemperature(ForgeConfigSpec.IntValue burningTemperatureConfig) {
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

        public Properties freezingTemperature(ForgeConfigSpec.IntValue freezingTemperatureConfig) {
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

        public Properties blockedFluids(List<Fluid> blockedFluids) {
            this.blockedFluids = blockedFluids;
            return this;
        }

        public Properties blockedFluids(TagKey<Fluid> blockedFluidsTag) {
            this.blockedFluidsTag = blockedFluidsTag;
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

        public Properties milking(ForgeConfigSpec.BooleanValue milkingConfig) {
            this.milkingConfig = milkingConfig;
            return this;
        }

        public Properties disableEntityObtaining() {
            this.entityObtaining = false;
            return this;
        }

        public Properties entityObtaining(ForgeConfigSpec.BooleanValue entityObtainingConfig) {
            this.entityObtainingConfig = entityObtainingConfig;
            return this;
        }

        public Properties blockedEntities(List<EntityType<?>> blockedEntities) {
            this.blockedEntities = blockedEntities;
            return this;
        }

        public Properties blockedEntities(TagKey<EntityType<?>> blockedEntitiesTag) {
            this.blockedEntitiesTag = blockedEntitiesTag;
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

        public Properties blockObtaining(ForgeConfigSpec.BooleanValue blockObtainingConfig) {
            this.blockObtainingConfig = blockObtainingConfig;
            return this;
        }

        public Properties blockedBlocks(List<Block> blockedBlocks) {
            this.blockedBlocks = blockedBlocks;
            return this;
        }

        public Properties blockedBlocks(TagKey<Block> blockedBlocksTag) {
            this.blockedBlocksTag = blockedBlocksTag;
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
