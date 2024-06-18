package de.cech12.bucketlib.item;

import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class UniversalBucketFluidStorage extends SingleFluidStorage {

    private final ContainerItemContext context;

    public UniversalBucketFluidStorage(ContainerItemContext context) {
        this.context = context;
        Optional<? extends FluidStorageData> optional = context.getItemVariant().getComponents().get(BucketLibMod.STORAGE);
        if (optional != null) {
            optional.ifPresent(data -> {
                if (!data.isEmpty()) {
                    this.variant = data.fluidVariant();
                    this.amount = data.amount();
                }
            });
        }
    }

    @Override
    protected long getCapacity(FluidVariant variant) {
        return FluidConstants.BUCKET;
    }

    @Override
    protected boolean canInsert(FluidVariant variant) {
        ItemStack stack = context.getItemVariant().toStack();
        return this.variant.isBlank() && !BucketLibUtil.containsMilk(stack) && !BucketLibUtil.containsEntityType(stack) && !BucketLibUtil.containsBlock(stack)
                && (context.getItemVariant().getItem() instanceof UniversalBucketItem universalBucketItem
                && universalBucketItem.canHoldFluid(variant.getFluid()));
    }

    @Override
    protected boolean canExtract(FluidVariant variant) {
        ItemStack stack = context.getItemVariant().toStack();
        return this.variant.getFluid() == variant.getFluid() && !BucketLibUtil.containsEntityType(stack) && !BucketLibUtil.containsMilk(stack);
    }

    @Override
    public long insert(FluidVariant insertedVariant, long maxAmount, TransactionContext transaction) {
        if (maxAmount < getCapacity()) {
            return 0;
        }
        return super.insert(insertedVariant, maxAmount, transaction);
    }

    @Override
    public long extract(FluidVariant extractedVariant, long maxAmount, TransactionContext transaction) {
        if (maxAmount < getCapacity()) {
            return 0;
        }
        ItemStack stackBefore = context.getItemVariant().toStack();
        long result = super.extract(extractedVariant, maxAmount, transaction);
        updateSnapshots(transaction);
        ItemStack stack = context.getItemVariant().toStack();
        if (result > 0) {
            boolean wasCracked = false;
            if (stackBefore.getItem() instanceof UniversalBucketItem bucketItem) {
                wasCracked = bucketItem.isCracked(stackBefore);
            }
            if (wasCracked) {
                stack.shrink(1);
            } else {
                if (BucketLibUtil.containsContent(stack)) { //remove milk content tag
                    BucketLibUtil.removeContentNoCopy(stack, false);
                }
                BucketLibUtil.damageByOne(stack);
            }
            if (!exchangeOrRemove(ItemVariant.of(stack), transaction)) {
                return 0;
            }
        }
        return result;
    }

    private boolean exchangeOrRemove(ItemVariant newVariant, TransactionContext transaction) {
        if (!newVariant.isBlank()) {
            return context.exchange(newVariant, 1, transaction) == 1;
        } else {
            return context.extract(context.getItemVariant(), 1, transaction) == 1;
        }
    }

}
