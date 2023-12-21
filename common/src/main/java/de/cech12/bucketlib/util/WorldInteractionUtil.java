package de.cech12.bucketlib.util;

import de.cech12.bucketlib.api.BucketLibTags;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

public class WorldInteractionUtil {

    private WorldInteractionUtil() {}

    public static InteractionResult tryMilkLivingEntity(ItemStack itemStack, LivingEntity entity, Player player, InteractionHand interactionHand) {
        if (!entity.getType().is(BucketLibTags.EntityTypes.MILKABLE)) {
            return InteractionResult.PASS;
        }
        player.setItemInHand(interactionHand, new ItemStack(Items.BUCKET));
        //deactivate instabuild for the fake interaction to avoid side effects like adding additional filled vanilla buckets into inventory
        boolean previousInstabuildValue = player.getAbilities().instabuild;
        player.getAbilities().instabuild = false;
        InteractionResult result = player.interactOn(entity, interactionHand);
        player.getAbilities().instabuild = previousInstabuildValue;
        if (result.consumesAction()) {
            itemStack = ItemUtils.createFilledResult(itemStack.copy(), player, BucketLibUtil.addMilk(ItemStackUtil.copyStackWithSize(itemStack, 1)));
        }
        player.setItemInHand(interactionHand, itemStack);
        return result;
    }

    public static InteractionResultHolder<ItemStack> tryPickupFromCauldron(Level level, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        ItemStack itemstack = player.getItemInHand(interactionHand);
        BlockPos hitBlockPos = blockHitResult.getBlockPos();
        BlockState hitBlockState = level.getBlockState(hitBlockPos);
        Block hitBlock = hitBlockState.getBlock();
        if (hitBlock instanceof AbstractCauldronBlock cauldronBlock
                && itemstack.getItem() instanceof UniversalBucketItem bucketItem
                && BucketLibUtil.isEmpty(itemstack)) {
            //check if bucket can hold cauldron content
            if (cauldronBlock == Blocks.LAVA_CAULDRON && bucketItem.canHoldFluid(Fluids.LAVA)
                    || cauldronBlock == Blocks.WATER_CAULDRON && bucketItem.canHoldFluid(Fluids.WATER)
                    || cauldronBlock == Blocks.POWDER_SNOW_CAULDRON && bucketItem.canHoldBlock(Blocks.POWDER_SNOW)) {
                //fake vanilla bucket using on cauldron
                player.setItemInHand(interactionHand, new ItemStack(Items.BUCKET));
                //deactivate instabuild for the fake interaction to avoid side effects like adding additional filled vanilla buckets into inventory
                boolean previousInstabuildValue = player.getAbilities().instabuild;
                player.getAbilities().instabuild = false;
                InteractionResult interactionResult = hitBlockState.use(level, player, interactionHand, blockHitResult);
                player.getAbilities().instabuild = previousInstabuildValue;
                ItemStack resultItemStack = player.getItemInHand(interactionHand);
                player.setItemInHand(interactionHand, itemstack);
                if (interactionResult.consumesAction()) {
                    if (resultItemStack.getItem() == Items.POWDER_SNOW_BUCKET) {
                        return new InteractionResultHolder<>(interactionResult, ItemUtils.createFilledResult(itemstack, player, BucketLibUtil.addBlock(ItemStackUtil.copyStackWithSize(itemstack, 1), Blocks.POWDER_SNOW)));
                    } else {
                        return new InteractionResultHolder<>(interactionResult, ItemUtils.createFilledResult(itemstack, player, BucketLibUtil.addFluid(ItemStackUtil.copyStackWithSize(itemstack, 1), Services.FLUID.getContainedFluid(resultItemStack))));
                    }
                }
            }
        }
        return InteractionResultHolder.pass(itemstack);
    }

    public static InteractionResultHolder<ItemStack> tryPlaceIntoCauldron(Level level, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        ItemStack itemstack = player.getItemInHand(interactionHand);
        BlockPos hitBlockPos = blockHitResult.getBlockPos();
        BlockState hitBlockState = level.getBlockState(hitBlockPos);
        Block hitBlock = hitBlockState.getBlock();
        if (hitBlock instanceof AbstractCauldronBlock cauldronBlock
                && itemstack.getItem() instanceof UniversalBucketItem
                && cauldronBlock == Blocks.CAULDRON
                && !BucketLibUtil.containsEntityType(itemstack)) {
            //fake vanilla bucket using on cauldron
            Fluid bucketFluid = BucketLibUtil.getFluid(itemstack);
            Block bucketBlock = BucketLibUtil.getBlock(itemstack);
            if (bucketFluid == Fluids.LAVA || bucketFluid == Fluids.WATER) {
                player.setItemInHand(interactionHand, new ItemStack(bucketFluid.getBucket()));
                //deactivate instabuild for the fake interaction to avoid side effects like adding additional empty vanilla buckets into inventory
                boolean previousInstabuildValue = player.getAbilities().instabuild;
                player.getAbilities().instabuild = false;
                InteractionResult interactionResult = hitBlockState.use(level, player, interactionHand, blockHitResult);
                player.getAbilities().instabuild = previousInstabuildValue;
                player.setItemInHand(interactionHand, itemstack);
                if (interactionResult.consumesAction()) {
                    return new InteractionResultHolder<>(interactionResult, BucketLibUtil.createEmptyResult(itemstack, player, BucketLibUtil.removeFluid(itemstack), interactionHand, true));
                }
            } else if (bucketBlock == Blocks.POWDER_SNOW) {
                player.setItemInHand(interactionHand, new ItemStack(Items.POWDER_SNOW_BUCKET));
                //deactivate instabuild for the fake interaction to avoid side effects like adding additional empty vanilla buckets into inventory
                boolean previousInstabuildValue = player.getAbilities().instabuild;
                player.getAbilities().instabuild = false;
                InteractionResult interactionResult = hitBlockState.use(level, player, interactionHand, blockHitResult);
                player.getAbilities().instabuild = previousInstabuildValue;
                player.setItemInHand(interactionHand, itemstack);
                if (interactionResult.consumesAction()) {
                    return new InteractionResultHolder<>(interactionResult, BucketLibUtil.createEmptyResult(itemstack, player, BucketLibUtil.removeBlock(itemstack), interactionHand, true));
                }
            }
        }
        return InteractionResultHolder.pass(itemstack);
    }

}
