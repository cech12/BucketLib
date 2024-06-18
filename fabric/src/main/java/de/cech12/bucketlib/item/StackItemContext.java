package de.cech12.bucketlib.item;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.List;

public class StackItemContext implements ContainerItemContext {


    private final SingleVariantStorage<ItemVariant> stackSlot = new SingleVariantStorage<>() {

        @Override
        protected ItemVariant getBlankVariant() {
            return ItemVariant.blank();
        }

        @Override
        protected long getCapacity(ItemVariant variant) {
            return variant.getItem().getDefaultMaxStackSize();
        }

        @Override
        public long insert(ItemVariant insertedVariant, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(insertedVariant, maxAmount);
            if (!isResourceBlank()) {
                return 0;
            }
            variant = insertedVariant;
            amount = maxAmount;
            return maxAmount;
        }

        @Override
        public long extract(ItemVariant extractedVariant, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(extractedVariant, maxAmount);
            if (isResourceBlank() || extractedVariant.getItem() != variant.getItem()) {
                return 0;
            }
            long amountDiff = amount - maxAmount;
            if (amountDiff > 0) {
                amount = amountDiff;
                return maxAmount;
            } else {
                variant = getBlankVariant();
                amount = 0;
                return maxAmount + amountDiff;
            }
        }
    };

    public StackItemContext(ItemStack stack) {
        stackSlot.variant = ItemVariant.of(stack);
        stackSlot.amount = stack.getCount();
    }

    @Override
    public SingleSlotStorage<ItemVariant> getMainSlot() {
        return stackSlot;
    }

    @Override
    public long insertOverflow(ItemVariant itemVariant, long maxAmount, TransactionContext transactionContext) {
        return 0;
    }

    @Override
    public @UnmodifiableView List<SingleSlotStorage<ItemVariant>> getAdditionalSlots() {
        return List.of();
    }
}
