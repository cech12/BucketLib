package de.cech12.bucketlib.gametest.tests;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.gametest.BucketLibGameTests;
import de.cech12.bucketlib.gametest.TestBuckets;
import de.cech12.bucketlib.gametest.TestUtils;
import de.cech12.bucketlib.platform.Services;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.List;
import java.util.Map;

/**
 * The bucket properties (UniversalBucketItem.Properties) decide which fluids, entities and blocks a bucket can hold,
 * whether it can milk, how it stacks and its durability. Values can be constants or config suppliers.
 */
public final class PropertiesTests {

    private PropertiesTests() {}

    public static void register(BucketLibGameTests.TestCollector tests) {
        tests.add("properties/fluids", PropertiesTests::fluids);
        tests.add("properties/fluid_temperatures", PropertiesTests::fluidTemperatures);
        tests.add("properties/milk_fluid", PropertiesTests::milkFluid);
        tests.add("properties/entities", PropertiesTests::entities);
        tests.add("properties/sulfur_cube_state", PropertiesTests::sulfurCubeState);
        tests.add("properties/blocks", PropertiesTests::blocks);
        tests.add("properties/milking", PropertiesTests::milking);
        tests.add("properties/config_suppliers", PropertiesTests::configSuppliers);
        tests.add("properties/stack_size", PropertiesTests::stackSize);
        tests.add("properties/invalid_values_are_rejected", PropertiesTests::invalidValuesAreRejected);
        tests.add("properties/getters", PropertiesTests::getters);
        tests.add("properties/temperature_limits_are_inclusive", PropertiesTests::temperatureLimitsAreInclusive);
    }

    private static void assertFluids(GameTestHelper helper, TestBuckets.Definition bucket, boolean water, boolean lava) {
        UniversalBucketItem item = bucket.item();
        TestUtils.assertEquals(helper, item.canHoldFluid(Fluids.WATER), water, bucket.name() + " can hold water");
        TestUtils.assertEquals(helper, item.canHoldFluid(Fluids.LAVA), lava, bucket.name() + " can hold lava");
        helper.assertTrue(item.canHoldFluid(Fluids.EMPTY), bucket.name() + " cannot be empty");
        // flowing fluids have no own bucket
        helper.assertFalse(item.canHoldFluid(Fluids.FLOWING_WATER), bucket.name() + " can hold flowing water");
        helper.assertFalse(item.canHoldFluid(Fluids.FLOWING_LAVA), bucket.name() + " can hold flowing lava");
    }

    /** Allowed and denied fluid lists and tags. Allowed fluids win over denied fluids and temperatures. */
    public static void fluids(GameTestHelper helper) {
        assertFluids(helper, TestBuckets.TEST_BUCKET, true, true);
        assertFluids(helper, TestBuckets.WATER_ALLOWING_BUCKET, true, false);
        assertFluids(helper, TestBuckets.WATER_ALLOWING_TAG_BUCKET, true, false);
        assertFluids(helper, TestBuckets.WATER_DENYING_BUCKET, false, true);
        assertFluids(helper, TestBuckets.WATER_DENYING_TAG_BUCKET, false, true);
        helper.succeed();
    }

    /** Max and min temperature (water 300, lava 1300 on both loaders). */
    public static void fluidTemperatures(GameTestHelper helper) {
        TestUtils.assertEquals(helper, Services.FLUID.getFluidTemperature(Fluids.WATER), 300, "temperature of water");
        TestUtils.assertEquals(helper, Services.FLUID.getFluidTemperature(Fluids.LAVA), 1300, "temperature of lava");
        assertFluids(helper, TestBuckets.TEMPERATURE_BUCKET, true, false);
        assertFluids(helper, TestBuckets.MIN_TEMPERATURE_BUCKET, false, true);
        helper.succeed();
    }

    /** The milk fluid of NeoForge (if enabled) can be held by every bucket that can milk; Fabric has no milk fluid. */
    public static void milkFluid(GameTestHelper helper) {
        if (!Services.FLUID.hasMilkFluid()) {
            if (de.cech12.bucketlib.gametest.Platforms.isFabric()) {
                TestUtils.assertEquals(helper, Services.FLUID.getMilkFluid(), Fluids.EMPTY, "milk fluid on Fabric");
            }
            helper.succeed();
            return;
        }
        Fluid milk = Services.FLUID.getMilkFluid();
        helper.assertTrue(TestBuckets.TEST_BUCKET.item().canHoldFluid(milk), "test bucket cannot hold the milk fluid");
        helper.assertFalse(TestBuckets.WATER_ALLOWING_BUCKET.item().canHoldFluid(milk), "water allowing bucket can hold the milk fluid");
        helper.succeed();
    }

    private static void assertEntities(GameTestHelper helper, TestBuckets.Definition bucket, boolean salmon, boolean pufferfish) {
        UniversalBucketItem item = bucket.item();
        TestUtils.assertEquals(helper, item.canHoldEntity(EntityTypes.SALMON), salmon, bucket.name() + " can hold salmon");
        TestUtils.assertEquals(helper, item.canHoldEntity(EntityTypes.PUFFERFISH), pufferfish, bucket.name() + " can hold pufferfish");
    }

    /** Entity obtaining can be disabled, entities can be denied or allowed with lists and tags. */
    public static void entities(GameTestHelper helper) {
        assertEntities(helper, TestBuckets.TEST_BUCKET, true, true);
        for (EntityType<?> type : List.of(EntityTypes.COD, EntityTypes.TROPICAL_FISH, EntityTypes.AXOLOTL, EntityTypes.TADPOLE, EntityTypes.SULFUR_CUBE)) {
            helper.assertTrue(TestBuckets.TEST_BUCKET.item().canHoldEntity(type), "test bucket cannot hold " + type);
        }
        assertEntities(helper, TestBuckets.NO_ENTITIES_BUCKET, false, false);
        assertEntities(helper, TestBuckets.ANTI_SALMON_BUCKET, false, true);
        assertEntities(helper, TestBuckets.ANTI_SALMON_TAG_BUCKET, false, true);
        assertEntities(helper, TestBuckets.ONLY_PUFFER_BUCKET, false, true);
        assertEntities(helper, TestBuckets.ONLY_PUFFER_TAG_BUCKET, false, true);
        helper.succeed();
    }

    /** Like vanilla buckets, only adult sulfur cubes that are not primed can be picked up. */
    public static void sulfurCubeState(GameTestHelper helper) {
        UniversalBucketItem bucket = TestBuckets.TEST_BUCKET.item();
        SulfurCube adult = helper.spawnWithNoFreeWill(EntityTypes.SULFUR_CUBE, TestUtils.CENTER);
        adult.setSize(2, true);
        adult.setBaby(false);
        helper.assertTrue(bucket.canHoldEntity(adult), "an adult sulfur cube cannot be held");
        SulfurCube baby = helper.spawnWithNoFreeWill(EntityTypes.SULFUR_CUBE, TestUtils.CENTER.east());
        baby.setSize(1, true);
        helper.assertTrue(baby.isBaby(), "test setup broken: sulfur cube is no baby");
        helper.assertFalse(bucket.canHoldEntity(baby), "a baby sulfur cube can be held");
        helper.assertFalse(TestBuckets.NO_ENTITIES_BUCKET.item().canHoldEntity(adult), "no entities bucket can hold a sulfur cube");
        var salmon = helper.spawnWithNoFreeWill(EntityTypes.SALMON, TestUtils.CENTER.west());
        helper.assertTrue(bucket.canHoldEntity(salmon), "a salmon cannot be held");
        helper.succeed();
    }

    private static void assertPowderSnow(GameTestHelper helper, TestBuckets.Definition bucket, boolean expected) {
        TestUtils.assertEquals(helper, bucket.item().canHoldBlock(Blocks.POWDER_SNOW), expected, bucket.name() + " can hold powder snow");
    }

    /** Block obtaining can be disabled, blocks can be denied or allowed with lists and tags. */
    public static void blocks(GameTestHelper helper) {
        assertPowderSnow(helper, TestBuckets.TEST_BUCKET, true);
        assertPowderSnow(helper, TestBuckets.NO_BLOCKS_BUCKET, false);
        assertPowderSnow(helper, TestBuckets.ANTI_POWDER_SNOW_BUCKET, false);
        assertPowderSnow(helper, TestBuckets.ANTI_POWDER_SNOW_TAG_BUCKET, false);
        assertPowderSnow(helper, TestBuckets.ONLY_POWDER_SNOW_BUCKET, true);
        assertPowderSnow(helper, TestBuckets.ONLY_POWDER_SNOW_TAG_BUCKET, true);
        assertPowderSnow(helper, TestBuckets.ONLY_STONE_BUCKET, false);
        helper.assertTrue(TestBuckets.ONLY_STONE_BUCKET.item().canHoldBlock(Blocks.STONE), "only stone bucket cannot hold stone");
        helper.succeed();
    }

    /** Milking is enabled by default and can be disabled. */
    public static void milking(GameTestHelper helper) {
        helper.assertTrue(TestBuckets.TEST_BUCKET.item().canMilkEntities(), "test bucket cannot milk");
        helper.assertFalse(TestBuckets.ANTI_MILK_BUCKET.item().canMilkEntities(), "anti milk bucket can milk");
        helper.succeed();
    }

    /**
     * Every property that has a supplier variant reads the current config value, so config changes apply to existing
     * buckets. The test changes the values and restores them in the same tick.
     */
    public static void configSuppliers(GameTestHelper helper) {
        UniversalBucketItem bucket = TestBuckets.CONFIG_BUCKET.item();
        try {
            assertFluids(helper, TestBuckets.CONFIG_BUCKET, true, false);
            TestUtils.assertEquals(helper, bucket.getDurability(), TestBuckets.Config.DEFAULT_DURABILITY, "durability of the config bucket");
            TestBuckets.Config.maxTemperature = 2000;
            assertFluids(helper, TestBuckets.CONFIG_BUCKET, true, true);
            TestBuckets.Config.minTemperature = 500;
            assertFluids(helper, TestBuckets.CONFIG_BUCKET, false, true);
            helper.assertTrue(bucket.canMilkEntities() && bucket.canHoldEntity(EntityTypes.SALMON) && bucket.canHoldBlock(Blocks.POWDER_SNOW), "config bucket cannot milk, hold salmon or powder snow");
            TestBuckets.Config.milking = false;
            TestBuckets.Config.entityObtaining = false;
            TestBuckets.Config.blockObtaining = false;
            helper.assertFalse(bucket.canMilkEntities(), "config bucket can milk with disabled milking");
            helper.assertFalse(bucket.canHoldEntity(EntityTypes.SALMON), "config bucket can hold salmon with disabled entity obtaining");
            helper.assertFalse(bucket.canHoldBlock(Blocks.POWDER_SNOW), "config bucket can hold powder snow with disabled block obtaining");
            TestBuckets.Config.durability = 7;
            TestUtils.assertEquals(helper, bucket.getDurability(), 7, "durability after a config change");
            Map<String, Integer> temperatures = Map.of("upper cracking", 11, "lower cracking", 12, "burning", 13, "freezing", 14);
            TestBuckets.Config.upperCrackingTemperature = 11;
            TestBuckets.Config.lowerCrackingTemperature = 12;
            TestBuckets.Config.burningTemperature = 13;
            TestBuckets.Config.freezingTemperature = 14;
            TestUtils.assertEquals(helper, bucket.getUpperBreakTemperature(), temperatures.get("upper cracking"), "upper cracking temperature");
            TestUtils.assertEquals(helper, bucket.getLowerBreakTemperature(), temperatures.get("lower cracking"), "lower cracking temperature");
            TestUtils.assertEquals(helper, bucket.getBurningTemperature(), temperatures.get("burning"), "burning temperature");
            TestUtils.assertEquals(helper, bucket.getFreezingTemperature(), temperatures.get("freezing"), "freezing temperature");
            TestUtils.assertEquals(helper, bucket.getMaxTemperature(), 2000, "max temperature");
            TestUtils.assertEquals(helper, bucket.getMinTemperature(), 500, "min temperature");
        } finally {
            TestBuckets.Config.reset();
        }
        helper.succeed();
    }

    /**
     * Empty buckets stack like the properties say (default 16 like vanilla buckets), filled buckets never stack. Also
     * checked for item stack templates (used e.g. for recipe results and crafting remainders).
     */
    public static void stackSize(GameTestHelper helper) {
        Map<TestBuckets.Definition, Integer> emptySizes = Map.of(TestBuckets.TEST_BUCKET, 16, TestBuckets.STACKING_BUCKET, 64, TestBuckets.SINGLE_BUCKET, 1);
        emptySizes.forEach((bucket, size) -> {
            TestUtils.assertEquals(helper, TestUtils.bucket(bucket).getMaxStackSize(), size, "max stack size of an empty " + bucket.name());
            TestUtils.assertEquals(helper, ItemStackTemplate.fromNonEmptyStack(TestUtils.bucket(bucket)).getMaxStackSize(), size, "max stack size of an empty " + bucket.name() + " template");
            TestUtils.assertEquals(helper, bucket.item().getMaxStackSize(TestUtils.bucket(bucket)), size, "UniversalBucketItem#getMaxStackSize of an empty " + bucket.name());
        });
        for (ItemStack filled : List.of(
                TestUtils.withFluid(TestBuckets.STACKING_BUCKET, Fluids.WATER),
                TestUtils.withMilk(TestBuckets.STACKING_BUCKET),
                TestUtils.withEntity(TestBuckets.STACKING_BUCKET, null, EntityTypes.SULFUR_CUBE),
                TestUtils.withBlock(TestBuckets.STACKING_BUCKET, Blocks.POWDER_SNOW))) {
            TestUtils.assertEquals(helper, filled.getMaxStackSize(), 1, "max stack size of " + TestUtils.describe(filled));
            TestUtils.assertEquals(helper, ItemStackTemplate.fromNonEmptyStack(filled).getMaxStackSize(), 1, "max stack size of the template of " + TestUtils.describe(filled));
        }
        helper.succeed();
    }

    /** A stack size lower than 1 and a negative durability are rejected when the bucket is created. */
    public static void invalidValuesAreRejected(GameTestHelper helper) {
        boolean stackSizeRejected = false;
        try {
            new UniversalBucketItem.Properties().stacksTo(0);
        } catch (RuntimeException e) {
            stackSizeRejected = true;
        }
        helper.assertTrue(stackSizeRejected, "stack size 0 is not rejected");
        boolean durabilityRejected = false;
        try {
            new UniversalBucketItem.Properties().durability(-1);
        } catch (RuntimeException e) {
            durabilityRejected = true;
        }
        helper.assertTrue(durabilityRejected, "durability -1 is not rejected");
        helper.succeed();
    }

    /** Getters of the properties: not set values are null (temperatures) or 0 (durability). */
    public static void getters(GameTestHelper helper) {
        UniversalBucketItem test = TestBuckets.TEST_BUCKET.item();
        TestUtils.assertEquals(helper, test.getDurability(), 0, "default durability");
        helper.assertTrue(test.getMaxTemperature() == null && test.getMinTemperature() == null
                && test.getUpperBreakTemperature() == null && test.getLowerBreakTemperature() == null
                && test.getBurningTemperature() == null && test.getFreezingTemperature() == null, "default temperatures are set");
        TestUtils.assertEquals(helper, TestBuckets.DURABILITY_BUCKET.item().getDurability(), 5, "durability");
        TestUtils.assertEquals(helper, TestBuckets.TEMPERATURE_BUCKET.item().getMaxTemperature(), 1000, "max temperature");
        TestUtils.assertEquals(helper, TestBuckets.MIN_TEMPERATURE_BUCKET.item().getMinTemperature(), 500, "min temperature");
        TestUtils.assertEquals(helper, TestBuckets.CRACKING_BUCKET.item().getUpperBreakTemperature(), 1000, "upper cracking temperature");
        TestUtils.assertEquals(helper, TestBuckets.LOWER_CRACKING_BUCKET.item().getLowerBreakTemperature(), 500, "lower cracking temperature");
        TestUtils.assertEquals(helper, TestBuckets.BURNING_BUCKET.item().getBurningTemperature(), 1000, "burning temperature");
        TestUtils.assertEquals(helper, TestBuckets.FREEZING_BUCKET.item().getFreezingTemperature(), 500, "freezing temperature");
        helper.assertTrue(TestBuckets.BURNING_BUCKET.item().isBurningBlock(Blocks.POWDER_SNOW), "powder snow is no burning block");
        helper.assertTrue(TestBuckets.BURNING_FLUIDS_BUCKET.item().isBurningFluid(Fluids.WATER), "water is no burning fluid (list)");
        helper.assertTrue(TestBuckets.BURNING_FLUIDS_TAG_BUCKET.item().isBurningFluid(Fluids.WATER), "water is no burning fluid (tag)");
        helper.assertTrue(TestBuckets.BURNING_FLUIDS_BUCKET.item().isBurningBlock(Blocks.POWDER_SNOW), "powder snow is no burning block (tag)");
        helper.assertTrue(TestBuckets.FREEZING_BUCKET.item().isFreezingBlock(Blocks.POWDER_SNOW), "powder snow is no freezing block");
        helper.assertTrue(TestBuckets.FREEZING_FLUIDS_BUCKET.item().isFreezingFluid(Fluids.WATER), "water is no freezing fluid (list)");
        helper.assertTrue(TestBuckets.FREEZING_FLUIDS_TAG_BUCKET.item().isFreezingFluid(Fluids.WATER), "water is no freezing fluid (tag)");
        helper.assertTrue(TestBuckets.FREEZING_FLUIDS_BUCKET.item().isFreezingBlock(Blocks.POWDER_SNOW), "powder snow is no freezing block (tag)");
        helper.assertFalse(test.isBurningFluid(Fluids.LAVA) || test.isFreezingFluid(Fluids.WATER) || test.isBurningBlock(Blocks.POWDER_SNOW) || test.isFreezingBlock(Blocks.POWDER_SNOW), "test bucket has burning or freezing contents");
        helper.assertFalse(test.isBurningBlock(null), "null is a burning block");
        helper.succeed();
    }

    /**
     * The temperature limits include the limit itself: max temperature 1300 holds lava (1300), 1299 not; an upper
     * cracking temperature of 1300 cracks with lava, 1301 not; the same for min and lower cracking temperature.
     */
    public static void temperatureLimitsAreInclusive(GameTestHelper helper) {
        var bucket = TestBuckets.CONFIG_BUCKET.item();
        ItemStack lava = TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.LAVA);
        try {
            TestBuckets.Config.maxTemperature = 1300;
            helper.assertTrue(bucket.canHoldFluid(Fluids.LAVA), "max temperature 1300 does not allow lava (1300)");
            TestBuckets.Config.maxTemperature = 1299;
            helper.assertFalse(bucket.canHoldFluid(Fluids.LAVA), "max temperature 1299 allows lava (1300)");
            TestBuckets.Config.maxTemperature = 2000;
            TestBuckets.Config.minTemperature = 300;
            helper.assertTrue(bucket.canHoldFluid(Fluids.WATER), "min temperature 300 does not allow water (300)");
            TestBuckets.Config.minTemperature = 301;
            helper.assertFalse(bucket.canHoldFluid(Fluids.WATER), "min temperature 301 allows water (300)");
            TestBuckets.Config.minTemperature = 0;
            ItemStack configLava = BucketLibUtil.addFluid(TestUtils.bucket(TestBuckets.CONFIG_BUCKET), Fluids.LAVA);
            ItemStack configWater = BucketLibUtil.addFluid(TestUtils.bucket(TestBuckets.CONFIG_BUCKET), Fluids.WATER);
            TestBuckets.Config.upperCrackingTemperature = 1300;
            helper.assertTrue(bucket.isCracked(configLava), "upper cracking temperature 1300 does not crack with lava");
            TestBuckets.Config.upperCrackingTemperature = 1301;
            helper.assertFalse(bucket.isCracked(configLava), "upper cracking temperature 1301 cracks with lava");
            TestBuckets.Config.lowerCrackingTemperature = 300;
            helper.assertTrue(bucket.isCracked(configWater), "lower cracking temperature 300 does not crack with water");
            TestBuckets.Config.lowerCrackingTemperature = 299;
            helper.assertFalse(bucket.isCracked(configWater), "lower cracking temperature 299 cracks with water");
            helper.assertTrue(!lava.isEmpty(), "test setup broken");
        } finally {
            TestBuckets.Config.reset();
        }
        helper.succeed();
    }

}
