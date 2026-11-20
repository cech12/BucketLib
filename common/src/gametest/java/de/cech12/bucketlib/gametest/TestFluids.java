package de.cech12.bucketlib.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Fluids of the test mod "bucketlibtest", like other mods add them. They cover the cases that vanilla fluids do not
 * have: a modded fluid with own bucket and own attributes, and fluids without a usable bucket (no bucket, a bucket of
 * another fluid, the milk bucket, a getBucket() that throws like the "noBucket" fluids of Registrate).
 * <p>
 * The fluids are source only and cannot be placed in the world (their block is air). The loader adapters register them
 * with their attributes (Fabric: FluidVariantAttributes, NeoForge: FluidType), see {@link #ALL}.
 */
public final class TestFluids {

    /** fluid tag bucketlibtest:test_fluids (contains all test fluids) */
    public static final TagKey<Fluid> TEST_FLUIDS_TAG = TagKey.create(Registries.FLUID, TestBuckets.id("test_fluids"));

    /** what getBucket() of the fluid returns */
    public enum BucketKind {
        /** an own BucketItem of this fluid ({@link Definition#bucketId()}) */
        OWN_BUCKET,
        /** no bucket (air), like most technical fluids */
        NO_BUCKET,
        /** the water bucket, a BucketItem of another fluid */
        FOREIGN_BUCKET,
        /** the vanilla milk bucket, although the fluid is not the milk fluid */
        MILK_BUCKET,
        /** throws an IllegalArgumentException like "noBucket" fluids of Registrate */
        THROWING
    }

    /**
     * @param name fluid name in the namespace of the test mod
     * @param bucketKind what getBucket() returns
     * @param temperature temperature (water 300, lava 1300)
     * @param lightLevel light level
     * @param lighterThanAir whether the fluid is lighter than air (gas)
     */
    public record Definition(String name, BucketKind bucketKind, int temperature, int lightLevel, boolean lighterThanAir) {
        /** @return fluid id */
        public Identifier id() {
            return TestBuckets.id(this.name);
        }

        /** @return id of the own bucket item (only used for {@link BucketKind#OWN_BUCKET}) */
        public Identifier bucketId() {
            return TestBuckets.id(this.name + "_bucket");
        }

        /** @return the registered fluid */
        public Fluid fluid() {
            return BuiltInRegistries.FLUID.getValue(this.id());
        }

        /** @return the registered own bucket item */
        public Item bucket() {
            return BuiltInRegistries.ITEM.getValue(this.bucketId());
        }

        /**
         * @param fluid the fluid instance of this definition (registration order of the loaders may differ)
         * @return a new own bucket item (only for registration)
         */
        public BucketItem createBucket(Fluid fluid) {
            return new BucketItem(fluid, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, this.bucketId())).craftRemainder(Items.BUCKET).stacksTo(1));
        }
    }

    /** hot glowing gas with own bucket (temperature 2000, light level 10, lighter than air) */
    public static final Definition HOT_GAS = new Definition("hot_gas", BucketKind.OWN_BUCKET, 2000, 10, true);
    /** fluid without bucket */
    public static final Definition NO_BUCKET_FLUID = new Definition("no_bucket_fluid", BucketKind.NO_BUCKET, 300, 0, false);
    /** fluid whose bucket is the water bucket */
    public static final Definition FOREIGN_BUCKET_FLUID = new Definition("foreign_bucket_fluid", BucketKind.FOREIGN_BUCKET, 300, 0, false);
    /** fluid whose bucket is the milk bucket, but which is not the milk fluid */
    public static final Definition MILK_BUCKET_FLUID = new Definition("milk_bucket_fluid", BucketKind.MILK_BUCKET, 300, 0, false);
    /** fluid whose getBucket() throws */
    public static final Definition THROWING_BUCKET_FLUID = new Definition("throwing_bucket_fluid", BucketKind.THROWING, 300, 0, false);

    /** all test fluids in registration order */
    public static final List<Definition> ALL = List.of(HOT_GAS, NO_BUCKET_FLUID, FOREIGN_BUCKET_FLUID, MILK_BUCKET_FLUID, THROWING_BUCKET_FLUID);

    /**
     * true in the game test runs (system property {@code bucketlibtest.gameTestRun}, set by the gametest runs of the
     * loaders), false in the client runs for manual tests.
     */
    public static final boolean GAME_TEST_RUN = Boolean.getBoolean("bucketlibtest.gameTestRun");

    /**
     * The test fluids that are registered. The fluids that simulate broken mods ({@link #MILK_BUCKET_FLUID} triggers
     * KI-19 on NeoForge without milk fluid, {@link #THROWING_BUCKET_FLUID} logs an error for every bucket) are only
     * registered in the game test runs, so that the creative inventory of the client runs keeps working.
     */
    public static final List<Definition> REGISTERED = GAME_TEST_RUN ? ALL
            : ALL.stream().filter(definition -> definition != MILK_BUCKET_FLUID && definition != THROWING_BUCKET_FLUID).toList();

    private TestFluids() {}

    /**
     * Source only test fluid. Abstract, because NeoForge needs a loader specific fluid type: the loader adapters create
     * subclasses.
     */
    public abstract static class TestFluid extends Fluid {

        private final Definition definition;

        protected TestFluid(Definition definition) {
            this.definition = definition;
        }

        /** @return definition of this fluid */
        public Definition definition() {
            return this.definition;
        }

        @Override
        public Item getBucket() {
            return switch (this.definition.bucketKind()) {
                case OWN_BUCKET -> BuiltInRegistries.ITEM.getValue(this.definition.bucketId());
                case NO_BUCKET -> Items.AIR;
                case FOREIGN_BUCKET -> Items.WATER_BUCKET;
                case MILK_BUCKET -> Items.MILK_BUCKET;
                case THROWING -> throw new IllegalArgumentException("Fluid " + this.definition.id() + " has no bucket (test fluid)");
            };
        }

        @Override
        protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid other, Direction direction) {
            return true;
        }

        @Override
        protected Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState fluidState) {
            return Vec3.ZERO;
        }

        @Override
        public int getTickDelay(LevelReader level) {
            return 5;
        }

        @Override
        protected float getExplosionResistance() {
            return 100.0F;
        }

        @Override
        public float getHeight(FluidState fluidState, BlockGetter level, BlockPos pos) {
            return 0.0F;
        }

        @Override
        public float getOwnHeight(FluidState fluidState) {
            return 0.0F;
        }

        @Override
        protected BlockState createLegacyBlock(FluidState fluidState) {
            return Blocks.AIR.defaultBlockState();
        }

        @Override
        public boolean isSource(FluidState fluidState) {
            return true;
        }

        @Override
        public int getAmount(FluidState fluidState) {
            return 8;
        }

        @Override
        public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) {
            return Shapes.empty();
        }
    }

}
