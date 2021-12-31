package cech12.bucketlib.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.ItemFluidContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class UniversalBucketItem extends ItemFluidContainer implements DispensibleContainerItem {

    //TODO Fluid
    //TODO Entity
    //TODO Blocks

    public UniversalBucketItem() {
        super((new Item.Properties()).tab(CreativeModeTab.TAB_MISC), FluidAttributes.BUCKET_VOLUME);
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
        //TODO
        return false;
    }

    private boolean containsFluid(ItemStack stack) {
        AtomicBoolean containsFluid = new AtomicBoolean(false);
        FluidUtil.getFluidContained(stack).ifPresent(fluidStack -> containsFluid.set(!fluidStack.isEmpty()));
        return containsFluid.get();
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return containsFluid(stack) ? 1 : 16;
    }

    @Override
    @Nonnull
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand interactionHand) {
        ItemStack itemstack = player.getItemInHand(interactionHand);
        boolean containsFluid = containsFluid(itemstack);
        //check hit block
        BlockHitResult blockhitresult = getPlayerPOVHitResult(level, player, containsFluid ? ClipContext.Fluid.NONE : ClipContext.Fluid.SOURCE_ONLY);
        if (blockhitresult.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(itemstack);
        } else if (blockhitresult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(itemstack);
        }
        BlockPos hitBlockPos = blockhitresult.getBlockPos();
        Direction hitDirection = blockhitresult.getDirection();
        BlockPos relativeBlockPos = hitBlockPos.relative(hitDirection);
        //Fluid interaction
        FluidActionResult result;
        if (containsFluid) {
            FluidStack fluidStack = FluidUtil.getFluidContained(itemstack).orElse(FluidStack.EMPTY);
            result = FluidUtil.tryPlaceFluid(player, level, interactionHand, relativeBlockPos, itemstack, fluidStack);
        } else {
            result = FluidUtil.tryPickUpFluid(itemstack, player, level, hitBlockPos, hitDirection);
        }
        if (result.isSuccess()) {
            return InteractionResultHolder.success(result.getResult());
        }
        //TODO Entity interaction
        //TODO Block interaction
        //TODO creative mode should not change the bucket
        //TODO play sound
        return super.use(level, player, interactionHand);
    }

    @Override
    public void checkExtraContent(@Nullable Player player, @Nonnull Level level, @Nonnull ItemStack stack, @Nonnull BlockPos pos) {
        //TODO dispenser behavior
        DispensibleContainerItem.super.checkExtraContent(player, level, stack, pos);
    }

    @Override
    public boolean emptyContents(@Nullable Player player, @Nonnull Level level, @Nonnull BlockPos pos, @Nullable BlockHitResult hitResult) {
        //TODO dispenser behavior
        return false;
    }

    /*
    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        //TODO init capabilities
        return super.initCapabilities(stack, nbt);
    }

     */
}
