package de.cech12.bucketlib.gametest.tests;

import de.cech12.bucketlib.gametest.BucketLibGameTests;
import de.cech12.bucketlib.gametest.TestBuckets;
import de.cech12.bucketlib.gametest.TestUtils;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.WorldInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Filling buckets from cauldrons and emptying buckets into cauldrons like vanilla buckets.
 */
public final class CauldronTests {

    private static final BlockPos CAULDRON = TestUtils.CENTER;

    private CauldronTests() {}

    public static void register(BucketLibGameTests.TestCollector tests) {
        tests.add("cauldron/fills_bucket_with_water", h -> fills(h, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), TestBuckets.TEST_BUCKET, true));
        tests.add("cauldron/fills_bucket_with_lava", h -> fills(h, Blocks.LAVA_CAULDRON.defaultBlockState(), TestBuckets.TEST_BUCKET, true));
        tests.add("cauldron/fills_bucket_with_powder_snow", h -> fills(h, Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), TestBuckets.TEST_BUCKET, true));
        tests.add("cauldron/does_not_fill_from_partly_filled_cauldron", h -> fills(h, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2), TestBuckets.TEST_BUCKET, false));
        tests.add("cauldron/does_not_fill_denied_lava", h -> fills(h, Blocks.LAVA_CAULDRON.defaultBlockState(), TestBuckets.TEMPERATURE_BUCKET, false));
        tests.add("cauldron/does_not_fill_denied_water", h -> fills(h, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), TestBuckets.WATER_DENYING_BUCKET, false));
        tests.add("cauldron/does_not_fill_denied_powder_snow", h -> fills(h, Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), TestBuckets.NO_BLOCKS_BUCKET, false));
        tests.add("cauldron/empties_water", h -> empties(h, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER), Blocks.WATER_CAULDRON));
        tests.add("cauldron/empties_lava", h -> empties(h, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.LAVA), Blocks.LAVA_CAULDRON));
        tests.add("cauldron/empties_powder_snow", h -> empties(h, TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW), Blocks.POWDER_SNOW_CAULDRON));
        tests.add("cauldron/creative_mode_keeps_the_bucket", CauldronTests::creativeModeKeepsTheBucket);
        tests.add("cauldron/entity_bucket_is_not_emptied_into_cauldron", CauldronTests::entityBucketIsNotEmptiedIntoCauldron);
        tests.add("cauldron/other_blocks_are_ignored", CauldronTests::otherBlocksAreIgnored);
    }

    private static ServerPlayer lookingAtCauldron(GameTestHelper helper, GameType gameType, ItemStack stack) {
        ServerPlayer player = TestUtils.player(helper, gameType, stack);
        TestUtils.lookDownAt(helper, player, CAULDRON);
        return player;
    }

    private static void fills(GameTestHelper helper, BlockState cauldron, TestBuckets.Definition bucket, boolean expected) {
        helper.setBlock(CAULDRON, cauldron);
        ServerPlayer player = lookingAtCauldron(helper, GameType.SURVIVAL, TestUtils.bucket(bucket));
        TestUtils.use(helper, player);
        ItemStack hand = player.getMainHandItem();
        if (expected) {
            helper.assertBlockPresent(Blocks.CAULDRON, CAULDRON);
            helper.assertTrue(hand.is(bucket.item()) && !BucketLibUtil.isEmpty(hand), "bucket was not filled: " + TestUtils.describe(hand));
            if (cauldron.is(Blocks.WATER_CAULDRON)) {
                TestUtils.assertEquals(helper, BucketLibUtil.getFluid(hand), Fluids.WATER, "fluid from the water cauldron");
            } else if (cauldron.is(Blocks.LAVA_CAULDRON)) {
                TestUtils.assertEquals(helper, BucketLibUtil.getFluid(hand), Fluids.LAVA, "fluid from the lava cauldron");
            } else {
                TestUtils.assertEquals(helper, BucketLibUtil.getBlock(hand), Blocks.POWDER_SNOW, "block from the powder snow cauldron");
            }
        } else {
            helper.assertTrue(helper.getBlockState(CAULDRON) == cauldron, "cauldron changed: " + helper.getBlockState(CAULDRON));
            helper.assertTrue(BucketLibUtil.isEmpty(hand), "bucket was filled: " + TestUtils.describe(hand));
        }
        helper.succeed();
    }

    private static void empties(GameTestHelper helper, ItemStack stack, Block expectedCauldron) {
        helper.setBlock(CAULDRON, Blocks.CAULDRON);
        ServerPlayer player = lookingAtCauldron(helper, GameType.SURVIVAL, stack);
        TestUtils.use(helper, player);
        helper.assertBlockPresent(expectedCauldron, CAULDRON);
        if (helper.getBlockState(CAULDRON).hasProperty(LayeredCauldronBlock.LEVEL)) {
            helper.assertBlockProperty(CAULDRON, LayeredCauldronBlock.LEVEL, 3);
        }
        ItemStack hand = player.getMainHandItem();
        helper.assertTrue(hand.is(stack.getItem()) && BucketLibUtil.isEmpty(hand), "bucket was not emptied: " + TestUtils.describe(hand));
        helper.succeed();
    }

    /**
     * Like vanilla buckets in creative mode: emptying keeps the filled bucket in the hand and adds an empty bucket to
     * the inventory, filling keeps the empty bucket and adds a filled one.
     */
    public static void creativeModeKeepsTheBucket(GameTestHelper helper) {
        helper.setBlock(CAULDRON, Blocks.CAULDRON);
        ServerPlayer emptying = lookingAtCauldron(helper, GameType.CREATIVE, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER));
        TestUtils.use(helper, emptying);
        helper.assertBlockPresent(Blocks.WATER_CAULDRON, CAULDRON);
        TestUtils.assertEquals(helper, BucketLibUtil.getFluid(emptying.getMainHandItem()), Fluids.WATER, "fluid in the hand after emptying in creative mode");
        helper.assertTrue(emptying.getInventory().getNonEquipmentItems().stream().anyMatch(s -> s.is(TestBuckets.TEST_BUCKET.item()) && BucketLibUtil.isEmpty(s)), "no empty bucket was added to the inventory");
        helper.assertTrue(emptying.getAbilities().instabuild, "creative abilities were not restored");
        ServerPlayer filling = lookingAtCauldron(helper, GameType.CREATIVE, TestUtils.bucket(TestBuckets.TEST_BUCKET));
        TestUtils.use(helper, filling);
        helper.assertBlockPresent(Blocks.CAULDRON, CAULDRON);
        helper.assertTrue(BucketLibUtil.isEmpty(filling.getMainHandItem()), "the empty bucket in the hand was filled in creative mode");
        helper.assertTrue(filling.getInventory().getNonEquipmentItems().stream().anyMatch(s -> s.is(TestBuckets.TEST_BUCKET.item()) && BucketLibUtil.getFluid(s) == Fluids.WATER), "no filled bucket was added to the inventory");
        helper.succeed();
    }

    /**
     * Like a vanilla fish bucket, a bucket with an entity is not emptied into the cauldron: the water and the fish are
     * placed in front of the cauldron (above it).
     */
    public static void entityBucketIsNotEmptiedIntoCauldron(GameTestHelper helper) {
        helper.setBlock(CAULDRON, Blocks.CAULDRON);
        ServerPlayer player = lookingAtCauldron(helper, GameType.SURVIVAL, TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.SALMON));
        TestUtils.use(helper, player);
        helper.assertBlockPresent(Blocks.CAULDRON, CAULDRON);
        helper.assertEntityPresent(EntityTypes.SALMON);
        helper.assertTrue(BucketLibUtil.isEmpty(player.getMainHandItem()), "salmon bucket was not emptied: " + TestUtils.describe(player.getMainHandItem()));
        helper.succeed();
    }

    /** The cauldron helpers ignore other blocks, non BucketLib items and the client side. */
    public static void otherBlocksAreIgnored(GameTestHelper helper) {
        helper.setBlock(CAULDRON, Blocks.STONE);
        ServerPlayer player = lookingAtCauldron(helper, GameType.SURVIVAL, TestUtils.bucket(TestBuckets.TEST_BUCKET));
        BlockHitResult hit = new BlockHitResult(helper.absoluteVec(Vec3.atCenterOf(CAULDRON)), Direction.UP, helper.absolutePos(CAULDRON), false);
        helper.assertFalse(WorldInteractionUtil.tryPickupFromCauldron(helper.getLevel(), player, InteractionHand.MAIN_HAND, hit).consumesAction(), "stone was used as cauldron");
        helper.assertFalse(WorldInteractionUtil.tryPlaceIntoCauldron(helper.getLevel(), player, InteractionHand.MAIN_HAND, hit).consumesAction(), "stone was used as cauldron");
        helper.setBlock(CAULDRON, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.STONE));
        helper.assertFalse(WorldInteractionUtil.tryPickupFromCauldron(helper.getLevel(), player, InteractionHand.MAIN_HAND, hit).consumesAction(), "stone filled from a cauldron");
        player.setItemInHand(InteractionHand.MAIN_HAND, TestUtils.withMilk(TestBuckets.TEST_BUCKET));
        helper.setBlock(CAULDRON, Blocks.CAULDRON);
        helper.assertFalse(WorldInteractionUtil.tryPlaceIntoCauldron(helper.getLevel(), player, InteractionHand.MAIN_HAND, hit).consumesAction(), "milk was emptied into a cauldron");
        helper.assertBlockPresent(Blocks.CAULDRON, CAULDRON);
        helper.succeed();
    }

}
