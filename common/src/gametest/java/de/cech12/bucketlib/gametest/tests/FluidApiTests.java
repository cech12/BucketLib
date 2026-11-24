package de.cech12.bucketlib.gametest.tests;

import de.cech12.bucketlib.gametest.BucketLibGameTests;
import de.cech12.bucketlib.gametest.GameTestPlatform;
import de.cech12.bucketlib.gametest.IGameTestPlatform.FluidContent;
import de.cech12.bucketlib.gametest.IGameTestPlatform.FluidTransfer;
import de.cech12.bucketlib.gametest.TestBuckets;
import de.cech12.bucketlib.gametest.TestUtils;
import de.cech12.bucketlib.platform.Services;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

/**
 * The fluid transfer API of the loader (Fabric Transfer API / NeoForge transfer capability): other mods (tanks, pipes,
 * machines) can read, fill and drain BucketLib buckets, always by whole buckets (1000 mB).
 */
public final class FluidApiTests {

    private FluidApiTests() {}

    public static void register(BucketLibGameTests.TestCollector tests) {
        tests.add("fluid_api/reads_the_content", FluidApiTests::readsTheContent);
        tests.add("fluid_api/fills_whole_buckets_only", FluidApiTests::fillsWholeBucketsOnly);
        tests.add("fluid_api/respects_the_properties", FluidApiTests::respectsTheProperties);
        tests.add("fluid_api/does_not_fill_filled_buckets", FluidApiTests::doesNotFillFilledBuckets);
        tests.add("fluid_api/drains_whole_buckets_only", FluidApiTests::drainsWholeBucketsOnly);
        tests.add("fluid_api/does_not_drain_entity_buckets", FluidApiTests::doesNotDrainEntityBuckets);
        tests.add("fluid_api/milk", FluidApiTests::milk);
        tests.add("fluid_api/draining_damages_the_bucket", FluidApiTests::drainingDamagesTheBucket);
        tests.add("fluid_api/draining_breaks_cracking_buckets", FluidApiTests::drainingBreaksCrackingBuckets);
    }

    public static void readsTheContent(GameTestHelper helper) {
        TestUtils.assertEquals(helper, GameTestPlatform.INSTANCE.getFluidContent(TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER)), new FluidContent(Fluids.WATER, 1000), "content of a water bucket");
        TestUtils.assertEquals(helper, GameTestPlatform.INSTANCE.getFluidContent(TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.LAVA)), new FluidContent(Fluids.LAVA, 1000), "content of a lava bucket");
        TestUtils.assertEquals(helper, GameTestPlatform.INSTANCE.getFluidContent(TestUtils.bucket(TestBuckets.TEST_BUCKET)), FluidContent.EMPTY, "content of an empty bucket");
        TestUtils.assertEquals(helper, GameTestPlatform.INSTANCE.getFluidContent(TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW)), FluidContent.EMPTY, "content of a powder snow bucket");
        TestUtils.assertEquals(helper, GameTestPlatform.INSTANCE.getFluidContent(TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.SALMON)), new FluidContent(Fluids.WATER, 1000), "content of a salmon bucket");
        helper.succeed();
    }

    /** An empty bucket accepts exactly one bucket (1000 mB) of a fluid, less is not accepted. */
    public static void fillsWholeBucketsOnly(GameTestHelper helper) {
        ItemStack empty = TestUtils.bucket(TestBuckets.TEST_BUCKET);
        FluidTransfer full = GameTestPlatform.INSTANCE.insertFluid(empty, Fluids.WATER, 1000);
        TestUtils.assertEquals(helper, full.millibuckets(), 1000, "inserted water");
        helper.assertTrue(full.result().is(TestBuckets.TEST_BUCKET.item()) && BucketLibUtil.getFluid(full.result()) == Fluids.WATER, "result is no water bucket: " + TestUtils.describe(full.result()));
        FluidTransfer more = GameTestPlatform.INSTANCE.insertFluid(empty, Fluids.WATER, 2500);
        TestUtils.assertEquals(helper, more.millibuckets(), 1000, "inserted water of 2500 mB");
        FluidTransfer less = GameTestPlatform.INSTANCE.insertFluid(empty, Fluids.WATER, 999);
        TestUtils.assertEquals(helper, less.millibuckets(), 0, "inserted water of 999 mB");
        helper.assertTrue(BucketLibUtil.isEmpty(less.result()), "bucket was filled with 999 mB");
        helper.succeed();
    }

    public static void respectsTheProperties(GameTestHelper helper) {
        TestUtils.assertEquals(helper, GameTestPlatform.INSTANCE.insertFluid(TestUtils.bucket(TestBuckets.TEMPERATURE_BUCKET), Fluids.LAVA, 1000).millibuckets(), 0, "lava inserted into the temperature bucket");
        TestUtils.assertEquals(helper, GameTestPlatform.INSTANCE.insertFluid(TestUtils.bucket(TestBuckets.WATER_DENYING_BUCKET), Fluids.WATER, 1000).millibuckets(), 0, "water inserted into the water denying bucket");
        TestUtils.assertEquals(helper, GameTestPlatform.INSTANCE.insertFluid(TestUtils.bucket(TestBuckets.WATER_ALLOWING_TAG_BUCKET), Fluids.WATER, 1000).millibuckets(), 1000, "water inserted into the water allowing bucket");
        helper.succeed();
    }

    /** Filled buckets (fluid, milk, entity, block) accept no fluid. */
    public static void doesNotFillFilledBuckets(GameTestHelper helper) {
        for (ItemStack stack : java.util.List.of(TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER), TestUtils.withMilk(TestBuckets.TEST_BUCKET),
                TestUtils.withEntity(TestBuckets.TEST_BUCKET, null, EntityTypes.SULFUR_CUBE))) {
            assertNoInsertion(helper, stack);
        }
        assertNoInsertion(helper, TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW));
        helper.succeed();
    }

    private static void assertNoInsertion(GameTestHelper helper, ItemStack stack) {
        TestUtils.assertEquals(helper, GameTestPlatform.INSTANCE.insertFluid(stack, Fluids.WATER, 1000).millibuckets(), 0, "water inserted into " + TestUtils.describe(stack));
        TestUtils.assertEquals(helper, GameTestPlatform.INSTANCE.insertFluid(stack, Fluids.LAVA, 1000).millibuckets(), 0, "lava inserted into " + TestUtils.describe(stack));
    }

    /** A filled bucket gives exactly one bucket of its fluid and becomes empty, less is not given. */
    public static void drainsWholeBucketsOnly(GameTestHelper helper) {
        ItemStack water = TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER);
        FluidTransfer full = GameTestPlatform.INSTANCE.extractFluid(water, Fluids.WATER, 1000);
        TestUtils.assertEquals(helper, full.millibuckets(), 1000, "extracted water");
        helper.assertTrue(full.result().is(TestBuckets.TEST_BUCKET.item()) && BucketLibUtil.isEmpty(full.result()), "result is no empty bucket: " + TestUtils.describe(full.result()));
        TestUtils.assertEquals(helper, GameTestPlatform.INSTANCE.extractFluid(water, Fluids.WATER, 500).millibuckets(), 0, "extracted water of 500 mB");
        TestUtils.assertEquals(helper, GameTestPlatform.INSTANCE.extractFluid(water, Fluids.LAVA, 1000).millibuckets(), 0, "extracted lava from a water bucket");
        TestUtils.assertEquals(helper, GameTestPlatform.INSTANCE.extractFluid(TestUtils.bucket(TestBuckets.TEST_BUCKET), Fluids.WATER, 1000).millibuckets(), 0, "extracted water from an empty bucket");
        helper.succeed();
    }

    /** The water of a bucket with an entity cannot be drained (the entity would be lost). */
    public static void doesNotDrainEntityBuckets(GameTestHelper helper) {
        FluidTransfer transfer = GameTestPlatform.INSTANCE.extractFluid(TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.SALMON), Fluids.WATER, 1000);
        TestUtils.assertEquals(helper, transfer.millibuckets(), 0, "extracted water from a salmon bucket");
        helper.assertTrue(BucketLibUtil.containsEntityType(transfer.result()), "salmon was removed");
        helper.succeed();
    }

    /**
     * Milk is only a fluid with the NeoForge milk fluid: then milk buckets can be drained and filled with milk,
     * otherwise milk is invisible for the fluid API.
     */
    public static void milk(GameTestHelper helper) {
        ItemStack milk = TestUtils.withMilk(TestBuckets.TEST_BUCKET);
        if (Services.FLUID.hasMilkFluid()) {
            var milkFluid = Services.FLUID.getMilkFluid();
            TestUtils.assertEquals(helper, GameTestPlatform.INSTANCE.getFluidContent(milk), new FluidContent(milkFluid, 1000), "content of a milk bucket");
            FluidTransfer drained = GameTestPlatform.INSTANCE.extractFluid(milk, milkFluid, 1000);
            TestUtils.assertEquals(helper, drained.millibuckets(), 1000, "extracted milk");
            helper.assertTrue(BucketLibUtil.isEmpty(drained.result()), "milk bucket is not empty after draining: " + TestUtils.describe(drained.result()));
            FluidTransfer filled = GameTestPlatform.INSTANCE.insertFluid(TestUtils.bucket(TestBuckets.TEST_BUCKET), milkFluid, 1000);
            TestUtils.assertEquals(helper, filled.millibuckets(), 1000, "inserted milk");
        } else {
            TestUtils.assertEquals(helper, GameTestPlatform.INSTANCE.getFluidContent(milk), FluidContent.EMPTY, "content of a milk bucket without milk fluid");
            TestUtils.assertEquals(helper, GameTestPlatform.INSTANCE.extractFluid(milk, Fluids.WATER, 1000).millibuckets(), 0, "extracted water from a milk bucket");
            helper.assertTrue(BucketLibUtil.containsMilk(GameTestPlatform.INSTANCE.extractFluid(milk, Fluids.WATER, 1000).result()), "milk was removed");
        }
        helper.succeed();
    }

    /** Draining through the fluid API costs one durability point and breaks a used up bucket. */
    public static void drainingDamagesTheBucket(GameTestHelper helper) {
        FluidTransfer transfer = GameTestPlatform.INSTANCE.extractFluid(TestUtils.withFluid(TestBuckets.DURABILITY_BUCKET, Fluids.WATER), Fluids.WATER, 1000);
        TestUtils.assertEquals(helper, transfer.result().getDamageValue(), 1, "damage after draining");
        ItemStack usedUp = TestUtils.withFluid(TestBuckets.DURABILITY_BUCKET, Fluids.WATER);
        usedUp.setDamageValue(4);
        FluidTransfer broken = GameTestPlatform.INSTANCE.extractFluid(usedUp, Fluids.WATER, 1000);
        TestUtils.assertEquals(helper, broken.millibuckets(), 1000, "extracted water of a used up bucket");
        helper.assertTrue(broken.result().isEmpty(), "used up bucket did not break: " + TestUtils.describe(broken.result()));
        FluidTransfer filled = GameTestPlatform.INSTANCE.insertFluid(TestUtils.bucket(TestBuckets.DURABILITY_BUCKET), Fluids.WATER, 1000);
        TestUtils.assertEquals(helper, filled.result().getDamageValue(), 0, "damage after filling");
        helper.succeed();
    }

    /** Draining a cracking bucket with a cracking fluid uses it up. */
    public static void drainingBreaksCrackingBuckets(GameTestHelper helper) {
        FluidTransfer transfer = GameTestPlatform.INSTANCE.extractFluid(TestUtils.withFluid(TestBuckets.CRACKING_BUCKET, Fluids.LAVA), Fluids.LAVA, 1000);
        TestUtils.assertEquals(helper, transfer.millibuckets(), 1000, "extracted lava");
        helper.assertTrue(transfer.result().isEmpty(), "cracking bucket did not break: " + TestUtils.describe(transfer.result()));
        helper.succeed();
    }

}
