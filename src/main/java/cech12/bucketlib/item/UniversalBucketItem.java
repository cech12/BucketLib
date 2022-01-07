package cech12.bucketlib.item;

import cech12.bucketlib.util.ColorUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.tags.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class UniversalBucketItem extends Item {

    //TODO Entity
    //TODO Blocks

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
    }

    public boolean isCracked(ItemStack stack) {
        //TODO infinity enchantment
        FluidStack fluidStack = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        if (!fluidStack.isEmpty()) {
            Fluid fluid = fluidStack.getFluid();
            int fluidTemperature = fluid.getAttributes().getTemperature();
            Integer upperCrackingTemperature = getUpperBreakTemperature();
            Integer lowerCrackingTemperature = getLowerBreakTemperature();
            return isCrackingFluid(fluid)
                    || (upperCrackingTemperature != null && fluidTemperature >= upperCrackingTemperature)
                    || (lowerCrackingTemperature != null && fluidTemperature <= lowerCrackingTemperature);
        }
        return false;
    }

    public boolean canHoldFluid(Fluid fluid) {
        Item bucket = fluid.getBucket();
        if (!(bucket instanceof BucketItem) || ((BucketItem) bucket).getFluid() != fluid) { //TODO forge milk fluid
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

    private boolean containsFluid(ItemStack stack) {
        AtomicBoolean containsFluid = new AtomicBoolean(false);
        FluidUtil.getFluidContained(stack).ifPresent(fluidStack -> containsFluid.set(!fluidStack.isEmpty()));
        return containsFluid.get();
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return containsFluid(stack) ? 1 : this.properties.maxStackSize;
    }

    @Override
    @Nonnull
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand interactionHand) {
        ItemStack itemstack = player.getItemInHand(interactionHand);
        EquipmentSlot equipmentSlot = interactionHand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        boolean containsFluid = containsFluid(itemstack);
        //check hit block
        BlockHitResult blockHitResult = getPlayerPOVHitResult(level, player, containsFluid ? ClipContext.Fluid.NONE : ClipContext.Fluid.SOURCE_ONLY);
        if (blockHitResult.getType() == HitResult.Type.MISS || blockHitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(itemstack);
        }
        BlockPos hitBlockPos = blockHitResult.getBlockPos();
        BlockState hitBlockState = level.getBlockState(hitBlockPos);
        Direction hitDirection = blockHitResult.getDirection();
        BlockPos relativeBlockPos = hitBlockPos.relative(hitDirection);
        //Fluid interaction
        if (containsFluid) {
            FluidStack fluidStack = FluidUtil.getFluidContained(itemstack).orElse(FluidStack.EMPTY);
            if (hitBlockState.getBlock() instanceof AbstractCauldronBlock) {
                //fake vanilla bucket using on cauldron
                player.setItemSlot(equipmentSlot, new ItemStack(fluidStack.getFluid().getBucket()));
                InteractionResult interactionResult = hitBlockState.use(level, player, interactionHand, blockHitResult);
                player.setItemSlot(equipmentSlot, itemstack);
                if (interactionResult.consumesAction()) {
                    return new InteractionResultHolder<>(interactionResult, createEmptyResult(itemstack, player, removeFluid(itemstack)));
                }
            }
            FluidActionResult fluidActionResult = FluidUtil.tryPlaceFluid(player, level, interactionHand, relativeBlockPos, itemstack, fluidStack);
            if (fluidActionResult.isSuccess()) {
                return InteractionResultHolder.sidedSuccess(this.createEmptyResult(itemstack, player, fluidActionResult.getResult()), level.isClientSide());
            }
        } else {
            if (hitBlockState.getBlock() instanceof AbstractCauldronBlock) {
                //check if bucket can hold cauldron content
                if (hitBlockState.getBlock() == Blocks.LAVA_CAULDRON && canHoldFluid(Fluids.LAVA)
                        || hitBlockState.getBlock() == Blocks.WATER_CAULDRON && canHoldFluid(Fluids.WATER)) {
                    //fake vanilla bucket using on cauldron
                    player.setItemSlot(equipmentSlot, new ItemStack(Items.BUCKET));
                    InteractionResult interactionResult = hitBlockState.use(level, player, interactionHand, blockHitResult);
                    FluidStack resultFluidStack = FluidUtil.getFluidContained(player.getItemInHand(interactionHand)).orElse(FluidStack.EMPTY);
                    player.setItemSlot(equipmentSlot, itemstack);
                    if (interactionResult.consumesAction()) {
                        return new InteractionResultHolder<>(interactionResult, ItemUtils.createFilledResult(itemstack, player, addFluid(itemstack, resultFluidStack.getFluid())));
                    }
                }
            }
            FluidActionResult fluidActionResult = FluidUtil.tryPickUpFluid(itemstack, player, level, hitBlockPos, hitDirection);
            if (fluidActionResult.isSuccess()) {
                return InteractionResultHolder.sidedSuccess(ItemUtils.createFilledResult(itemstack, player, fluidActionResult.getResult()), level.isClientSide());
            }
        }
        //TODO Entity interaction
        //TODO Block interaction
        return super.use(level, player, interactionHand);
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
                    items.add(addFluid(emptyBucket, fluid));
                }
            }
            //TODO add milk bucket
            //TODO add entity buckets
            //TODO add block buckets
        }
    }

    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
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

    private ItemStack addFluid(ItemStack itemStack, Fluid fluid) {
        AtomicReference<ItemStack> resultItemStack = new AtomicReference<>(itemStack.copy());
        FluidUtil.getFluidHandler(resultItemStack.get()).ifPresent(fluidHandler -> {
            fluidHandler.fill(new FluidStack(fluid, FluidAttributes.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
            resultItemStack.set(fluidHandler.getContainer());
        });
        return resultItemStack.get();
    }

    private ItemStack removeFluid(ItemStack itemStack) {
        AtomicReference<ItemStack> resultItemStack = new AtomicReference<>(itemStack.copy());
        FluidUtil.getFluidHandler(resultItemStack.get()).ifPresent(fluidHandler -> {
            fluidHandler.drain(new FluidStack(fluidHandler.getFluidInTank(0).getFluid(), FluidAttributes.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
            resultItemStack.set(fluidHandler.getContainer());
        });
        return resultItemStack.get();
    }

    @Override
    public ItemStack getContainerItem(ItemStack itemStack) {
        //TODO remove other content (Entity, Blocks, ...)
        return removeFluid(itemStack);
    }

    @Override
    public ICapabilityProvider initCapabilities(@Nonnull ItemStack stack, @Nullable CompoundTag nbt) {
        return new UniversalBucketFluidHandler(stack);
    }

    private Integer getIntProperty(ForgeConfigSpec.IntValue config, Integer defaultValue) {
        if (config != null) {
            return config.get();
        }
        return defaultValue;
    }

    private boolean isFluidListedInProperty(Fluid fluid, Tag<Fluid> tag, List<Fluid> defaultList) {
        if (tag != null) {
            return tag.contains(fluid);
        }
        return defaultList != null && defaultList.contains(fluid);
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
        return isFluidListedInProperty(fluid, this.properties.crackingFluidsTag, this.properties.crackingFluids);
    }

    private boolean isBlockedFluid(Fluid fluid) {
        return isFluidListedInProperty(fluid, this.properties.blockedFluidsTag, this.properties.blockedFluids);
    }

    private boolean isAllowedFluid(Fluid fluid) {
        return isFluidListedInProperty(fluid, this.properties.allowedFluidsTag, this.properties.allowedFluids);
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

    }

}
