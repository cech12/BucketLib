package cech12.bucketlib.item;

/*
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class UniversalBucketFluidHandler extends FluidHandlerItemStack {

    private final LazyOptional<IFluidHandlerItem> holder = LazyOptional.of(() -> this);

    public UniversalBucketFluidHandler(@Nonnull ItemStack container) {
        super(container, FluidAttributes.BUCKET_VOLUME);
    }

    @Override
    protected void setContainerToEmpty() {
        this.container = this.container.getContainerItem();
    }

    @Override
    protected void setFluid(FluidStack fluid) {
        this.container
        //TODO
    }

    @Override
    public int fill(FluidStack resource, IFluidHandler.FluidAction doFill) {
        //only fill the bucket, if there is enough fluid to fill the bucket completely
        if (resource.getAmount() < FluidAttributes.BUCKET_VOLUME) {
            return 0;
        }
        return super.fill(resource, doFill);
    }

    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        //only drain the bucket, if there is enough space to drain the bucket completely
        if (maxDrain < FluidAttributes.BUCKET_VOLUME) {
            return FluidStack.EMPTY;
        }
        //consider infinity enchantment
        if (action == FluidAction.EXECUTE
                && CeramicBucketUtils.isAffectedByInfinityEnchantment(this.container)) {
            //simulate drain to simulate infinity effect
            return super.drain(maxDrain, FluidAction.SIMULATE);
        }
        return super.drain(maxDrain, action);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY) {
            return (LazyOptional<T>) holder;
        }
        return LazyOptional.empty();
    }

}
*/