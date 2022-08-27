package cech12.bucketlib.item;

import cech12.bucketlib.api.item.UniversalBucketItem;
import cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;

import javax.annotation.Nonnull;

public class UniversalBucketFluidHandler extends FluidHandlerItemStack {

    public UniversalBucketFluidHandler(@Nonnull ItemStack container) {
        super(container, FluidType.BUCKET_VOLUME);
    }

    @Override
    public int fill(FluidStack resource, IFluidHandler.FluidAction doFill) {
        //only fill the bucket, if there is no milk inside it.
        if (BucketLibUtil.containsMilk(getContainer())) {
            return 0;
        }
        //only fill the bucket, if there is enough fluid to fill the bucket completely
        if (resource.getAmount() < capacity) {
            return 0;
        }
        return super.fill(resource, doFill);
    }

    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        //only drain the bucket, if there is no entity in the bucket
        if (BucketLibUtil.containsEntityType(getContainer())) {
            return FluidStack.EMPTY;
        }
        //only drain milk if the fluid is active
        if (!ForgeMod.MILK.isPresent() && BucketLibUtil.containsMilk(getContainer())) {
            return FluidStack.EMPTY;
        }
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
        } else {
            BucketLibUtil.damageByOne(container);
        }
    }

    @Override
    public boolean canFillFluidType(FluidStack fluid) {
        Item item = container.getItem();
        if (item instanceof UniversalBucketItem bucketItem) {
            return fluid.getFluid() != Fluids.EMPTY && bucketItem.canHoldFluid(fluid.getFluid());
        }
        return super.canFillFluidType(fluid);
    }

}