package de.cech12.bucketlib.item;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.platform.Services;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.RegistryUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

public class UniversalBucketDispenseBehaviour extends DefaultDispenseItemBehavior {

    private static final UniversalBucketDispenseBehaviour INSTANCE = new UniversalBucketDispenseBehaviour();

    public static UniversalBucketDispenseBehaviour getInstance()
    {
        return INSTANCE;
    }

    private UniversalBucketDispenseBehaviour() {}

    private final DefaultDispenseItemBehavior dispenseBehavior = new DefaultDispenseItemBehavior();

    @Override
    @Nonnull
    public ItemStack execute(@Nonnull BlockSource source, @Nonnull ItemStack stack) {
        if (stack.getItem() instanceof UniversalBucketItem) {
            if (BucketLibUtil.isEmpty(stack)) {
                return fillBucket(source, stack);
            } else {
                return emptyBucket(source, stack);
            }
        }
        return Services.FLUID.dispenseFluidContainer(source, stack);
    }

    private ItemStack fillBucket(@Nonnull BlockSource source, @Nonnull ItemStack stack) {
        ServerLevel level = source.level();
        BlockPos pickupPosition = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
        BlockState blockState = level.getBlockState(pickupPosition);
        RegistryUtil.BucketBlock bucketBlock = RegistryUtil.getBucketBlock(blockState.getBlock());
        if (bucketBlock != null) {
            //pickup block
            if (stack.getItem() instanceof UniversalBucketItem universalBucketItem
                    && universalBucketItem.canHoldBlock(bucketBlock.block())
                    && bucketBlock.block() instanceof BucketPickup bucketPickup
            ) {
                ItemStack vanillaStack = bucketPickup.pickupBlock(null, level, pickupPosition, blockState);
                if (!vanillaStack.isEmpty()) {
                    if (stack.getCount() == 1) {
                        return BucketLibUtil.addBlock(stack, bucketBlock.block());
                    }
                    ItemStack usedStack = stack.copy();
                    usedStack.setCount(1);
                    ItemStack resultStack = BucketLibUtil.addBlock(usedStack, bucketBlock.block());
                    if (stack.getCount() == 1) {
                        return resultStack;
                    }
                    if ((source.blockEntity()).addItem(resultStack) < 0) {
                        this.dispenseBehavior.dispense(source, resultStack);
                    }
                    ItemStack stackCopy = stack.copy();
                    stackCopy.shrink(1);
                    return stackCopy;
                }

            }
        }
        return Services.FLUID.dispenseFluidContainer(source, stack);
    }

    private ItemStack emptyBucket(@Nonnull BlockSource source, @Nonnull ItemStack stack) {
        ServerLevel level = source.level();
        BlockPos placePosition = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
        if (BucketLibUtil.containsBlock(stack)) {
            //place block
            Block placeBlock = BucketLibUtil.getBlock(stack);
            if (placeBlock != null && placeBlock.asItem() instanceof DispensibleContainerItem dispensibleContainerItem) {
                if (dispensibleContainerItem.emptyContents(null, level, placePosition, null)) {
                    return BucketLibUtil.removeBlock(stack);
                }
            }
        } else if (BucketLibUtil.containsEntityType(stack)) {
            //place entity
            if (stack.getItem() instanceof UniversalBucketItem bucketItem) {
                if (BucketLibUtil.containsFluid(stack)) {
                    //fluid can only be placed correctly if the entity is not inside
                    ItemStack stackWithoutEntity = BucketLibUtil.removeEntityType(stack.copy(), false);
                    ItemStack fluidResult = Services.FLUID.dispenseFluidContainer(source, stackWithoutEntity);
                    bucketItem.spawnEntityFromBucket(null, source.level(), stack, placePosition, false);
                    return fluidResult;
                } else {
                    return bucketItem.spawnEntityFromBucket(null, source.level(), stack, placePosition, true);
                }
            }
        }
        return Services.FLUID.dispenseFluidContainer(source, stack);
    }

}
