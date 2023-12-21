package de.cech12.bucketlib.platform;

import de.cech12.bucketlib.platform.services.IFluidHelper;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.concurrent.atomic.AtomicReference;

/**
 * The fluid service implementation for Forge.
 */
public class ForgeFluidHelper implements IFluidHelper {

    private static final DispenseFluidContainer dispenseFluidBehavior = DispenseFluidContainer.getInstance();

    @Override
    public Component getFluidDescription(Fluid fluid) {
        return fluid.getFluidType().getDescription();
    }

    @Override
    public int getFluidTemperature(Fluid fluid) {
        return fluid.getFluidType().getTemperature();
    }

    @Override
    public boolean hasMilkFluid() {
        return ForgeMod.MILK.isPresent();
    }

    @Override
    public Fluid getMilkFluid() {
        return ForgeMod.MILK.get();
    }

    @Override
    public ItemStack dispenseFluidContainer(BlockSource source, ItemStack stack) {
        return dispenseFluidBehavior.execute(source, stack);
    }

    @Override
    public Fluid getContainedFluid(ItemStack stack) {
        return FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY).getFluid();
    }

    @Override
    public ItemStack addFluid(ItemStack stack, Fluid fluid) {
        AtomicReference<ItemStack> resultItemStack = new AtomicReference<>(stack.copy());
        FluidUtil.getFluidHandler(resultItemStack.get()).ifPresent(fluidHandler -> {
            fluidHandler.fill(new FluidStack(fluid, FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
            resultItemStack.set(fluidHandler.getContainer());
        });
        return resultItemStack.get();
    }

    @Override
    public ItemStack removeFluid(ItemStack stack) {
        AtomicReference<ItemStack> resultItemStack = new AtomicReference<>(stack.copy());
        FluidUtil.getFluidHandler(resultItemStack.get()).ifPresent(fluidHandler -> {
            fluidHandler.drain(new FluidStack(fluidHandler.getFluidInTank(0).getFluid(), FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
            //damaging is done by fluid handler
            resultItemStack.set(fluidHandler.getContainer());
        });
        return resultItemStack.get();
    }

    @Override
    public Tuple<Boolean, ItemStack> tryPickUpFluid(ItemStack stack, Player player, Level level, InteractionHand interactionHand, BlockPos pos, Direction direction) {
        FluidActionResult fluidActionResult = FluidUtil.tryPickUpFluid(stack, player, level, pos, direction);
        return new Tuple<>(fluidActionResult.isSuccess(), fluidActionResult.getResult());
    }

    @Override
    public Tuple<Boolean, ItemStack> tryPlaceFluid(ItemStack stack, Player player, Level level, InteractionHand interactionHand, BlockPos pos) {
        FluidStack fluidStack = FluidUtil.getFluidHandler(stack).map(fluidHandler -> fluidHandler.getFluidInTank(0)).orElse(FluidStack.EMPTY);
        FluidActionResult fluidActionResult = FluidUtil.tryPlaceFluid(player, level, interactionHand, pos, stack, fluidStack);
        return new Tuple<>(fluidActionResult.isSuccess(), fluidActionResult.getResult());
    }


}
