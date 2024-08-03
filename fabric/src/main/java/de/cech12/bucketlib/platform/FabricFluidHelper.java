package de.cech12.bucketlib.platform;

import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
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
                return result.getB();
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
            return context.getItemVariant().toStack();
        }
        return stack.copy();
    }

    @Override
    public ItemStack removeFluid(ItemStack stack) {
        ContainerItemContext context = new StackItemContext(stack);
        Storage<FluidVariant> storage = context.find(FluidStorage.ITEM);
        if (storage instanceof UniversalBucketFluidStorage bucketFluidStorage) {
            try (Transaction transaction = Transaction.openOuter()) {
                for (StorageView<FluidVariant> view : storage.nonEmptyViews()) {
                    bucketFluidStorage.extract(view.getResource(), FluidConstants.BUCKET, transaction);
                }
                transaction.commit();
            }
            return context.getItemVariant().toStack();
        }
        return stack.copy();
    }

    @Override
    public Tuple<Boolean, ItemStack> tryPickUpFluid(ItemStack stack, @Nullable Player player, Level level, InteractionHand interactionHand, BlockPos pos, Direction direction) {
        //Fluid Storage interaction
        Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, pos, direction.getOpposite());
        if (storage != null && player != null && FluidStorageUtil.interactWithFluidStorage(storage, player, interactionHand)) {
            return new Tuple<>(true, player.getItemInHand(interactionHand).copy());
        }
        //Fluid Source / Waterlogged Block interaction
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof BucketPickup bucketPickup && RegistryUtil.getBucketBlock(block) == null) {
            ItemStack fullVanillaBucket = bucketPickup.pickupBlock(player, level, pos, state);
            if (fullVanillaBucket.getItem() instanceof BucketItem bucketItem) {
                Fluid fluid = Services.BUCKET.getFluidOfBucketItem(bucketItem);
                if (stack.getItem() instanceof UniversalBucketItem universalBucketItem && universalBucketItem.canHoldFluid(fluid)) {
                    SoundEvent sound = bucketPickup.getPickupSound().orElse(FluidVariantAttributes.getFillSound(FluidVariant.of(fluid)));
                    level.playSound(player, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
                    ItemStack usedStack = stack.copy();
                    usedStack.setCount(1);
                    usedStack = BucketLibUtil.addFluid(usedStack, fluid);
                    return new Tuple<>(true, usedStack);
                }
                if (!level.isClientSide() && player != null) {
                    ((ServerPlayer) player).connection.send(new ClientboundSetActionBarTextPacket(Component.literal("This fluid cannot be hold by the used fluid container.")));
                }
                level.setBlock(pos, state, 3);
                return new Tuple<>(false, stack);
            }
            //show incompatibility message and reset the block state
            if (!fullVanillaBucket.isEmpty()) {
                if (!level.isClientSide() && player != null) {
                    ((ServerPlayer) player).connection.send(new ClientboundSetActionBarTextPacket(Component.literal(fullVanillaBucket.getItem() + " is not compatible with BucketLib.")));
                    BucketLib.LOG.warn("{} is not an instance of BucketItem and is incompatible with BucketLib.", fullVanillaBucket.getItem());
                }
                level.setBlock(pos, state, 3);
                return new Tuple<>(false, stack);
            }
        }
        return new Tuple<>(false, stack);
    }

    @Override
    public Tuple<Boolean, ItemStack> tryPlaceFluid(ItemStack stack, @Nullable Player player, Level level, InteractionHand interactionHand, BlockPos pos) {
        //Fluid Storage interaction
        Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, pos, null);
        if (storage != null && player != null && FluidStorageUtil.interactWithFluidStorage(storage, player, interactionHand)) {
            return new Tuple<>(true, player.getItemInHand(interactionHand).copy());
        }
        Fluid fluid = BucketLibUtil.getFluid(stack);
        //vaporize
        if (level.dimensionType().ultraWarm() && fluid.is(FluidTags.WATER)) {
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            level.playSound(player, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);
            for (int i = 0; i < 8; ++i) {
                level.addParticle(ParticleTypes.LARGE_SMOKE, (double) x + Math.random(), (double) y + Math.random(), (double) z + Math.random(), 0.0, 0.0, 0.0);
            }
            return new Tuple<>(true, BucketLibUtil.removeFluid(stack));
        }
        //waterlogged Block interaction
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof LiquidBlockContainer liquidBlockContainer && liquidBlockContainer.canPlaceLiquid(player, level, pos, state, fluid)) {
            liquidBlockContainer.placeLiquid(level, pos, state, fluid.defaultFluidState());
            level.playSound(player, pos, FluidVariantAttributes.getEmptySound(FluidVariant.of(fluid)), SoundSource.BLOCKS, 1.0F, 1.0F);
            return new Tuple<>(true, BucketLibUtil.removeFluid(stack));
        }
        //air / replaceable block interaction
        if (state.isAir() || state.canBeReplaced(fluid) || (!state.getFluidState().isEmpty() && !(block instanceof LiquidBlockContainer))) {
            if (level.setBlock(pos, fluid.defaultFluidState().createLegacyBlock(), 11) || state.getFluidState().isSource()) {
                level.playSound(player, pos, FluidVariantAttributes.getEmptySound(FluidVariant.of(fluid)), SoundSource.BLOCKS, 1.0F, 1.0F);
                return new Tuple<>(true, BucketLibUtil.removeFluid(stack));
            }
        }
        return new Tuple<>(false, stack);
    }

    @Override
    public void curePotionEffects(LivingEntity entity, ItemStack curativeItem) {
        entity.removeAllEffects();
    }

}
