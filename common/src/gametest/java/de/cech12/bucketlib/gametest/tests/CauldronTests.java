package de.cech12.bucketlib.gametest.tests;

import de.cech12.bucketlib.gametest.BucketLibGameTests;
import de.cech12.bucketlib.gametest.KnownIssues;
import de.cech12.bucketlib.gametest.Platforms;
import de.cech12.bucketlib.gametest.TestBuckets;
import de.cech12.bucketlib.gametest.TestUtils;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.WorldInteractionUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    private static final BlockPos VANILLA = new BlockPos(1, 1, 2);
    private static final BlockPos BUCKETLIB = new BlockPos(3, 1, 2);

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
        tests.add("cauldron/behaves_like_vanilla_buckets", CauldronTests::behavesLikeVanillaBuckets);
        tests.add("cauldron/fills_one_of_stacked_buckets", CauldronTests::fillsOneOfStackedBuckets);
        tests.add("cauldron/respects_allowed_and_denied_fluids", CauldronTests::respectsAllowedAndDeniedFluids);
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

    // --- comparison with vanilla buckets ---------------------------------------------------------------------------------------------------

    private enum Content {
        EMPTY(Items.BUCKET), WATER(Items.WATER_BUCKET), LAVA(Items.LAVA_BUCKET), POWDER_SNOW(Items.POWDER_SNOW_BUCKET);

        private final Item vanillaBucket;

        Content(Item vanillaBucket) {
            this.vanillaBucket = vanillaBucket;
        }

        ItemStack bucketLib() {
            return switch (this) {
                case EMPTY -> TestUtils.bucket(TestBuckets.TEST_BUCKET);
                case WATER -> TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER);
                case LAVA -> TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.LAVA);
                case POWDER_SNOW -> TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW);
            };
        }

        static Content ofVanilla(ItemStack stack) {
            for (Content content : values()) {
                if (stack.is(content.vanillaBucket)) {
                    return content;
                }
            }
            return null;
        }

        static Content ofBucketLib(ItemStack stack) {
            if (!stack.is(TestBuckets.TEST_BUCKET.item())) {
                return null;
            }
            if (BucketLibUtil.getBlock(stack) == Blocks.POWDER_SNOW) {
                return POWDER_SNOW;
            }
            if (BucketLibUtil.getFluid(stack) == Fluids.WATER) {
                return WATER;
            }
            if (BucketLibUtil.getFluid(stack) == Fluids.LAVA) {
                return LAVA;
            }
            return BucketLibUtil.isEmpty(stack) ? EMPTY : null;
        }
    }

    private record Outcome(BlockState cauldron, BlockState above, Content hand) {}

    private static Outcome useOn(GameTestHelper helper, BlockPos pos, BlockState cauldron, ItemStack stack, boolean vanilla) {
        helper.setBlock(pos.above(), Blocks.AIR);
        helper.setBlock(pos, cauldron);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, stack);
        TestUtils.lookDownAt(helper, player, pos);
        // players use items on blocks with useItemOn first (vanilla cauldron interactions), then use (BucketLib)
        var hit = new net.minecraft.world.phys.BlockHitResult(helper.absoluteVec(net.minecraft.world.phys.Vec3.atCenterOf(pos).add(0, 0.5, 0)), Direction.UP, helper.absolutePos(pos), false);
        var result = player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        if (!result.consumesAction()) {
            TestUtils.use(helper, player);
        }
        ItemStack hand = player.getMainHandItem();
        Outcome outcome = new Outcome(helper.getBlockState(pos), helper.getBlockState(pos.above()), vanilla ? Content.ofVanilla(hand) : Content.ofBucketLib(hand));
        helper.setBlock(pos.above(), Blocks.AIR);
        helper.setBlock(pos, Blocks.AIR);
        return outcome;
    }

    /**
     * Every bucket content on every cauldron (empty, partly and fully filled water/powder snow, lava) has the same
     * result as with the vanilla bucket: same cauldron afterward, same block above, same content of the held bucket.
     */
    public static void behavesLikeVanillaBuckets(GameTestHelper helper) {
        List<BlockState> cauldrons = new ArrayList<>(List.of(Blocks.CAULDRON.defaultBlockState(), Blocks.LAVA_CAULDRON.defaultBlockState()));
        for (int level = 1; level <= 3; level++) {
            cauldrons.add(Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, level));
            cauldrons.add(Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, level));
        }
        List<String> differences = new ArrayList<>();
        List<String> knownDifferences = new ArrayList<>();
        for (BlockState cauldron : cauldrons) {
            for (Content content : Content.values()) {
                Outcome vanilla = useOn(helper, VANILLA, cauldron, new ItemStack(content.vanillaBucket), true);
                Outcome bucketLib = TestUtils.noException(helper, content + " on " + cauldron, () -> useOn(helper, BUCKETLIB, cauldron, content.bucketLib(), false));
                if (!vanilla.equals(bucketLib)) {
                    // KI-21: a filled bucket on a cauldron that is not empty places its content above the cauldron
                    boolean known = content != Content.EMPTY && !cauldron.is(Blocks.CAULDRON);
                    (known ? knownDifferences : differences).add(content + " on " + cauldron + ": vanilla " + vanilla + ", BucketLib " + bucketLib);
                }
            }
        }
        helper.assertTrue(differences.isEmpty(), differences.size() + " differences to vanilla buckets: " + differences);
        KnownIssues.run(helper, "KI-21 filled buckets are not emptied into cauldrons that are not empty like vanilla buckets", Platforms.ALL, () ->
                helper.assertTrue(knownDifferences.isEmpty(), knownDifferences.size() + " differences to vanilla buckets: " + knownDifferences));
    }

    /** From a stack of empty buckets one is filled from a cauldron and put into the inventory. */
    public static void fillsOneOfStackedBuckets(GameTestHelper helper) {
        helper.setBlock(CAULDRON, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.bucket(TestBuckets.TEST_BUCKET).copyWithCount(2));
        TestUtils.lookDownAt(helper, player, CAULDRON);
        TestUtils.use(helper, player);
        helper.assertBlockPresent(Blocks.CAULDRON, CAULDRON);
        ItemStack hand = player.getMainHandItem();
        helper.assertTrue(BucketLibUtil.isEmpty(hand) && hand.getCount() == 1, "hand does not hold 1 empty bucket: " + TestUtils.describe(hand));
        helper.assertTrue(player.getInventory().getNonEquipmentItems().stream().anyMatch(s -> !s.isEmpty() && BucketLibUtil.getFluid(s) == Fluids.WATER), "water bucket is not in the inventory");
        helper.succeed();
    }

    /** Allowed and denied fluid lists and tags also apply to cauldrons. */
    public static void respectsAllowedAndDeniedFluids(GameTestHelper helper) {
        for (var bucket : List.of(TestBuckets.WATER_ALLOWING_BUCKET, TestBuckets.WATER_ALLOWING_TAG_BUCKET, TestBuckets.WATER_DENYING_BUCKET, TestBuckets.WATER_DENYING_TAG_BUCKET)) {
            boolean allowed = bucket.item().canHoldFluid(Fluids.WATER);
            helper.setBlock(CAULDRON, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
            ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.bucket(bucket));
            TestUtils.lookDownAt(helper, player, CAULDRON);
            TestUtils.use(helper, player);
            TestUtils.assertEquals(helper, BucketLibUtil.getFluid(player.getMainHandItem()) == Fluids.WATER, allowed, bucket.name() + " was filled from the water cauldron");
            TestUtils.assertEquals(helper, helper.getBlockState(CAULDRON).is(Blocks.CAULDRON), allowed, "cauldron was emptied by " + bucket.name());
        }
        helper.succeed();
    }

}
