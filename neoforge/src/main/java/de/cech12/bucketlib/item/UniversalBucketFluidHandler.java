package de.cech12.bucketlib.item;

import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;

import javax.annotation.Nonnull;

public class UniversalBucketFluidHandler extends FluidHandlerItemStack {

    public UniversalBucketFluidHandler(@Nonnull ItemStack container) {
        super(BucketLibMod.FLUID_COMPONENT, container, FluidType.BUCKET_VOLUME);
    }

    @Nonnull
    @Override
    public FluidStack getFluid() {
        FluidStack fluidStack = super.getFluid();
        //fill milk bucket with milk fluid if it is enabled
        if (fluidStack.isEmpty() && NeoForgeMod.MILK.isBound()) {
            ResourceLocation bucketContent = BucketLibUtil.getContent(this.container);
            if (bucketContent != null && bucketContent.equals(BucketLibUtil.MILK_LOCATION)) {
                fluidStack = new FluidStack(NeoForgeMod.MILK.get(), FluidType.BUCKET_VOLUME);
                setFluid(fluidStack);
            }
        }
        return fluidStack;
    }

    @Override
    public int fill(@Nonnull FluidStack resource, @Nonnull IFluidHandler.FluidAction doFill) {
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
    public FluidStack drain(int maxDrain, @Nonnull FluidAction action) {
        //only drain the bucket, if there is no entity in the bucket
        if (BucketLibUtil.containsEntityType(getContainer())) {
            return FluidStack.EMPTY;
        }
        //only drain milk if the fluid is active
        if (!NeoForgeMod.MILK.isBound() && BucketLibUtil.containsMilk(getContainer())) {
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
            if (BucketLibUtil.containsContent(container)) { //remove milk content tag
                BucketLibUtil.removeContentNoCopy(container, null, null, false);
            }
            BucketLibUtil.damageByOne(container, null); //TODO get ServerLevel!
        }
    }

    @Override
    public boolean canFillFluidType(@Nonnull FluidStack fluid) {
        Item item = container.getItem();
        if (item instanceof UniversalBucketItem bucketItem) {
            return fluid.getFluid() != Fluids.EMPTY && bucketItem.canHoldFluid(fluid.getFluid());
        }
        return super.canFillFluidType(fluid);
    }

}