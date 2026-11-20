package de.cech12.bucketlib.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Blocks of the test mod "bucketlibtest" for cases that vanilla blocks do not have. The loader adapters register them
 * (while the block registry is open, see {@link #create()}).
 */
public final class TestBlocks {

    /** id of a block of another mod that can be picked up with a bucket, but gives an item that is no BucketItem */
    public static final Identifier INCOMPATIBLE_FLUID_SOURCE = TestBuckets.id("incompatible_fluid_source");

    private TestBlocks() {}

    /** @return a new incompatible fluid source block (only for registration) */
    public static Block create() {
        return new IncompatibleFluidSourceBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, INCOMPATIBLE_FLUID_SOURCE)));
    }

    /** @return the registered incompatible fluid source block */
    public static Block incompatibleFluidSource() {
        return BuiltInRegistries.BLOCK.getValue(INCOMPATIBLE_FLUID_SOURCE);
    }

    /**
     * A block that is picked up like a fluid source (it is removed), but gives a stick instead of a bucket, like fluids
     * of mods that use own bucket classes.
     */
    public static final class IncompatibleFluidSourceBlock extends Block implements BucketPickup {

        public IncompatibleFluidSourceBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        public ItemStack pickupBlock(@Nullable LivingEntity user, LevelAccessor level, BlockPos pos, BlockState state) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
            return new ItemStack(Items.STICK);
        }

        @Override
        public Optional<SoundEvent> getPickupSound() {
            return Optional.empty();
        }
    }

}
