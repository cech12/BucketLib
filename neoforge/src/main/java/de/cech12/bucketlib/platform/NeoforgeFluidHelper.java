package de.cech12.bucketlib.platform;

import de.cech12.bucketlib.platform.services.IFluidHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.DispenseFluidContainer;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

/**
 * The fluid service implementation for NeoForge.
 */
public class NeoforgeFluidHelper implements IFluidHelper {

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
    public int getFluidTintColor(ItemStack stack) {
        var fluid = FluidUtil.getFirstStackContained(stack);
        FluidTintSource tintSource = Minecraft.getInstance()
                .getModelManager()
                .getFluidStateModelSet()
                .get(fluid.getFluid().defaultFluidState())
                .fluidTintSource();
        return tintSource != null ? tintSource.colorAsStack(fluid) : -1;
    }

    @Override
    public int getFluidLightLevel(Fluid fluid) {
        return fluid.getFluidType().getLightLevel();
    }

    @Override
    public boolean isFluidLighterThanAir(Fluid fluid) {
        return fluid.getFluidType().isLighterThanAir();
    }

    @Override
    public boolean hasMilkFluid() {
        return NeoForgeMod.MILK.isBound();
    }

    @Override
    public Fluid getMilkFluid() {
        return NeoForgeMod.MILK.get();
    }

    @Override
    public ItemStack dispenseFluidContainer(BlockSource source, ItemStack stack) {
        return dispenseFluidBehavior.execute(source, stack);
    }

    @Override
    public Fluid getContainedFluid(ItemStack stack) {
        ResourceHandler<FluidResource> handler = ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM);
        if (handler != null) {
            return handler.getResource(0).getFluid();
        }
        return Fluids.EMPTY;
    }

    @Override
    public ItemStack addFluid(ItemStack stack, Fluid fluid) {
        ItemAccess access = ItemAccess.forStack(stack.copy());
        ResourceHandler<FluidResource> handler = access.getCapability(Capabilities.Fluid.ITEM);
        if (handler != null) {
            try (Transaction transaction = Transaction.open(null)) {
                FluidResource resource = FluidResource.of(fluid);
                int insertedAmount = handler.insert(resource, FluidType.BUCKET_VOLUME, transaction);
                if (insertedAmount == FluidType.BUCKET_VOLUME) {
                    transaction.commit();
                    stack = access.getResource().toStack();
                }
            }
        }
        return stack;
    }

    @Override
    public ItemStack removeFluid(ItemStack stack, ServerLevel level, @Nullable Player player) {
        ItemAccess access = ItemAccess.forStack(stack.copy());
        ResourceHandler<FluidResource> handler = access.getCapability(Capabilities.Fluid.ITEM);
        if (handler != null) {
            FluidResource resource = handler.getResource(0);
            if (!resource.isEmpty()) {
                try (Transaction transaction = Transaction.open(null)) {
                    int extractedAmount = handler.extract(resource, FluidType.BUCKET_VOLUME, transaction);
                    if (extractedAmount == FluidType.BUCKET_VOLUME) {
                        //damaging is done by fluid handler
                        transaction.commit();
                        stack = access.getResource().toStack();
                    }
                }
            }
        }
        return stack;
    }

    @Override
    public FluidInteractionResult tryPickUpFluid(ItemStack stack, Player player, Level level, InteractionHand interactionHand, BlockPos pos, Direction direction) {
        ItemAccess access = ItemAccess.forStack(stack.copyWithCount(1));
        ResourceHandler<FluidResource> handler = access.getCapability(Capabilities.Fluid.ITEM);
        FluidStack pickedFluidStack = FluidUtil.tryPickupFluid(handler, player, level, pos, direction, null);
        return new FluidInteractionResult(!pickedFluidStack.isEmpty(), access.getResource().toStack());
    }

    @Override
    public FluidInteractionResult tryPlaceFluid(ItemStack stack, Player player, Level level, InteractionHand interactionHand, BlockPos pos) {
        ItemAccess access = ItemAccess.forStack(stack.copy());
        ResourceHandler<FluidResource> handler = access.getCapability(Capabilities.Fluid.ITEM);
        FluidStack placedFluidStack = FluidUtil.tryPlaceFluid(handler, player, level, pos, false, null);
        return new FluidInteractionResult(!placedFluidStack.isEmpty(), access.getResource().toStack());
    }

    @Override
    public void curePotionEffects(LivingEntity entity, ItemStack curativeItem) {
        entity.removeAllEffects(); //wait for https://github.com/neoforged/NeoForge/pull/1603
    }

}
