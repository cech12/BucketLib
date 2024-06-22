package de.cech12.bucketlib.platform;

import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.item.FluidStorageData;
import de.cech12.bucketlib.item.StackItemContext;
import de.cech12.bucketlib.item.UniversalBucketFluidStorage;
import de.cech12.bucketlib.platform.services.IFluidHelper;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.RegistryUtil;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * The fluid service implementation for NeoForge.
 */
public class FabricFluidHelper implements IFluidHelper {

    @Override
    public Component getFluidDescription(Fluid fluid) {
        return FluidVariantAttributes.getName(FluidVariant.of(fluid));
    }

    @Override
    public int getFluidTemperature(Fluid fluid) {
        return FluidVariantAttributes.getTemperature(FluidVariant.of(fluid));
    }

    @Override
    public boolean hasMilkFluid() {
        return false;
    }

    @Override
    public Fluid getMilkFluid() {
        return Fluids.EMPTY;
    }

    @Override
    public ItemStack dispenseFluidContainer(BlockSource source, ItemStack stack) {
        Level level = source.level();
        Direction dispenserFacing = source.state().getValue(DispenserBlock.FACING);
        BlockPos pos = source.pos().relative(dispenserFacing);
        if (BucketLibUtil.isEmpty(stack)) {
            Tuple<Boolean, ItemStack> result = tryPickUpFluid(stack, null, level, null, pos, dispenserFacing);
            if (result.getA()) {
                if (stack.getCount() == 1) {
                    return result.getB();
                }
                if (!(source.blockEntity()).insertItem(result.getB()).isEmpty()) {
                    new DefaultDispenseItemBehavior().dispense(source, result.getB());
                }
                ItemStack stackCopy = stack.copy();
                stackCopy.shrink(1);
                return stackCopy;
            }
        } else {
            Tuple<Boolean, ItemStack> result = tryPlaceFluid(stack, null, level, null, pos);
            if (result.getA()) {
                return result.getB();
            }
        }
        return stack;
    }

    @Override
    public Fluid getContainedFluid(ItemStack stack) {
        ContainerItemContext context = new StackItemContext(stack);
        Storage<FluidVariant> storage = context.find(FluidStorage.ITEM);
        if (storage instanceof UniversalBucketFluidStorage bucketFluidStorage) {
            for (StorageView<FluidVariant> view : bucketFluidStorage.nonEmptyViews()) {
                return view.getResource().getFluid();
            }
        }
        return Fluids.EMPTY;
    }

    @Override
    public ItemStack addFluid(ItemStack stack, Fluid fluid) {
        ContainerItemContext context = new StackItemContext(stack);
        Storage<FluidVariant> storage = context.find(FluidStorage.ITEM);
        if (storage instanceof UniversalBucketFluidStorage bucketFluidStorage) {
            try (Transaction transaction = Transaction.openOuter()) {
                if (bucketFluidStorage.insert(FluidVariant.of(fluid), FluidConstants.BUCKET, transaction) == FluidConstants.BUCKET) {
                    transaction.commit();
                }
            }
            ItemStack resultStack = context.getItemVariant().toStack();
            if (!resultStack.isEmpty()) {
                resultStack.set(BucketLibMod.STORAGE, new FluidStorageData(FluidVariant.of(fluid), FluidConstants.BUCKET));
            }
            return resultStack;
        }
        return stack.copy();
    }

    @Override
    public ItemStack removeFluid(ItemStack stack, ServerLevel level, @Nullable Player player) {
        ContainerItemContext context = new StackItemContext(stack);
        Storage<FluidVariant> storage = context.find(FluidStorage.ITEM);
        if (storage instanceof UniversalBucketFluidStorage bucketFluidStorage) {
            try (Transaction transaction = Transaction.openOuter()) {
                for (StorageView<FluidVariant> view : storage.nonEmptyViews()) {
                    bucketFluidStorage.extract(view.getResource(), FluidConstants.BUCKET, transaction);
                }
                transaction.commit();
            }
            ItemStack resultStack = context.getItemVariant().toStack();
            if (!resultStack.isEmpty()) {
                resultStack.remove(BucketLibMod.STORAGE);
            }
            return resultStack;
        }
        return stack.copy();
    }

    @Override
    public Tuple<Boolean, ItemStack> tryPickUpFluid(ItemStack stack, @Nullable Player player, Level level, InteractionHand interactionHand, BlockPos pos, Direction direction) {
        //Fluid Storage interaction
        Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, pos, direction.getOpposite());
        if (storage != null && player != null && FluidStorageUtil.interactWithFluidStorage(storage, player, interactionHand)) {
            return new Tuple<>(true, player.getItemInHand(interactionHand));
        }
        //Fluid Source / Waterlogged Block interaction
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof BucketPickup bucketPickup && RegistryUtil.getBucketBlock(block) == null) {
            ItemStack fullVanillaBucket = bucketPickup.pickupBlock(player, level, pos, state);
            if (!fullVanillaBucket.isEmpty() && fullVanillaBucket.getItem() instanceof BucketItem bucketItem) {
                Fluid fluid = Services.BUCKET.getFluidOfBucketItem(bucketItem);
                SoundEvent sound = bucketPickup.getPickupSound().orElse(FluidVariantAttributes.getFillSound(FluidVariant.of(fluid)));
                level.playSound(player, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
                ItemStack usedStack = stack.copy();
                usedStack.setCount(1);
                usedStack = BucketLibUtil.addFluid(usedStack, fluid);
                return new Tuple<>(true, usedStack);
            }
        }
        return new Tuple<>(false, stack);
    }

    @Override
    public Tuple<Boolean, ItemStack> tryPlaceFluid(ItemStack stack, @Nullable Player player, Level level, InteractionHand interactionHand, BlockPos pos) {
        //Fluid Storage interaction
        Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, pos, null);
        if (storage != null && player != null && FluidStorageUtil.interactWithFluidStorage(storage, player, interactionHand)) {
            return new Tuple<>(true, player.getItemInHand(interactionHand));
        }
        Fluid fluid = BucketLibUtil.getFluid(stack);
        ServerLevel serverLevel = (level instanceof ServerLevel) ? (ServerLevel) level : null;
        //vaporize
        if (level.dimensionType().ultraWarm() && fluid.is(FluidTags.WATER)) {
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            level.playSound(player, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);
            for (int i = 0; i < 8; ++i) {
                level.addParticle(ParticleTypes.LARGE_SMOKE, (double) x + Math.random(), (double) y + Math.random(), (double) z + Math.random(), 0.0, 0.0, 0.0);
            }
            return new Tuple<>(true, BucketLibUtil.removeFluid(stack, serverLevel, player));
        }
        //waterlogged Block interaction
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof LiquidBlockContainer liquidBlockContainer && liquidBlockContainer.canPlaceLiquid(player, level, pos, state, fluid)) {
            liquidBlockContainer.placeLiquid(level, pos, state, fluid.defaultFluidState());
            level.playSound(player, pos, FluidVariantAttributes.getEmptySound(FluidVariant.of(fluid)), SoundSource.BLOCKS, 1.0F, 1.0F);
            return new Tuple<>(true, BucketLibUtil.removeFluid(stack, serverLevel, player));
        }
        //air / replaceable block interaction
        if (state.isAir() || state.canBeReplaced(fluid) || (!state.getFluidState().isEmpty() && !(block instanceof LiquidBlockContainer))) {
            if (level.setBlock(pos, fluid.defaultFluidState().createLegacyBlock(), 11) || state.getFluidState().isSource()) {
                level.playSound(player, pos, FluidVariantAttributes.getEmptySound(FluidVariant.of(fluid)), SoundSource.BLOCKS, 1.0F, 1.0F);
                return new Tuple<>(true, BucketLibUtil.removeFluid(stack, serverLevel, player));
            }
        }
        return new Tuple<>(false, stack);
    }

    @Override
    public void curePotionEffects(LivingEntity entity, ItemStack curativeItem) {
        entity.removeAllEffects();
    }

}
