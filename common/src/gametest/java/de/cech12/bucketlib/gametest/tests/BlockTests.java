package de.cech12.bucketlib.gametest.tests;

import de.cech12.bucketlib.gametest.BucketLibGameTests;
import de.cech12.bucketlib.gametest.TestBuckets;
import de.cech12.bucketlib.gametest.TestUtils;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Picking up and placing blocks that have a vanilla bucket (powder snow) like a player with a vanilla bucket.
 */
public final class BlockTests {

    private static final BlockPos TARGET = TestUtils.CENTER;

    private BlockTests() {}

    public static void register(BucketLibGameTests.TestCollector tests) {
        tests.add("block/picks_up_powder_snow", BlockTests::picksUpPowderSnow);
        tests.add("block/does_not_pick_up_denied_powder_snow", BlockTests::doesNotPickUpDeniedPowderSnow);
        tests.add("block/places_powder_snow", BlockTests::placesPowderSnow);
        tests.add("block/creative_mode_keeps_the_bucket", BlockTests::creativeModeKeepsTheBucket);
        tests.add("block/picks_up_one_of_stacked_buckets", BlockTests::picksUpOneOfStackedBuckets);
        tests.add("block/creative_pickup_keeps_the_empty_bucket", BlockTests::creativePickupKeepsTheEmptyBucket);
    }

    /** Every bucket that can hold powder snow picks it up (also with allowed lists and tags). */
    public static void picksUpPowderSnow(GameTestHelper helper) {
        for (TestBuckets.Definition bucket : List.of(TestBuckets.TEST_BUCKET, TestBuckets.ONLY_POWDER_SNOW_BUCKET, TestBuckets.ONLY_POWDER_SNOW_TAG_BUCKET)) {
            helper.setBlock(TARGET, Blocks.POWDER_SNOW);
            ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.bucket(bucket));
            TestUtils.lookDownAt(helper, player, TARGET);
            TestUtils.use(helper, player);
            ItemStack hand = player.getMainHandItem();
            helper.assertTrue(hand.is(bucket.item()), bucket.name() + " was replaced by " + hand);
            TestUtils.assertEquals(helper, BucketLibUtil.getBlock(hand), Blocks.POWDER_SNOW, "block in the " + bucket.name());
            helper.assertBlockPresent(Blocks.AIR, TARGET);
        }
        helper.succeed();
    }

    /** Buckets that cannot hold powder snow leave it in the world. */
    public static void doesNotPickUpDeniedPowderSnow(GameTestHelper helper) {
        for (TestBuckets.Definition bucket : List.of(TestBuckets.NO_BLOCKS_BUCKET, TestBuckets.ANTI_POWDER_SNOW_BUCKET, TestBuckets.ANTI_POWDER_SNOW_TAG_BUCKET, TestBuckets.ONLY_STONE_BUCKET)) {
            helper.setBlock(TARGET, Blocks.POWDER_SNOW);
            ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.bucket(bucket));
            TestUtils.lookDownAt(helper, player, TARGET);
            TestUtils.use(helper, player);
            helper.assertTrue(BucketLibUtil.isEmpty(player.getMainHandItem()), bucket.name() + " picked up powder snow");
            helper.assertBlockPresent(Blocks.POWDER_SNOW, TARGET);
        }
        helper.succeed();
    }

    /** A bucket with powder snow places it like a vanilla powder snow bucket and becomes empty. */
    public static void placesPowderSnow(GameTestHelper helper) {
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW));
        TestUtils.lookDownAt(helper, player, TARGET.below());
        TestUtils.use(helper, player);
        helper.assertBlockPresent(Blocks.POWDER_SNOW, TARGET);
        ItemStack hand = player.getMainHandItem();
        helper.assertTrue(hand.is(TestBuckets.TEST_BUCKET.item()) && BucketLibUtil.isEmpty(hand), "bucket is not empty: " + TestUtils.describe(hand));
        helper.succeed();
    }

    /** In creative mode the bucket keeps the powder snow when placing it. */
    public static void creativeModeKeepsTheBucket(GameTestHelper helper) {
        ServerPlayer player = TestUtils.player(helper, GameType.CREATIVE, TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW));
        TestUtils.lookDownAt(helper, player, TARGET.below());
        TestUtils.use(helper, player);
        helper.assertBlockPresent(Blocks.POWDER_SNOW, TARGET);
        TestUtils.assertEquals(helper, BucketLibUtil.getBlock(player.getMainHandItem()), Blocks.POWDER_SNOW, "block in the bucket after placing in creative mode");
        helper.succeed();
    }

    public static void picksUpOneOfStackedBuckets(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.POWDER_SNOW);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.bucket(TestBuckets.TEST_BUCKET).copyWithCount(2));
        TestUtils.lookDownAt(helper, player, TARGET);
        TestUtils.use(helper, player);
        helper.assertBlockPresent(Blocks.AIR, TARGET);
        ItemStack hand = player.getMainHandItem();
        helper.assertTrue(BucketLibUtil.isEmpty(hand) && hand.getCount() == 1, "hand does not hold 1 empty bucket: " + TestUtils.describe(hand));
        helper.assertTrue(player.getInventory().getNonEquipmentItems().stream().anyMatch(s -> !s.isEmpty() && BucketLibUtil.getBlock(s) == Blocks.POWDER_SNOW), "powder snow bucket is not in the inventory");
        helper.succeed();
    }

    public static void creativePickupKeepsTheEmptyBucket(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.POWDER_SNOW);
        ServerPlayer player = TestUtils.player(helper, GameType.CREATIVE, TestUtils.bucket(TestBuckets.TEST_BUCKET));
        TestUtils.lookDownAt(helper, player, TARGET);
        TestUtils.use(helper, player);
        helper.assertBlockPresent(Blocks.AIR, TARGET);
        helper.assertTrue(BucketLibUtil.isEmpty(player.getMainHandItem()), "the empty bucket in the hand was filled in creative mode");
        helper.assertTrue(player.getInventory().getNonEquipmentItems().stream().anyMatch(s -> !s.isEmpty() && BucketLibUtil.getBlock(s) == Blocks.POWDER_SNOW), "powder snow bucket is not in the inventory");
        helper.succeed();
    }

}
