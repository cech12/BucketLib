package cech12.bucketlib.item;

import cech12.bucketlib.api.item.UniversalBucketItem;
import cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;

import javax.annotation.Nonnull;

public class UniversalBucketFluidHandler extends FluidHandlerItemStack {

    public UniversalBucketFluidHandler(@Nonnull ItemStack container) {
        super(container, FluidAttributes.BUCKET_VOLUME);
    }

    @Override
    public int fill(FluidStack resource, IFluidHandler.FluidAction doFill) {
        //only fill the bucket, if there is enough fluid to fill the bucket completely
        if (resource.getAmount() < capacity) {
            return 0;
        }
        return super.fill(resource, doFill);
    }

    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        //only drain the bucket, if there is enough space to drain the bucket completely
        if (maxDrain < capacity) {
            return FluidStack.EMPTY;
        }
        if (action == FluidAction.EXECUTE
                && BucketLibUtil.isAffectedByInfinityEnchantment(this.container)) {
            //simulate drain to simulate infinity effect
            return super.drain(maxDrain, FluidAction.SIMULATE);
        }
        return super.drain(maxDrain, action);
    }

    @Override
    protected void setContainerToEmpty() {
        boolean wasCracked = false;
        Item item = container.getItem();
        if (item instanceof UniversalBucketItem bucketItem) {
            wasCracked = bucketItem.isCracked(container);
        }
        super.setContainerToEmpty();
        if (wasCracked) {
            container.shrink(1);
        }
    }

    @Override
    public boolean canFillFluidType(FluidStack fluid) {
        Item item = container.getItem();
        if (item instanceof UniversalBucketItem bucketItem) {
            return bucketItem.canHoldFluid(fluid.getFluid());
        }
        return super.canFillFluidType(fluid);
    }

}