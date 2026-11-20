package de.cech12.bucketlib.gametest.tests;

import de.cech12.bucketlib.gametest.BucketLibGameTests;
import de.cech12.bucketlib.gametest.GameTestPlatform;
import de.cech12.bucketlib.gametest.KnownIssues;
import de.cech12.bucketlib.gametest.Platforms;
import de.cech12.bucketlib.gametest.TestBuckets;
import de.cech12.bucketlib.gametest.TestUtils;
import de.cech12.bucketlib.platform.Services;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.WorldInteractionUtil;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;

import java.util.List;
import java.util.Set;

/**
 * Milking cows, goats and mooshrooms and drinking milk like with a vanilla milk bucket.
 */
public final class MilkTests {

    private MilkTests() {}

    public static void register(BucketLibGameTests.TestCollector tests) {
        tests.add("milk/milks_milkable_entities", MilkTests::milksMilkableEntities);
        tests.add("milk/milks_one_of_stacked_buckets", MilkTests::milksOneOfStackedBuckets);
        tests.add("milk/creative_mode_keeps_the_empty_bucket", MilkTests::creativeModeKeepsTheEmptyBucket);
        tests.add("milk/does_not_milk_without_milking", MilkTests::doesNotMilkWithoutMilking);
        tests.add("milk/does_not_milk_other_entities", MilkTests::doesNotMilkOtherEntities);
        tests.add("milk/does_not_milk_with_filled_bucket", MilkTests::doesNotMilkWithFilledBucket);
        tests.add("milk/drinking_removes_effects", MilkTests::drinkingRemovesEffects);
        tests.add("milk/drinking_in_creative_mode_keeps_the_milk", MilkTests::drinkingInCreativeModeKeepsTheMilk);
        tests.add("milk/is_drunk_instead_of_placed", MilkTests::isDrunkInsteadOfPlaced);
        tests.add("milk/use_duration_and_animation", MilkTests::useDurationAndAnimation);
        tests.add("milk/milk_fluid_of_other_mods_can_be_drunk", MilkTests::milkFluidOfOtherModsCanBeDrunk);
    }

    /** Cows, goats and mooshrooms (tag bucketlib:milkable) can be milked, the entity is not changed. */
    public static void milksMilkableEntities(GameTestHelper helper) {
        for (EntityType<? extends LivingEntity> type : List.<EntityType<? extends LivingEntity>>of(EntityTypes.COW, EntityTypes.GOAT, EntityTypes.MOOSHROOM)) {
            LivingEntity entity = (LivingEntity) helper.spawnWithNoFreeWill((EntityType) type, TestUtils.CENTER);
            ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.bucket(TestBuckets.TEST_BUCKET));
            var result = TestUtils.interact(player, entity);
            helper.assertTrue(result.consumesAction(), "milking a " + type + " did not consume the action");
            helper.assertTrue(BucketLibUtil.containsMilk(player.getMainHandItem()), "milking a " + type + " did not fill the bucket: " + TestUtils.describe(player.getMainHandItem()));
            helper.assertTrue(player.getMainHandItem().is(TestBuckets.TEST_BUCKET.item()), "bucket was replaced after milking a " + type);
            helper.assertFalse(entity.isRemoved(), type + " was removed");
            entity.discard();
        }
        helper.succeed();
    }

    /** From a stack of empty buckets one is filled with milk and put into the inventory. */
    public static void milksOneOfStackedBuckets(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityTypes.COW, TestUtils.CENTER);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.bucket(TestBuckets.TEST_BUCKET).copyWithCount(2));
        TestUtils.interact(player, cow);
        ItemStack hand = player.getMainHandItem();
        helper.assertTrue(BucketLibUtil.isEmpty(hand) && hand.getCount() == 1, "hand does not hold 1 empty bucket: " + TestUtils.describe(hand));
        helper.assertTrue(player.getInventory().getNonEquipmentItems().stream().anyMatch(BucketLibUtil::containsMilk), "milk bucket is not in the inventory");
        helper.succeed();
    }

    /** In creative mode the empty bucket stays in the hand and a milk bucket is added to the inventory (like vanilla). */
    public static void creativeModeKeepsTheEmptyBucket(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityTypes.COW, TestUtils.CENTER);
        ServerPlayer player = TestUtils.player(helper, GameType.CREATIVE, TestUtils.bucket(TestBuckets.TEST_BUCKET));
        TestUtils.interact(player, cow);
        helper.assertTrue(BucketLibUtil.isEmpty(player.getMainHandItem()), "empty bucket in the hand was filled in creative mode");
        helper.assertTrue(player.getInventory().getNonEquipmentItems().stream().anyMatch(BucketLibUtil::containsMilk), "milk bucket is not in the inventory");
        helper.assertFalse(player.getInventory().contains(new ItemStack(Items.MILK_BUCKET)), "a vanilla milk bucket was added");
        helper.assertTrue(player.getAbilities().instabuild, "creative abilities were not restored");
        helper.succeed();
    }

    /** Buckets with disabled milking (constant or config) cannot milk. */
    public static void doesNotMilkWithoutMilking(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityTypes.COW, TestUtils.CENTER);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.bucket(TestBuckets.ANTI_MILK_BUCKET));
        TestUtils.interact(player, cow);
        helper.assertTrue(BucketLibUtil.isEmpty(player.getMainHandItem()), "anti milk bucket was filled with milk");
        try {
            TestBuckets.Config.milking = false;
            player.setItemInHand(InteractionHand.MAIN_HAND, TestUtils.bucket(TestBuckets.CONFIG_BUCKET));
            TestUtils.interact(player, cow);
            helper.assertTrue(BucketLibUtil.isEmpty(player.getMainHandItem()), "config bucket with disabled milking was filled with milk");
        } finally {
            TestBuckets.Config.reset();
        }
        helper.succeed();
    }

    /** Entities that are not milkable (pig) and baby cows cannot be milked. */
    public static void doesNotMilkOtherEntities(GameTestHelper helper) {
        var pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, TestUtils.CENTER);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.bucket(TestBuckets.TEST_BUCKET));
        helper.assertFalse(WorldInteractionUtil.tryMilkLivingEntity(player.getMainHandItem(), pig, player, InteractionHand.MAIN_HAND).consumesAction(), "a pig was milked");
        TestUtils.interact(player, pig);
        helper.assertTrue(BucketLibUtil.isEmpty(player.getMainHandItem()), "a pig was milked");
        Cow baby = helper.spawnWithNoFreeWill(EntityTypes.COW, TestUtils.CENTER);
        baby.setBaby(true);
        TestUtils.interact(player, baby);
        helper.assertTrue(BucketLibUtil.isEmpty(player.getMainHandItem()), "a baby cow was milked");
        helper.assertTrue(player.getMainHandItem().is(TestBuckets.TEST_BUCKET.item()), "bucket was replaced: " + player.getMainHandItem());
        helper.succeed();
    }

    /** A bucket that is not empty cannot milk. */
    public static void doesNotMilkWithFilledBucket(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityTypes.COW, TestUtils.CENTER);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER));
        TestUtils.interact(player, cow);
        helper.assertFalse(BucketLibUtil.containsMilk(player.getMainHandItem()), "water bucket was filled with milk");
        TestUtils.assertEquals(helper, BucketLibUtil.getFluid(player.getMainHandItem()), Fluids.WATER, "fluid after interacting with a cow");
        helper.succeed();
    }

    private static ItemStack drink(GameTestHelper helper, ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        player.startUsingItem(InteractionHand.MAIN_HAND);
        stack.onUseTick(helper.getLevel(), player, 32);
        ItemStack result = stack.finishUsingItem(helper.getLevel(), player);
        player.setItemInHand(InteractionHand.MAIN_HAND, result);
        player.stopUsingItem();
        return result;
    }

    /** Drinking milk removes all effects, counts as used milk bucket and empties the bucket. */
    public static void drinkingRemovesEffects(GameTestHelper helper) {
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.withMilk(TestBuckets.TEST_BUCKET));
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 1000));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 1000));
        helper.assertTrue(TestUtils.use(helper, player).consumesAction(), "drinking milk did not start");
        ItemStack result = drink(helper, player);
        helper.assertTrue(player.getActiveEffects().isEmpty(), "effects were not removed: " + player.getActiveEffects());
        helper.assertTrue(result.is(TestBuckets.TEST_BUCKET.item()) && BucketLibUtil.isEmpty(result), "bucket is not empty after drinking: " + TestUtils.describe(result));
        TestUtils.assertEquals(helper, player.getStats().getValue(Stats.ITEM_USED.get(Items.MILK_BUCKET)), 1, "used milk bucket statistic");
        helper.succeed();
    }

    /** In creative mode the bucket keeps its milk after drinking. */
    public static void drinkingInCreativeModeKeepsTheMilk(GameTestHelper helper) {
        ServerPlayer player = TestUtils.player(helper, GameType.CREATIVE, TestUtils.withMilk(TestBuckets.TEST_BUCKET));
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 1000));
        ItemStack result = drink(helper, player);
        helper.assertTrue(player.getActiveEffects().isEmpty(), "effects were not removed in creative mode");
        helper.assertTrue(BucketLibUtil.containsMilk(result), "milk was removed in creative mode");
        helper.succeed();
    }

    /** Using a milk bucket at a block starts drinking, no fluid is placed (also with the NeoForge milk fluid). */
    public static void isDrunkInsteadOfPlaced(GameTestHelper helper) {
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.withMilk(TestBuckets.TEST_BUCKET));
        TestUtils.lookDownAt(helper, player, TestUtils.CENTER.below());
        helper.assertTrue(TestUtils.use(helper, player).consumesAction(), "using milk at a block did not start drinking");
        helper.assertTrue(player.isUsingItem(), "player does not drink");
        helper.assertTrue(helper.getBlockState(TestUtils.CENTER).isAir(), "milk was placed: " + helper.getBlockState(TestUtils.CENTER));
        helper.assertTrue(BucketLibUtil.containsMilk(player.getMainHandItem()), "milk was removed without drinking");
        helper.succeed();
    }

    /** Milk is drunk in 32 ticks with the drink animation like vanilla milk, other contents are not drinkable. */
    public static void useDurationAndAnimation(GameTestHelper helper) {
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, ItemStack.EMPTY);
        ItemStack milk = TestUtils.withMilk(TestBuckets.TEST_BUCKET);
        TestUtils.assertEquals(helper, milk.getUseDuration(player), new ItemStack(Items.MILK_BUCKET).getUseDuration(player), "use duration of milk");
        TestUtils.assertEquals(helper, milk.getUseAnimation(), ItemUseAnimation.DRINK, "use animation of milk");
        ItemStack water = TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER);
        TestUtils.assertEquals(helper, water.getUseDuration(player), 0, "use duration of water");
        TestUtils.assertEquals(helper, water.getUseAnimation(), ItemUseAnimation.NONE, "use animation of water");
        water.onUseTick(helper.getLevel(), player, 10);
        // particles and sounds like vanilla milk during the whole drinking time
        for (int remaining = 32; remaining >= 0; remaining--) {
            int ticks = remaining;
            TestUtils.noException(helper, "drinking tick " + ticks, () -> milk.onUseTick(helper.getLevel(), player, ticks));
        }
        helper.succeed();
    }

    /**
     * NeoForge with enabled milk fluid: a bucket that another mod (e.g. a tank) filled with the milk fluid through the
     * fluid API is a milk bucket that can be drunk, like a bucket filled by milking.
     */
    public static void milkFluidOfOtherModsCanBeDrunk(GameTestHelper helper) {
        if (!Services.FLUID.hasMilkFluid()) {
            helper.succeed();
            return;
        }
        var transfer = GameTestPlatform.INSTANCE.insertFluid(TestUtils.bucket(TestBuckets.TEST_BUCKET), Services.FLUID.getMilkFluid(), 1000);
        TestUtils.assertEquals(helper, transfer.millibuckets(), 1000, "inserted milk");
        KnownIssues.run(helper, "KI-3 bucket filled with the milk fluid by the fluid API is no milk bucket", Set.of(Platforms.NEOFORGE), () ->
                helper.assertTrue(BucketLibUtil.containsMilk(transfer.result()), "bucket filled with the milk fluid is no milk bucket: " + TestUtils.describe(transfer.result())));
    }

}
