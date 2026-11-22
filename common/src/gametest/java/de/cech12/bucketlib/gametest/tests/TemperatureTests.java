package de.cech12.bucketlib.gametest.tests;

import de.cech12.bucketlib.gametest.BucketLibGameTests;
import de.cech12.bucketlib.gametest.TestBuckets;
import de.cech12.bucketlib.gametest.TestUtils;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

import java.util.List;

/**
 * Buckets with hot contents set their holder on fire, buckets with cold contents freeze their holder (both lose
 * durability while doing so).
 */
public final class TemperatureTests {

    private TemperatureTests() {}

    public static void register(BucketLibGameTests.TestCollector tests) {
        tests.add("temperature/hot_contents_burn_the_holder", TemperatureTests::hotContentsBurnTheHolder);
        tests.add("temperature/cold_contents_freeze_the_holder", TemperatureTests::coldContentsFreezeTheHolder);
        tests.add("temperature/fully_frozen_holder_is_hurt", TemperatureTests::fullyFrozenHolderIsHurt);
        tests.add("temperature/normal_contents_do_nothing", TemperatureTests::normalContentsDoNothing);
        tests.add("temperature/fire_immune_and_creative_holders", TemperatureTests::fireImmuneAndCreativeHolders);
    }

    private static void tick(GameTestHelper helper, Entity holder, ItemStack stack) {
        stack.getItem().inventoryTick(stack, helper.getLevel(), holder, EquipmentSlot.MAINHAND);
    }

    /** Fluids at or above the burning temperature, burning fluids and burning blocks set the holder on fire. */
    public static void hotContentsBurnTheHolder(GameTestHelper helper) {
        List<ItemStack> hot = List.of(
                TestUtils.withFluid(TestBuckets.BURNING_BUCKET, Fluids.LAVA),
                TestUtils.withBlock(TestBuckets.BURNING_BUCKET, Blocks.POWDER_SNOW),
                TestUtils.withFluid(TestBuckets.BURNING_FLUIDS_BUCKET, Fluids.WATER),
                TestUtils.withFluid(TestBuckets.BURNING_FLUIDS_TAG_BUCKET, Fluids.WATER),
                TestUtils.withBlock(TestBuckets.BURNING_FLUIDS_BUCKET, Blocks.POWDER_SNOW));
        for (ItemStack stack : hot) {
            ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, stack);
            player.setTicksFrozen(20);
            tick(helper, player, stack);
            helper.assertTrue(player.getRemainingFireTicks() > 0, "holder of " + TestUtils.describe(stack) + " is not on fire");
            TestUtils.assertEquals(helper, player.getTicksFrozen(), 0, "frozen ticks of the holder of " + TestUtils.describe(stack));
            // damaged once per second (tick count 0)
            TestUtils.assertEquals(helper, stack.getDamageValue(), 1, "damage of " + TestUtils.describe(stack));
        }
        helper.succeed();
    }

    /** Fluids at or below the freezing temperature, freezing fluids and freezing blocks freeze the holder. */
    public static void coldContentsFreezeTheHolder(GameTestHelper helper) {
        List<ItemStack> cold = List.of(
                TestUtils.withFluid(TestBuckets.FREEZING_BUCKET, Fluids.WATER),
                TestUtils.withBlock(TestBuckets.FREEZING_BUCKET, Blocks.POWDER_SNOW),
                TestUtils.withFluid(TestBuckets.FREEZING_FLUIDS_BUCKET, Fluids.WATER),
                TestUtils.withFluid(TestBuckets.FREEZING_FLUIDS_TAG_BUCKET, Fluids.WATER),
                TestUtils.withBlock(TestBuckets.FREEZING_FLUIDS_BUCKET, Blocks.POWDER_SNOW));
        for (ItemStack stack : cold) {
            ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, stack);
            tick(helper, player, stack);
            TestUtils.assertEquals(helper, player.getTicksFrozen(), 3, "frozen ticks of the holder of " + TestUtils.describe(stack));
            TestUtils.assertEquals(helper, stack.getDamageValue(), 0, "damage of " + TestUtils.describe(stack) + " before the holder is fully frozen");
        }
        helper.succeed();
    }

    /** A fully frozen holder gets freeze damage every 2 seconds and the bucket loses durability. */
    public static void fullyFrozenHolderIsHurt(GameTestHelper helper) {
        ItemStack stack = TestUtils.withFluid(TestBuckets.FREEZING_BUCKET, Fluids.WATER);
        // a mob, because new players are invulnerable for some seconds
        var holder = helper.spawnWithNoFreeWill(EntityTypes.PIG, TestUtils.CENTER);
        holder.setTicksFrozen(holder.getTicksRequiredToFreeze());
        float health = holder.getHealth();
        holder.tickCount = 40;
        tick(helper, holder, stack);
        helper.assertTrue(holder.isFullyFrozen(), "holder is not fully frozen");
        helper.assertTrue(holder.getHealth() < health, "fully frozen holder was not hurt");
        TestUtils.assertEquals(helper, stack.getDamageValue(), 1, "damage after hurting the holder");
        // a holder in powder snow freezes slower (vanilla adds the freezing ticks of powder snow)
        ServerPlayer inSnow = TestUtils.player(helper, GameType.SURVIVAL, stack);
        inSnow.isInPowderSnow = true;
        tick(helper, inSnow, stack);
        TestUtils.assertEquals(helper, inSnow.getTicksFrozen(), 1, "frozen ticks of a holder in powder snow");
        helper.succeed();
    }

    private static void assertNothingHappens(GameTestHelper helper, ItemStack stack) {
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, stack);
        TestUtils.noException(helper, "inventory tick of " + TestUtils.describe(stack), () -> tick(helper, player, stack));
        helper.assertFalse(player.getRemainingFireTicks() > 0, "holder of " + TestUtils.describe(stack) + " is on fire");
        TestUtils.assertEquals(helper, player.getTicksFrozen(), 0, "frozen ticks of the holder of " + TestUtils.describe(stack));
        TestUtils.assertEquals(helper, stack.getDamageValue(), 0, "damage of " + TestUtils.describe(stack));
    }

    /**
     * Contents without burning or freezing properties do nothing, also for buckets whose burning or freezing blocks
     * are given as immutable list (List.of) while the bucket contains no block.
     */
    public static void normalContentsDoNothing(GameTestHelper helper) {
        for (ItemStack stack : List.of(TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.LAVA), TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW),
                TestUtils.withFluid(TestBuckets.BURNING_FLUIDS_TAG_BUCKET, Fluids.LAVA), TestUtils.withFluid(TestBuckets.FREEZING_FLUIDS_TAG_BUCKET, Fluids.LAVA))) {
            assertNothingHappens(helper, stack);
        }
        for (ItemStack stack : List.of(TestUtils.withFluid(TestBuckets.BURNING_BUCKET, Fluids.WATER), TestUtils.withFluid(TestBuckets.FREEZING_BUCKET, Fluids.LAVA),
                TestUtils.bucket(TestBuckets.BURNING_BUCKET), TestUtils.bucket(TestBuckets.FREEZING_BUCKET))) {
            assertNothingHappens(helper, stack);
        }
        helper.succeed();
    }

    /**
     * Fire immune holders do not burn, burning holders do not freeze, creative players are affected without damaging
     * the bucket.
     */
    public static void fireImmuneAndCreativeHolders(GameTestHelper helper) {
        ItemStack lava = TestUtils.withFluid(TestBuckets.BURNING_BUCKET, Fluids.LAVA);
        var blaze = helper.spawnWithNoFreeWill(EntityTypes.BLAZE, TestUtils.CENTER);
        tick(helper, blaze, lava);
        TestUtils.assertEquals(helper, lava.getDamageValue(), 0, "damage after a fire immune holder");
        ServerPlayer creative = TestUtils.player(helper, GameType.CREATIVE, lava);
        tick(helper, creative, lava);
        helper.assertTrue(creative.getRemainingFireTicks() > 0, "creative holder is not on fire");
        TestUtils.assertEquals(helper, lava.getDamageValue(), 0, "damage after a creative holder");
        ItemStack water = TestUtils.withFluid(TestBuckets.FREEZING_BUCKET, Fluids.WATER);
        ServerPlayer burning = TestUtils.player(helper, GameType.SURVIVAL, water);
        burning.setRemainingFireTicks(100);
        tick(helper, burning, water);
        TestUtils.assertEquals(helper, burning.getTicksFrozen(), 0, "frozen ticks of a burning holder");
        helper.succeed();
    }

}
