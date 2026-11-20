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

}
