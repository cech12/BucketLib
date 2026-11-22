package de.cech12.bucketlib.gametest.tests;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.gametest.BucketLibGameTests;
import de.cech12.bucketlib.gametest.GameTestPlatform;
import de.cech12.bucketlib.gametest.KnownIssues;
import de.cech12.bucketlib.gametest.Platforms;
import de.cech12.bucketlib.gametest.TestBuckets;
import de.cech12.bucketlib.gametest.TestFluids;
import de.cech12.bucketlib.gametest.TestUtils;
import de.cech12.bucketlib.platform.Services;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluid;

import java.util.List;
import java.util.Map;

/**
 * Fluids of other mods (test fluids of the test mod, see {@link TestFluids}): a modded fluid with own bucket and own
 * attributes works like a vanilla fluid, fluids without a usable bucket are rejected without crashing.
 */
public final class ModdedFluidTests {

    private ModdedFluidTests() {}

    public static void register(BucketLibGameTests.TestCollector tests) {
        tests.add("modded_fluid/test_fluids_are_registered", ModdedFluidTests::testFluidsAreRegistered);
        tests.add("modded_fluid/attributes_of_the_loader_are_used", ModdedFluidTests::attributesOfTheLoaderAreUsed);
        tests.add("modded_fluid/fluid_with_own_bucket_can_be_held", ModdedFluidTests::fluidWithOwnBucketCanBeHeld);
        tests.add("modded_fluid/temperature_properties_use_the_fluid_temperature", ModdedFluidTests::temperaturePropertiesUseTheFluidTemperature);
        tests.add("modded_fluid/fluid_api_with_modded_fluid", ModdedFluidTests::fluidApiWithModdedFluid);
        tests.add("modded_fluid/fluids_without_bucket_cannot_be_held", ModdedFluidTests::fluidsWithoutBucketCannotBeHeld);
        tests.add("modded_fluid/fluid_with_milk_bucket_cannot_be_held", ModdedFluidTests::fluidWithMilkBucketCannotBeHeld);
        tests.add("modded_fluid/fluid_whose_bucket_throws_cannot_be_held", ModdedFluidTests::fluidWhoseBucketThrowsCannotBeHeld);
        tests.add("modded_fluid/creative_tab_lists_only_holdable_fluids", ModdedFluidTests::creativeTabListsOnlyHoldableFluids);
        tests.add("modded_fluid/fluid_ingredient_with_modded_fluids", ModdedFluidTests::fluidIngredientWithModdedFluids);
        tests.add("modded_fluid/incompatible_fluid_source_is_not_picked_up", ModdedFluidTests::incompatibleFluidSourceIsNotPickedUp);
        tests.add("modded_fluid/dispenser_does_not_pick_up_incompatible_fluid_source", ModdedFluidTests::dispenserDoesNotPickUpIncompatibleFluidSource);
    }

    /** The test fluids and the bucket of the hot gas are registered (test setup). */
    public static void testFluidsAreRegistered(GameTestHelper helper) {
        for (TestFluids.Definition definition : TestFluids.ALL) {
            Fluid fluid = definition.fluid();
            helper.assertTrue(fluid instanceof TestFluids.TestFluid, "test fluid " + definition.id() + " is not registered: " + fluid);
            helper.assertTrue(fluid.defaultFluidState().is(TestFluids.TEST_FLUIDS_TAG), definition.id() + " is not in " + TestFluids.TEST_FLUIDS_TAG.location());
        }
        TestUtils.assertEquals(helper, TestFluids.HOT_GAS.fluid().getBucket(), TestFluids.HOT_GAS.bucket(), "bucket of the hot gas");
        TestUtils.assertEquals(helper, Services.BUCKET.getFluidOfBucketItem((net.minecraft.world.item.BucketItem) TestFluids.HOT_GAS.bucket()), TestFluids.HOT_GAS.fluid(), "fluid of the hot gas bucket");
        helper.succeed();
    }

    /** BucketLib reads temperature, light level, gas property and name of a modded fluid from the loader. */
    public static void attributesOfTheLoaderAreUsed(GameTestHelper helper) {
        Fluid gas = TestFluids.HOT_GAS.fluid();
        TestUtils.assertEquals(helper, Services.FLUID.getFluidTemperature(gas), 2000, "temperature of the hot gas");
        TestUtils.assertEquals(helper, Services.FLUID.getFluidLightLevel(gas), 10, "light level of the hot gas");
        helper.assertTrue(Services.FLUID.isFluidLighterThanAir(gas), "hot gas is not lighter than air");
        helper.assertFalse(Services.FLUID.isFluidLighterThanAir(TestFluids.NO_BUCKET_FLUID.fluid()), "no bucket fluid is lighter than air");
        var description = Services.FLUID.getFluidDescription(gas);
        helper.assertTrue(description.getContents() instanceof TranslatableContents translatable && translatable.getKey().equals("fluid.bucketlibtest.hot_gas"), "name of the hot gas: " + description);
        helper.succeed();
    }

    /** A modded fluid with an own bucket can be held like a vanilla fluid and gives the bucket its name. */
    public static void fluidWithOwnBucketCanBeHeld(GameTestHelper helper) {
        Fluid gas = TestFluids.HOT_GAS.fluid();
        helper.assertTrue(TestBuckets.TEST_BUCKET.item().canHoldFluid(gas), "test bucket cannot hold the hot gas");
        ItemStack stack = TestUtils.withFluid(TestBuckets.TEST_BUCKET, gas);
        TestUtils.assertEquals(helper, BucketLibUtil.getFluid(stack), gas, "fluid in the bucket");
        var name = stack.getHoverName();
        helper.assertTrue(name.getContents() instanceof TranslatableContents translatable && translatable.getKey().endsWith(UniversalBucketItem.DESCRIPTION_FILLED_SUFFIX),
                "name of the hot gas bucket: " + name);
        ItemStack removed = BucketLibUtil.removeFluid(stack, helper.getLevel(), null);
        helper.assertTrue(BucketLibUtil.isEmpty(removed), "hot gas was not removed");
        // allowed lists: only water
        helper.assertFalse(TestBuckets.WATER_ALLOWING_BUCKET.item().canHoldFluid(gas), "water allowing bucket can hold the hot gas");
        helper.assertTrue(TestBuckets.WATER_DENYING_BUCKET.item().canHoldFluid(gas), "water denying bucket cannot hold the hot gas");
        helper.succeed();
    }

    /** Temperature properties (max/min temperature, cracking, burning) use the temperature of the modded fluid (2000). */
    public static void temperaturePropertiesUseTheFluidTemperature(GameTestHelper helper) {
        Fluid gas = TestFluids.HOT_GAS.fluid();
        helper.assertFalse(TestBuckets.TEMPERATURE_BUCKET.item().canHoldFluid(gas), "temperature bucket (max 1000) can hold the hot gas");
        helper.assertTrue(TestBuckets.MIN_TEMPERATURE_BUCKET.item().canHoldFluid(gas), "min temperature bucket (min 500) cannot hold the hot gas");
        helper.assertTrue(TestBuckets.CRACKING_BUCKET.item().isCracked(TestUtils.withFluid(TestBuckets.CRACKING_BUCKET, gas)), "cracking bucket with hot gas is not cracked");
        ItemStack burning = TestUtils.withFluid(TestBuckets.BURNING_BUCKET, gas);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, burning);
        burning.getItem().inventoryTick(burning, helper.getLevel(), player, EquipmentSlot.MAINHAND);
        helper.assertTrue(player.getRemainingFireTicks() > 0, "holder of a burning bucket with hot gas is not on fire");
        helper.succeed();
    }

    /** Other mods can fill and drain BucketLib buckets with a modded fluid through the fluid API. */
    public static void fluidApiWithModdedFluid(GameTestHelper helper) {
        Fluid gas = TestFluids.HOT_GAS.fluid();
        var filled = GameTestPlatform.INSTANCE.insertFluid(TestUtils.bucket(TestBuckets.TEST_BUCKET), gas, 1000);
        TestUtils.assertEquals(helper, filled.millibuckets(), 1000, "inserted hot gas");
        TestUtils.assertEquals(helper, GameTestPlatform.INSTANCE.getFluidContent(filled.result()), new de.cech12.bucketlib.gametest.IGameTestPlatform.FluidContent(gas, 1000), "content after inserting hot gas");
        var drained = GameTestPlatform.INSTANCE.extractFluid(filled.result(), gas, 1000);
        TestUtils.assertEquals(helper, drained.millibuckets(), 1000, "extracted hot gas");
        helper.assertTrue(BucketLibUtil.isEmpty(drained.result()), "bucket is not empty after draining the hot gas");
        TestUtils.assertEquals(helper, GameTestPlatform.INSTANCE.insertFluid(TestUtils.bucket(TestBuckets.TEMPERATURE_BUCKET), gas, 1000).millibuckets(), 0, "hot gas inserted into the temperature bucket");
        TestUtils.assertEquals(helper, GameTestPlatform.INSTANCE.insertFluid(TestUtils.bucket(TestBuckets.TEST_BUCKET), TestFluids.NO_BUCKET_FLUID.fluid(), 1000).millibuckets(), 0, "fluid without bucket inserted");
        helper.succeed();
    }

    private static void assertCannotBeHeld(GameTestHelper helper, TestFluids.Definition definition) {
        Fluid fluid = definition.fluid();
        for (TestBuckets.Definition bucket : List.of(TestBuckets.TEST_BUCKET, TestBuckets.WATER_DENYING_BUCKET, TestBuckets.MIN_TEMPERATURE_BUCKET)) {
            boolean canHold = TestUtils.noException(helper, bucket.name() + " canHoldFluid(" + definition.id() + ")", () -> bucket.item().canHoldFluid(fluid));
            helper.assertFalse(canHold, bucket.name() + " can hold " + definition.id());
        }
        ItemStack added = TestUtils.noException(helper, "adding " + definition.id(), () -> BucketLibUtil.addFluid(TestUtils.bucket(TestBuckets.TEST_BUCKET), fluid));
        helper.assertTrue(BucketLibUtil.isEmpty(added), definition.id() + " was added: " + TestUtils.describe(added));
    }

    /** Fluids without bucket and fluids whose bucket belongs to another fluid cannot be held (no vanilla bucket exists). */
    public static void fluidsWithoutBucketCannotBeHeld(GameTestHelper helper) {
        assertCannotBeHeld(helper, TestFluids.NO_BUCKET_FLUID);
        assertCannotBeHeld(helper, TestFluids.FOREIGN_BUCKET_FLUID);
        helper.succeed();
    }

    /** A fluid whose bucket is the milk bucket, but which is not the milk fluid, cannot be held. */
    public static void fluidWithMilkBucketCannotBeHeld(GameTestHelper helper) {
        assertCannotBeHeld(helper, TestFluids.MILK_BUCKET_FLUID);
        helper.succeed();
    }

    /**
     * A fluid whose getBucket() throws (like "noBucket" fluids of Registrate) cannot be held, BucketLib logs an error
     * instead of crashing.
     */
    public static void fluidWhoseBucketThrowsCannotBeHeld(GameTestHelper helper) {
        int errors = de.cech12.bucketlib.gametest.ExpectedLogs.incompatibleFluidErrors();
        assertCannotBeHeld(helper, TestFluids.THROWING_BUCKET_FLUID);
        helper.assertTrue(de.cech12.bucketlib.gametest.ExpectedLogs.incompatibleFluidErrors() > errors, "BucketLib did not log the incompatible fluid");
        helper.succeed();
    }

    /** The creative tab lists the modded fluid with own bucket, but no fluid without usable bucket. */
    public static void creativeTabListsOnlyHoldableFluids(GameTestHelper helper) {
        TestUtils.noException(helper, "building the creative tabs", () -> CreativeModeTabs.tryRebuildTabContents(helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess()));
        var items = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(Services.PLATFORM.getToolsAndUtilitiesTab()).getDisplayItems();
        Map<Fluid, Long> testFluidBuckets = new java.util.HashMap<>();
        for (ItemStack stack : items) {
            if (stack.is(TestBuckets.TEST_BUCKET.item()) && BucketLibUtil.getFluid(stack) instanceof TestFluids.TestFluid fluid) {
                testFluidBuckets.merge(fluid, 1L, Long::sum);
            }
        }
        TestUtils.assertEquals(helper, testFluidBuckets.get(TestFluids.HOT_GAS.fluid()), 1L, "test buckets with hot gas in the creative tab");
        TestUtils.assertEquals(helper, testFluidBuckets.size(), 1, "test fluids in the creative tab " + testFluidBuckets.keySet());
        helper.succeed();
    }

    /**
     * A fluid ingredient with a fluid tag that contains modded fluids accepts their buckets and shows only fluids that
     * have a bucket (fluids without usable bucket are skipped).
     */
    public static void fluidIngredientWithModdedFluids(GameTestHelper helper) {
        Ingredient ingredient = TestUtils.ingredient(helper, "fluid", Map.of("tag", TestFluids.TEST_FLUIDS_TAG.location().toString()));
        helper.assertTrue(ingredient.test(new ItemStack(TestFluids.HOT_GAS.bucket())), "fluid ingredient does not accept the hot gas bucket");
        helper.assertTrue(ingredient.test(TestUtils.withFluid(TestBuckets.TEST_BUCKET, TestFluids.HOT_GAS.fluid())), "fluid ingredient does not accept a test bucket with hot gas");
        helper.assertFalse(ingredient.test(TestUtils.withFluid(TestBuckets.TEST_BUCKET, net.minecraft.world.level.material.Fluids.WATER)), "fluid ingredient accepts water");
        // the shown items (recipe book, recipe viewers) skip fluids without usable bucket
        Ingredient withBucket = TestUtils.ingredient(helper, "fluid", Map.of("fluid", TestFluids.HOT_GAS.id().toString()));
        var shownGas = withBucket.items().map(holder -> holder.value()).toList();
        helper.assertTrue(shownGas.contains(TestFluids.HOT_GAS.bucket()) && shownGas.contains(TestBuckets.TEST_BUCKET.item()), "fluid ingredient does not show the hot gas buckets: " + shownGas);
        helper.assertFalse(shownGas.contains(TestBuckets.TEMPERATURE_BUCKET.item()), "fluid ingredient shows the temperature bucket: " + shownGas);
        for (TestFluids.Definition definition : List.of(TestFluids.NO_BUCKET_FLUID, TestFluids.FOREIGN_BUCKET_FLUID, TestFluids.MILK_BUCKET_FLUID)) {
            Ingredient without = TestUtils.ingredient(helper, "fluid", Map.of("fluid", definition.id().toString()));
            var shown = TestUtils.noException(helper, "items of the fluid ingredient of " + definition.id(), () -> without.items().map(holder -> holder.value()).toList());
            helper.assertTrue(shown.isEmpty(), "fluid ingredient of " + definition.id() + " shows " + shown);
        }
        var shown = TestUtils.noException(helper, "items of the fluid ingredient of the test fluid tag", () -> ingredient.items().map(holder -> holder.value()).toList());
        helper.assertTrue(shown.contains(TestFluids.HOT_GAS.bucket()), "fluid ingredient of the tag does not show the hot gas bucket: " + shown);
        helper.succeed();
    }

    /**
     * A block of another mod that is picked up like a fluid, but gives no BucketItem, is not picked up: the block stays
     * (it is restored), the bucket stays empty and nothing crashes.
     */
    public static void incompatibleFluidSourceIsNotPickedUp(GameTestHelper helper) {
        var pos = TestUtils.CENTER;
        helper.setBlock(pos, de.cech12.bucketlib.gametest.TestBlocks.incompatibleFluidSource());
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.bucket(TestBuckets.TEST_BUCKET));
        TestUtils.lookDownAt(helper, player, pos);
        TestUtils.noException(helper, "using an empty bucket at an incompatible fluid source", () -> TestUtils.use(helper, player));
        helper.assertBlockPresent(de.cech12.bucketlib.gametest.TestBlocks.incompatibleFluidSource(), pos);
        helper.assertTrue(BucketLibUtil.isEmpty(player.getMainHandItem()) && player.getMainHandItem().is(TestBuckets.TEST_BUCKET.item()), "bucket was changed: " + TestUtils.describe(player.getMainHandItem()));
        helper.succeed();
    }

    /** A dispenser with an empty bucket does not pick up an incompatible fluid source (the block stays). */
    public static void dispenserDoesNotPickUpIncompatibleFluidSource(GameTestHelper helper) {
        var target = new net.minecraft.core.BlockPos(2, 1, 2);
        var dispenserPos = new net.minecraft.core.BlockPos(2, 1, 1);
        helper.setBlock(target, de.cech12.bucketlib.gametest.TestBlocks.incompatibleFluidSource());
        helper.setBlock(dispenserPos, net.minecraft.world.level.block.Blocks.DISPENSER.defaultBlockState().setValue(net.minecraft.world.level.block.DispenserBlock.FACING, net.minecraft.core.Direction.SOUTH));
        var dispenser = helper.getBlockEntity(dispenserPos, net.minecraft.world.level.block.entity.DispenserBlockEntity.class);
        dispenser.setItem(0, TestUtils.bucket(TestBuckets.TEST_BUCKET));
        helper.setBlock(dispenserPos.west(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(5, () -> {
            helper.assertBlockPresent(de.cech12.bucketlib.gametest.TestBlocks.incompatibleFluidSource(), target);
            boolean filled = false;
            for (int slot = 0; slot < dispenser.getContainerSize(); slot++) {
                filled |= !dispenser.getItem(slot).isEmpty() && !BucketLibUtil.isEmpty(dispenser.getItem(slot));
            }
            helper.assertFalse(filled, "a bucket in the dispenser was filled");
            helper.succeed();
        });
    }

}
