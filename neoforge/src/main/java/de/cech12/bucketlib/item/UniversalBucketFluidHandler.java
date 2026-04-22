package de.cech12.bucketlib.item;

import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.ItemAccessFluidHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;

public class UniversalBucketFluidHandler extends ItemAccessFluidHandler {

    public UniversalBucketFluidHandler(ItemAccess itemAccess, DataComponentType<SimpleFluidContent> component) {
        super(itemAccess, component, FluidType.BUCKET_VOLUME);
    }

    @Override
    @NotNull
    protected FluidResource getResourceFrom(@NotNull ItemResource accessResource, int index) {
        FluidResource fluidResource = super.getResourceFrom(accessResource, index);
        //fill milk bucket with milk fluid if it is enabled
        if (accessResource.is(validItem) && fluidResource.isEmpty() && NeoForgeMod.MILK.isBound() && BucketLibUtil.containsMilk(accessResource)) {
            fluidResource = FluidResource.of(NeoForgeMod.MILK.get());
        }
        return fluidResource;
    }

    @Override
    protected int getAmountFrom(@NotNull ItemResource accessResource, int index) {
        int amount = super.getAmountFrom(accessResource, index);
        //fill milk bucket with milk fluid if it is enabled
        if (accessResource.is(validItem) && amount < FluidType.BUCKET_VOLUME && NeoForgeMod.MILK.isBound() && BucketLibUtil.containsMilk(accessResource)) {
            amount = FluidType.BUCKET_VOLUME;
        }
        return amount;
    }

    @Override
    public boolean isValid(int index, @NotNull FluidResource resource) {
        //only if the container can hold the fluid
        if (validItem instanceof UniversalBucketItem bucketItem && resource.getFluid() != Fluids.EMPTY && !bucketItem.canHoldFluid(resource.getFluid())) {
            return false;
        }
        //only fill the bucket, if there is no entity in the bucket
        if (BucketLibUtil.containsEntityType(itemAccess.getResource())) {
            return false;
        }
        //only fill the bucket, if there is no milk inside it.
        if (BucketLibUtil.containsMilk(itemAccess.getResource())) {
            return false;
        }
        return super.isValid(index, resource);
    }

    @Override
    public int insert(int index, @NotNull FluidResource resource, int amount, @NotNull TransactionContext transaction) {
        //only fill the bucket, if there is enough fluid to fill the bucket completely
        if (amount < capacity) {
            return 0;
        }
        return super.insert(index, resource, amount, transaction);
    }

    @Override
    public int extract(int index, @NotNull FluidResource resource, int amount, @NotNull TransactionContext transaction) {
        //only drain the bucket, if there is no entity in the bucket
        if (BucketLibUtil.containsEntityType(itemAccess.getResource())) {
            return 0;
        }
        //only drain milk if the fluid is active
        if (!NeoForgeMod.MILK.isBound() && BucketLibUtil.containsMilk(itemAccess.getResource())) {
            return 0;
        }
        //only drain the bucket, if there is enough space to drain the bucket completely
        if (amount < capacity) {
            return 0;
        }
        if (BucketLibUtil.isAffectedByInfinityEnchantment(itemAccess.getResource().toStack())) {
            //don't change the values to simulate infinity effect
            return amount;
        }
        ItemStack previousStackCopy = itemAccess.getResource().toStack().copy();
        int extracted = super.extract(index, resource, amount, transaction);
        if (extracted > 0) {
            damageByOne(previousStackCopy, transaction);
        }
        return extracted;
    }

    private void damageByOne(ItemStack previousStackCopy, @NotNull TransactionContext transaction) {
        boolean wasCracked = false;
        if (validItem instanceof UniversalBucketItem bucketItem) {
            wasCracked = bucketItem.isCracked(previousStackCopy);
        }
        ItemStack emptiedStackCopy = itemAccess.getResource().toStack().copy();
        if (!wasCracked) {
            if (BucketLibUtil.containsContent(previousStackCopy)) { //remove milk content tag
                BucketLibUtil.removeContentNoCopy(emptiedStackCopy, null, null, false);
            }
            emptiedStackCopy.remove(BucketLibMod.FLUID_COMPONENT);
            BucketLibUtil.damageByOne(emptiedStackCopy, null); //server level not available here
        }
        //persist changes
        try (Transaction innerTransaction = Transaction.open(transaction)) {
            if (!wasCracked && !emptiedStackCopy.isEmpty()) {
                itemAccess.exchange(ItemAccess.forStack(emptiedStackCopy).getResource(), 1, innerTransaction);
            } else {
                //shrink by 1
                itemAccess.extract(itemAccess.getResource(), 1, innerTransaction);
            }
            innerTransaction.commit();
        }
    }

}