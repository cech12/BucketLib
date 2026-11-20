package de.cech12.bucketlib.gametest.tests;

import de.cech12.bucketlib.gametest.BucketLibGameTests;
import de.cech12.bucketlib.gametest.TestBuckets;
import de.cech12.bucketlib.gametest.TestUtils;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/**
 * Picking up and placing fluids in the world like a player with a vanilla bucket.
 */
public final class FluidTests {

    /** fluid source position, the player looks down at it */
    private static final BlockPos SOURCE = TestUtils.CENTER;
    /** floor below the center, placing at it puts the fluid at {@link #SOURCE} */
    private static final BlockPos FLOOR = TestUtils.CENTER.below();

    private FluidTests() {}

    public static void register(BucketLibGameTests.TestCollector tests) {
        tests.add("fluid/picks_up_water", h -> picksUp(h, Fluids.WATER));
        tests.add("fluid/picks_up_lava", h -> picksUp(h, Fluids.LAVA));
        tests.add("fluid/picks_up_one_of_stacked_buckets", FluidTests::picksUpOneOfStackedBuckets);
        tests.add("fluid/does_not_pick_up_denied_fluid", FluidTests::doesNotPickUpDeniedFluid);
        tests.add("fluid/places_water", h -> places(h, Fluids.WATER));
        tests.add("fluid/places_lava", h -> places(h, Fluids.LAVA));
        tests.add("fluid/creative_mode_keeps_the_bucket", FluidTests::creativeModeKeepsTheBucket);
        tests.add("fluid/waterlogs_blocks", FluidTests::waterlogsBlocks);
        tests.add("fluid/picks_up_water_of_waterlogged_blocks", FluidTests::picksUpWaterOfWaterloggedBlocks);
        tests.add("fluid/replaces_replaceable_blocks", FluidTests::replacesReplaceableBlocks);
        tests.add("fluid/water_evaporates_in_the_nether", FluidTests::waterEvaporatesInTheNether);
        tests.add("fluid/cracking_buckets_break_when_emptied", FluidTests::crackingBucketsBreakWhenEmptied);
        tests.add("fluid/does_nothing_without_target", FluidTests::doesNothingWithoutTarget);
    }

    private static ServerPlayer lookingAtSource(GameTestHelper helper, GameType gameType, ItemStack stack) {
        ServerPlayer player = TestUtils.player(helper, gameType, stack);
        TestUtils.lookDownAt(helper, player, SOURCE);
        return player;
    }

    private static ServerPlayer lookingAtFloor(GameTestHelper helper, GameType gameType, ItemStack stack) {
        ServerPlayer player = TestUtils.player(helper, gameType, stack);
        TestUtils.lookDownAt(helper, player, FLOOR);
        return player;
    }

    /** An empty bucket picks up a fluid source like a vanilla bucket. */
    private static void picksUp(GameTestHelper helper, Fluid fluid) {
        helper.setBlock(SOURCE, fluid.defaultFluidState().createLegacyBlock());
        ServerPlayer player = lookingAtSource(helper, GameType.SURVIVAL, TestUtils.bucket(TestBuckets.TEST_BUCKET));
        TestUtils.use(helper, player);
        ItemStack hand = player.getMainHandItem();
        helper.assertTrue(hand.is(TestBuckets.TEST_BUCKET.item()), "bucket was replaced by " + hand);
        TestUtils.assertEquals(helper, BucketLibUtil.getFluid(hand), fluid, "fluid in the bucket");
        helper.assertBlockPresent(Blocks.AIR, SOURCE);
        helper.assertTrue(player.getStats().getValue(Stats.ITEM_USED.get(TestBuckets.TEST_BUCKET.item())) <= 1, "used statistic is counted more than once");
        helper.succeed();
    }

    /** From a stack of empty buckets one is filled and put into the inventory, the rest stays in the hand. */
    public static void picksUpOneOfStackedBuckets(GameTestHelper helper) {
        helper.setBlock(SOURCE, Blocks.WATER);
        ServerPlayer player = lookingAtSource(helper, GameType.SURVIVAL, TestUtils.bucket(TestBuckets.TEST_BUCKET).copyWithCount(3));
        TestUtils.use(helper, player);
        ItemStack hand = player.getMainHandItem();
        helper.assertTrue(hand.is(TestBuckets.TEST_BUCKET.item()) && BucketLibUtil.isEmpty(hand) && hand.getCount() == 2, "hand does not hold 2 empty buckets: " + TestUtils.describe(hand));
        boolean filledInInventory = player.getInventory().getNonEquipmentItems().stream()
                .anyMatch(stack -> stack.is(TestBuckets.TEST_BUCKET.item()) && BucketLibUtil.getFluid(stack) == Fluids.WATER && stack.getCount() == 1);
        helper.assertTrue(filledInInventory, "filled bucket is not in the inventory");
        helper.succeed();
    }

    /** A bucket does not pick up a fluid it cannot hold, the source stays. */
    public static void doesNotPickUpDeniedFluid(GameTestHelper helper) {
        helper.setBlock(SOURCE, Blocks.LAVA);
        ServerPlayer player = lookingAtSource(helper, GameType.SURVIVAL, TestUtils.bucket(TestBuckets.TEMPERATURE_BUCKET));
        TestUtils.use(helper, player);
        helper.assertTrue(BucketLibUtil.isEmpty(player.getMainHandItem()), "temperature bucket picked up lava");
        helper.assertBlockPresent(Blocks.LAVA, SOURCE);
        helper.succeed();
    }

    /** A filled bucket places its fluid like a vanilla bucket and becomes empty. */
    private static void places(GameTestHelper helper, Fluid fluid) {
        ServerPlayer player = lookingAtFloor(helper, GameType.SURVIVAL, TestUtils.withFluid(TestBuckets.TEST_BUCKET, fluid));
        TestUtils.use(helper, player);
        ItemStack hand = player.getMainHandItem();
        helper.assertTrue(hand.is(TestBuckets.TEST_BUCKET.item()) && BucketLibUtil.isEmpty(hand), "bucket is not empty: " + TestUtils.describe(hand));
        helper.assertTrue(helper.getBlockState(SOURCE).getFluidState().isSourceOfType(fluid), "no " + fluid + " source was placed: " + helper.getBlockState(SOURCE));
        helper.succeed();
    }

    /** In creative mode the bucket keeps its content when placing, and keeps empty in the hand when picking up. */
    public static void creativeModeKeepsTheBucket(GameTestHelper helper) {
        ServerPlayer placing = lookingAtFloor(helper, GameType.CREATIVE, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER));
        TestUtils.use(helper, placing);
        TestUtils.assertEquals(helper, BucketLibUtil.getFluid(placing.getMainHandItem()), Fluids.WATER, "fluid in the bucket after placing in creative mode");
        helper.assertTrue(helper.getBlockState(SOURCE).getFluidState().isSource(), "no water was placed in creative mode");
        ServerPlayer pickingUp = lookingAtSource(helper, GameType.CREATIVE, TestUtils.bucket(TestBuckets.TEST_BUCKET));
        TestUtils.use(helper, pickingUp);
        helper.assertTrue(BucketLibUtil.isEmpty(pickingUp.getMainHandItem()), "the empty bucket in the hand was filled in creative mode");
        boolean filledInInventory = pickingUp.getInventory().getNonEquipmentItems().stream()
                .anyMatch(stack -> stack.is(TestBuckets.TEST_BUCKET.item()) && BucketLibUtil.getFluid(stack) == Fluids.WATER);
        helper.assertTrue(filledInInventory, "filled bucket is not in the inventory in creative mode");
        helper.succeed();
    }

    /** Placing water at a waterloggable block waterlogs it like a vanilla bucket. */
    public static void waterlogsBlocks(GameTestHelper helper) {
        helper.setBlock(SOURCE, Blocks.OAK_SLAB.defaultBlockState());
        ServerPlayer player = lookingAtSource(helper, GameType.SURVIVAL, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER));
        TestUtils.use(helper, player);
        helper.assertBlockProperty(SOURCE, SlabBlock.WATERLOGGED, true);
        helper.assertTrue(BucketLibUtil.isEmpty(player.getMainHandItem()), "bucket is not empty after waterlogging");
        helper.succeed();
    }

    /** An empty bucket takes the water of a waterlogged block. */
    public static void picksUpWaterOfWaterloggedBlocks(GameTestHelper helper) {
        helper.setBlock(SOURCE, Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
        ServerPlayer player = lookingAtSource(helper, GameType.SURVIVAL, TestUtils.bucket(TestBuckets.TEST_BUCKET));
        TestUtils.use(helper, player);
        helper.assertBlockProperty(SOURCE, SlabBlock.WATERLOGGED, false);
        TestUtils.assertEquals(helper, BucketLibUtil.getFluid(player.getMainHandItem()), Fluids.WATER, "fluid after picking up from a waterlogged block");
        helper.succeed();
    }

    /** A fluid replaces blocks like short grass. */
    public static void replacesReplaceableBlocks(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.DIRT);
        helper.setBlock(SOURCE, Blocks.SHORT_GRASS);
        ServerPlayer player = lookingAtSource(helper, GameType.SURVIVAL, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER));
        TestUtils.use(helper, player);
        helper.assertTrue(helper.getBlockState(SOURCE).getFluidState().isSourceOfType(Fluids.WATER), "grass was not replaced by water: " + helper.getBlockState(SOURCE));
        helper.succeed();
    }

    /** Like a vanilla water bucket, water evaporates in dimensions where water evaporates (nether) and the bucket gets empty. */
    public static void waterEvaporatesInTheNether(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "test setup broken: no nether");
        BlockPos floor = new BlockPos(0, 120, 0);
        nether.getChunk(floor);
        var previousFloor = nether.getBlockState(floor);
        var previousTarget = nether.getBlockState(floor.above());
        var previousAir = nether.getBlockState(floor.above(2));
        try {
            nether.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
            nether.setBlockAndUpdate(floor.above(), Blocks.AIR.defaultBlockState());
            nether.setBlockAndUpdate(floor.above(2), Blocks.AIR.defaultBlockState());
            ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER));
            player.setServerLevel(nether);
            Vec3 pos = Vec3.atBottomCenterOf(floor.above());
            player.snapTo(pos.x, pos.y, pos.z, 0, 90);
            player.gameMode.useItem(player, nether, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND);
            helper.assertTrue(BucketLibUtil.isEmpty(player.getMainHandItem()), "bucket is not empty after using water in the nether");
            helper.assertTrue(nether.getBlockState(floor.above()).getFluidState().isEmpty(), "water was placed in the nether");
        } finally {
            nether.setBlockAndUpdate(floor.above(2), previousAir);
            nether.setBlockAndUpdate(floor.above(), previousTarget);
            nether.setBlockAndUpdate(floor, previousFloor);
        }
        helper.succeed();
    }

    /**
     * Buckets break (are used up) when they are emptied after holding a fluid that cracks them: above the upper
     * cracking temperature, below the lower cracking temperature or a listed cracking fluid. Breaking counts as
     * broken item.
     */
    public static void crackingBucketsBreakWhenEmptied(GameTestHelper helper) {
        record Case(TestBuckets.Definition bucket, Fluid fluid) {}
        java.util.List<Case> cases = java.util.List.of(
                new Case(TestBuckets.CRACKING_BUCKET, Fluids.LAVA),
                new Case(TestBuckets.LOWER_CRACKING_BUCKET, Fluids.WATER),
                new Case(TestBuckets.CRACKING_FLUIDS_BUCKET, Fluids.WATER),
                new Case(TestBuckets.CRACKING_FLUIDS_TAG_BUCKET, Fluids.WATER));
        for (Case testCase : cases) {
            ItemStack stack = TestUtils.withFluid(testCase.bucket(), testCase.fluid());
            helper.assertTrue(testCase.bucket().item().isCracked(stack), testCase.bucket().name() + " with " + testCase.fluid() + " is not cracked");
            helper.setBlock(SOURCE, Blocks.AIR);
            ServerPlayer player = lookingAtFloor(helper, GameType.SURVIVAL, stack);
            // a silent player plays no break sound, but the bucket breaks anyway
            player.setSilent(testCase.bucket() == TestBuckets.LOWER_CRACKING_BUCKET);
            TestUtils.use(helper, player);
            helper.assertTrue(player.getMainHandItem().isEmpty(), testCase.bucket().name() + " did not break: " + TestUtils.describe(player.getMainHandItem()));
            TestUtils.assertEquals(helper, player.getStats().getValue(Stats.ITEM_BROKEN.get(testCase.bucket().item())), 1, "broken statistic of " + testCase.bucket().name());
            helper.assertTrue(helper.getBlockState(SOURCE).getFluidState().isSourceOfType(testCase.fluid()), "fluid of " + testCase.bucket().name() + " was not placed");
        }
        // not cracking contents
        helper.assertFalse(TestBuckets.CRACKING_BUCKET.item().isCracked(TestUtils.withFluid(TestBuckets.CRACKING_BUCKET, Fluids.WATER)), "cracking bucket with water is cracked");
        helper.assertFalse(TestBuckets.LOWER_CRACKING_BUCKET.item().isCracked(TestUtils.withFluid(TestBuckets.LOWER_CRACKING_BUCKET, Fluids.LAVA)), "lower cracking bucket with lava is cracked");
        helper.assertFalse(TestBuckets.CRACKING_BUCKET.item().isCracked(TestUtils.bucket(TestBuckets.CRACKING_BUCKET)), "empty cracking bucket is cracked");
        helper.succeed();
    }

    /** Using a bucket into the air does nothing (empty and filled). */
    public static void doesNothingWithoutTarget(GameTestHelper helper) {
        for (ItemStack stack : java.util.List.of(TestUtils.bucket(TestBuckets.TEST_BUCKET), TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER),
                TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW), TestUtils.withEntity(TestBuckets.TEST_BUCKET, null, net.minecraft.world.entity.EntityTypes.SULFUR_CUBE))) {
            ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, stack.copy());
            Vec3 pos = helper.absoluteVec(Vec3.atBottomCenterOf(TestUtils.CENTER.above(40)));
            player.snapTo(pos.x, pos.y, pos.z, 0, -90); // looks up into the empty sky high above the test area
            var result = TestUtils.use(helper, player);
            helper.assertFalse(result.consumesAction(), "using " + TestUtils.describe(stack) + " into the air consumed the action");
            helper.assertTrue(ItemStack.isSameItemSameComponents(player.getMainHandItem(), stack), "using into the air changed " + TestUtils.describe(stack));
        }
        helper.succeed();
    }

}
