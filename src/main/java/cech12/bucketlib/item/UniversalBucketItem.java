package cech12.bucketlib.item;

import cech12.bucketlib.api.BucketLibTags;
import cech12.bucketlib.config.ServerConfig;
import cech12.bucketlib.util.BucketLibUtil;
import cech12.bucketlib.util.ColorUtil;
import cech12.bucketlib.util.RegistryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.Tag;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        //TODO only for debugging
        FluidUtil.getFluidContained(stack).ifPresent(fluidStack ->
                tooltip.add(new TextComponent("Fluid: " + fluidStack.getRawFluid().getRegistryName()).withStyle(ChatFormatting.BLUE))
        );
        if (BucketLibUtil.containsMilk(stack)) {
            tooltip.add(new TextComponent("Contains Milk").withStyle(ChatFormatting.WHITE));
        }
        if (BucketLibUtil.containsEntityType(stack)) {
            tooltip.add(new TextComponent("EntityType: " + BucketLibUtil.getEntityType(stack)).withStyle(ChatFormatting.GREEN));
        }
        if (BucketLibUtil.containsBlock(stack)) {
            tooltip.add(new TextComponent("Block: " + BucketLibUtil.getBlock(stack)).withStyle(ChatFormatting.RED));
        }
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
        return BucketLibUtil.containsFluid(stack) ? 1 : this.properties.maxStackSize;
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
                //pickup fluid interaction
                if (hitBlockState.getBlock() instanceof AbstractCauldronBlock) {
                    //check if bucket can hold cauldron content
                    if (hitBlockState.getBlock() == Blocks.LAVA_CAULDRON && canHoldFluid(Fluids.LAVA)
                            || hitBlockState.getBlock() == Blocks.WATER_CAULDRON && canHoldFluid(Fluids.WATER)) {
                        //fake vanilla bucket using on cauldron
                        player.setItemInHand(interactionHand, new ItemStack(Items.BUCKET));
                        InteractionResult interactionResult = hitBlockState.use(level, player, interactionHand, blockHitResult);
                        FluidStack resultFluidStack = FluidUtil.getFluidContained(player.getItemInHand(interactionHand)).orElse(FluidStack.EMPTY);
                        player.setItemInHand(interactionHand, itemstack);
                        if (interactionResult.consumesAction()) {
                            return new InteractionResultHolder<>(interactionResult, ItemUtils.createFilledResult(itemstack, player, BucketLibUtil.addFluid(itemstack, resultFluidStack.getFluid())));
                        }
                    }
                }
                FluidActionResult fluidActionResult = FluidUtil.tryPickUpFluid(itemstack, player, level, hitBlockPos, hitDirection);
                if (fluidActionResult.isSuccess()) {
                    return InteractionResultHolder.sidedSuccess(ItemUtils.createFilledResult(itemstack, player, fluidActionResult.getResult()), level.isClientSide());
                }
                //pickup block interaction
                //TODO Cauldron interaction
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
            } else if (BucketLibUtil.containsFluid(itemstack)) {
                //place fluid interaction
                FluidStack fluidStack = FluidUtil.getFluidContained(itemstack).orElse(FluidStack.EMPTY);
                if (hitBlockState.getBlock() instanceof AbstractCauldronBlock && !BucketLibUtil.containsEntityType(itemstack)) {
                    //fake vanilla bucket using on cauldron
                    player.setItemInHand(interactionHand, new ItemStack(fluidStack.getFluid().getBucket()));
                    InteractionResult interactionResult = hitBlockState.use(level, player, interactionHand, blockHitResult);
                    player.setItemInHand(interactionHand, itemstack);
                    if (interactionResult.consumesAction()) {
                        return new InteractionResultHolder<>(interactionResult, createEmptyResult(itemstack, player, BucketLibUtil.removeFluid(itemstack)));
                    }
                }
                FluidActionResult fluidActionResult = FluidUtil.tryPlaceFluid(player, level, interactionHand, relativeBlockPos, itemstack, fluidStack);
                if (fluidActionResult.isSuccess()) {
                    ItemStack emptyBucket = fluidActionResult.getResult();
                    if (BucketLibUtil.containsEntityType(emptyBucket)) {
                        //place entity if exists
                        emptyBucket = spawnEntityFromBucket(player, level, emptyBucket, relativeBlockPos);
                    }
                    return InteractionResultHolder.sidedSuccess(this.createEmptyResult(itemstack, player, emptyBucket), level.isClientSide());
                }
            } else if (BucketLibUtil.containsEntityType(itemstack)) {
                //place entity interaction
                ItemStack emptyBucket = spawnEntityFromBucket(player, level, itemstack, relativeBlockPos);
                return InteractionResultHolder.sidedSuccess(this.createEmptyResult(itemstack, player, emptyBucket), level.isClientSide());
            } else if (BucketLibUtil.containsBlock(itemstack)) {
                //place block interaction
                //TODO Cauldron interaction
                Block block = BucketLibUtil.getBlock(itemstack);
                RegistryUtil.BucketBlock bucketBlock = RegistryUtil.getBucketBlock(block);
                if (block != null && bucketBlock != null) {
                    //fake vanilla bucket use
                    ItemStack fakeStack = new ItemStack(bucketBlock.bucketItem());
                    player.setItemInHand(interactionHand, fakeStack);
                    InteractionResult interactionResult = fakeStack.useOn(new UseOnContext(player, interactionHand, blockHitResult));
                    player.setItemInHand(interactionHand, itemstack);
                    if (interactionResult.consumesAction()) {
                        return new InteractionResultHolder<>(interactionResult, createEmptyResult(itemstack, player, BucketLibUtil.removeBlock(itemstack)));
                    }
                }
            }
        }
        if (BucketLibUtil.containsMilk(itemstack)) {
            return ItemUtils.startUsingInstantly(level, player, interactionHand);
        }
        return InteractionResultHolder.pass(itemstack);
    }

    private ItemStack spawnEntityFromBucket(@Nonnull Player player, Level level, ItemStack itemStack, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            EntityType<?> entityType = BucketLibUtil.getEntityType(itemStack);
            if (entityType != null) {
                Entity entity = entityType.spawn(serverLevel, itemStack, null, pos, MobSpawnType.BUCKET, true, false);
                if (entity instanceof Bucketable bucketable) {
                    bucketable.loadFromBucketTag(itemStack.getOrCreateTag());
                    bucketable.setFromBucket(true);
                }
                serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
                return BucketLibUtil.removeEntityType(itemStack);
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
            return BucketLibUtil.tryMilkLivingEntity(itemStack, entity, player, interactionHand);
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
        if (player instanceof Player && !((Player)player).getAbilities().instabuild) {
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

    private ItemStack createEmptyResult(ItemStack initialStack, Player player, ItemStack resultStack) {
        return player.getAbilities().instabuild ? initialStack : resultStack;
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
                if (canHoldFluid(fluid)) {
                    items.add(BucketLibUtil.addFluid(emptyBucket, fluid));
                }
            }
            //add milk bucket if fluid does not exist
            if (!ForgeMod.MILK.isPresent() && this.canMilkEntities()) {
                items.add(BucketLibUtil.addMilk(emptyBucket));
            }
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
                && FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY).getFluid().is(BucketLibTags.Fluids.INFINITY_ENCHANTABLE)) {
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

    private <T> boolean isElementListedInProperty(T element, Tag<T> tag, List<T> defaultList) {
        if (tag != null) {
            return tag.contains(element);
        }
        return defaultList != null && defaultList.contains(element);
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

    private boolean isBlockedFluid(Fluid fluid) {
        return isElementListedInProperty(fluid, this.properties.blockedFluidsTag, this.properties.blockedFluids);
    }

    private boolean isAllowedFluid(Fluid fluid) {
        return isElementListedInProperty(fluid, this.properties.allowedFluidsTag, this.properties.allowedFluids);
    }

    private boolean canMilkEntities() {
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
        Tag<Fluid> crackingFluidsTag = null;
        List<Fluid> blockedFluids = null;
        Tag<Fluid> blockedFluidsTag = null;
        List<Fluid> allowedFluids = null;
        Tag<Fluid> allowedFluidsTag = null;

        boolean milking = true;
        ForgeConfigSpec.BooleanValue milkingConfig = null;

        boolean entityObtaining = true;
        ForgeConfigSpec.BooleanValue entityObtainingConfig = null;
        List<EntityType<?>> blockedEntities = null;
        Tag<EntityType<?>> blockedEntitiesTag = null;
        List<EntityType<?>> allowedEntities = null;
        Tag<EntityType<?>> allowedEntitiesTag = null;

        boolean blockObtaining = true;
        ForgeConfigSpec.BooleanValue blockObtainingConfig = null;
        List<Block> blockedBlocks = null;
        Tag<Block> blockedBlocksTag = null;
        List<Block> allowedBlocks = null;
        Tag<Block> allowedBlocksTag = null;

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

        public Properties crackingFluids(Tag<Fluid> crackingFluidsTag) {
            this.crackingFluidsTag = crackingFluidsTag;
            return this;
        }

        public Properties blockedFluids(List<Fluid> blockedFluids) {
            this.blockedFluids = blockedFluids;
            return this;
        }

        public Properties blockedFluids(Tag<Fluid> blockedFluidsTag) {
            this.blockedFluidsTag = blockedFluidsTag;
            return this;
        }

        public Properties allowedFluids(List<Fluid> allowedFluids) {
            this.allowedFluids = allowedFluids;
            return this;
        }

        public Properties allowedFluids(Tag<Fluid> allowedFluidsTag) {
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

        public Properties blockedEntities(Tag<EntityType<?>> blockedEntitiesTag) {
            this.blockedEntitiesTag = blockedEntitiesTag;
            return this;
        }

        public Properties allowedEntities(List<EntityType<?>> allowedEntities) {
            this.allowedEntities = allowedEntities;
            return this;
        }

        public Properties allowedEntities(Tag<EntityType<?>> allowedEntitiesTag) {
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

        public Properties blockedBlocks(Tag<Block> blockedBlocksTag) {
            this.blockedBlocksTag = blockedBlocksTag;
            return this;
        }

        public Properties allowedBlocks(List<Block> allowedBlocks) {
            this.allowedBlocks = allowedBlocks;
            return this;
        }

        public Properties allowedBlocks(Tag<Block> allowedBlocksTag) {
            this.allowedBlocksTag = allowedBlocksTag;
            return this;
        }

    }

}
