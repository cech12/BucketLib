package de.cech12.bucketlib.gametest.tests;

import de.cech12.bucketlib.gametest.BucketLibGameTests;
import de.cech12.bucketlib.gametest.KnownIssues;
import de.cech12.bucketlib.gametest.Platforms;
import de.cech12.bucketlib.gametest.TestBuckets;
import de.cech12.bucketlib.gametest.TestUtils;
import de.cech12.bucketlib.platform.Services;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Cases of the old test suites (Ceramic Bucket and the former Forge tests of BucketLib) that the feature tests did not
 * cover yet: cauldrons like vanilla, dispensers with lava, breaking and waterlogging, filled buckets and entities,
 * inclusive temperature limits, Infinity with entities, stacked and creative variants. The test names use the prefix
 * of their feature.
 */
public final class LegacySuiteTests {

    private static final BlockPos VANILLA = new BlockPos(1, 1, 2);
    private static final BlockPos BUCKETLIB = new BlockPos(3, 1, 2);
    private static final BlockPos DISPENSER = new BlockPos(2, 1, 1);
    private static final BlockPos TARGET = new BlockPos(2, 1, 2);

    private LegacySuiteTests() {}

    public static void register(BucketLibGameTests.TestCollector tests) {
        tests.add("cauldron/behaves_like_vanilla_buckets", LegacySuiteTests::cauldronBehavesLikeVanillaBuckets);
        tests.add("cauldron/fills_one_of_stacked_buckets", LegacySuiteTests::cauldronFillsOneOfStackedBuckets);
        tests.add("cauldron/respects_allowed_and_denied_fluids", LegacySuiteTests::cauldronRespectsAllowedAndDeniedFluids);
        tests.add("dispenser/picks_up_and_places_lava", LegacySuiteTests::dispenserPicksUpAndPlacesLava);
        tests.add("dispenser/cracking_bucket_breaks", LegacySuiteTests::dispenserCrackingBucketBreaks);
        tests.add("dispenser/damages_durability_bucket", LegacySuiteTests::dispenserDamagesDurabilityBucket);
        tests.add("dispenser/waterlogs_blocks", LegacySuiteTests::dispenserWaterlogsBlocks);
        tests.add("dispenser/picks_up_water_of_waterlogged_blocks", LegacySuiteTests::dispenserPicksUpWaterOfWaterloggedBlocks);
        tests.add("entity/filled_buckets_do_not_pick_up_entities", LegacySuiteTests::filledBucketsDoNotPickUpEntities);
        tests.add("entity/creative_pickup_keeps_the_water_bucket", LegacySuiteTests::creativePickupKeepsTheWaterBucket);
        tests.add("entity/anti_salmon_bucket_picks_up_cod", LegacySuiteTests::antiSalmonBucketPicksUpCod);
        tests.add("properties/temperature_limits_are_inclusive", LegacySuiteTests::temperatureLimitsAreInclusive);
        tests.add("infinity/enabled/entity_buckets_are_not_enchantable", LegacySuiteTests::entityBucketsAreNotEnchantable);
        tests.add("block/picks_up_one_of_stacked_buckets", LegacySuiteTests::blockPicksUpOneOfStackedBuckets);
        tests.add("block/creative_pickup_keeps_the_empty_bucket", LegacySuiteTests::blockCreativePickupKeepsTheEmptyBucket);
        tests.add("content/stacks_compare_by_content", LegacySuiteTests::stacksCompareByContent);
    }

    // --- cauldrons ---------------------------------------------------------------------------------------------------

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

    private static Outcome use(GameTestHelper helper, BlockPos pos, BlockState cauldron, ItemStack stack, boolean vanilla) {
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
    public static void cauldronBehavesLikeVanillaBuckets(GameTestHelper helper) {
        List<BlockState> cauldrons = new ArrayList<>(List.of(Blocks.CAULDRON.defaultBlockState(), Blocks.LAVA_CAULDRON.defaultBlockState()));
        for (int level = 1; level <= 3; level++) {
            cauldrons.add(Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, level));
            cauldrons.add(Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, level));
        }
        List<String> differences = new ArrayList<>();
        for (BlockState cauldron : cauldrons) {
            for (Content content : Content.values()) {
                Outcome vanilla = use(helper, VANILLA, cauldron, new ItemStack(content.vanillaBucket), true);
                Outcome bucketLib = TestUtils.noException(helper, content + " on " + cauldron, () -> use(helper, BUCKETLIB, cauldron, content.bucketLib(), false));
                if (!vanilla.equals(bucketLib)) {
                    differences.add(content + " on " + cauldron + ": vanilla " + vanilla + ", BucketLib " + bucketLib);
                }
            }
        }
        KnownIssues.run(helper, "KI-21 buckets are not emptied into partly filled cauldrons like vanilla buckets", Platforms.ALL, () ->
                helper.assertTrue(differences.isEmpty(), differences.size() + " differences to vanilla buckets: " + differences));
    }

    /** From a stack of empty buckets one is filled from a cauldron and put into the inventory. */
    public static void cauldronFillsOneOfStackedBuckets(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.bucket(TestBuckets.TEST_BUCKET).copyWithCount(2));
        TestUtils.lookDownAt(helper, player, TARGET);
        TestUtils.use(helper, player);
        helper.assertBlockPresent(Blocks.CAULDRON, TARGET);
        ItemStack hand = player.getMainHandItem();
        helper.assertTrue(BucketLibUtil.isEmpty(hand) && hand.getCount() == 1, "hand does not hold 1 empty bucket: " + TestUtils.describe(hand));
        helper.assertTrue(player.getInventory().getNonEquipmentItems().stream().anyMatch(s -> !s.isEmpty() && BucketLibUtil.getFluid(s) == Fluids.WATER), "water bucket is not in the inventory");
        helper.succeed();
    }

    /** Allowed and denied fluid lists and tags also apply to cauldrons. */
    public static void cauldronRespectsAllowedAndDeniedFluids(GameTestHelper helper) {
        for (var bucket : List.of(TestBuckets.WATER_ALLOWING_BUCKET, TestBuckets.WATER_ALLOWING_TAG_BUCKET, TestBuckets.WATER_DENYING_BUCKET, TestBuckets.WATER_DENYING_TAG_BUCKET)) {
            boolean allowed = bucket.item().canHoldFluid(Fluids.WATER);
            helper.setBlock(TARGET, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
            ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.bucket(bucket));
            TestUtils.lookDownAt(helper, player, TARGET);
            TestUtils.use(helper, player);
            TestUtils.assertEquals(helper, BucketLibUtil.getFluid(player.getMainHandItem()) == Fluids.WATER, allowed, bucket.name() + " was filled from the water cauldron");
            TestUtils.assertEquals(helper, helper.getBlockState(TARGET).is(Blocks.CAULDRON), allowed, "cauldron was emptied by " + bucket.name());
        }
        helper.succeed();
    }

    // --- dispensers --------------------------------------------------------------------------------------------------

    private static DispenserBlockEntity dispense(GameTestHelper helper, ItemStack stack) {
        helper.setBlock(DISPENSER, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.SOUTH));
        DispenserBlockEntity dispenser = helper.getBlockEntity(DISPENSER, DispenserBlockEntity.class);
        dispenser.setItem(0, stack);
        helper.setBlock(DISPENSER.west(), Blocks.REDSTONE_BLOCK);
        return dispenser;
    }

    public static void dispenserPicksUpAndPlacesLava(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.LAVA);
        DispenserBlockEntity dispenser = dispense(helper, TestUtils.bucket(TestBuckets.TEST_BUCKET));
        helper.startSequence()
                .thenWaitUntil(() -> TestUtils.assertEquals(helper, BucketLibUtil.getFluid(dispenser.getItem(0)), Fluids.LAVA, "fluid in the dispenser bucket"))
                .thenExecute(() -> helper.assertBlockPresent(Blocks.AIR, TARGET))
                .thenExecute(() -> {
                    helper.setBlock(DISPENSER.west(), Blocks.AIR);
                })
                .thenIdle(2)
                .thenExecute(() -> helper.setBlock(DISPENSER.west(), Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> helper.assertTrue(helper.getBlockState(TARGET).getFluidState().isSourceOfType(Fluids.LAVA), "lava was not placed again"))
                .thenExecute(() -> helper.assertTrue(BucketLibUtil.isEmpty(dispenser.getItem(0)), "dispenser bucket is not empty: " + TestUtils.describe(dispenser.getItem(0))))
                .thenSucceed();
    }

    /** A dispensed cracking bucket places its fluid and breaks. */
    public static void dispenserCrackingBucketBreaks(GameTestHelper helper) {
        DispenserBlockEntity dispenser = dispense(helper, TestUtils.withFluid(TestBuckets.CRACKING_BUCKET, Fluids.LAVA));
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(TARGET).getFluidState().isSourceOfType(Fluids.LAVA), "lava was not placed");
            helper.assertTrue(dispenser.getItem(0).isEmpty(), "cracking bucket did not break: " + TestUtils.describe(dispenser.getItem(0)));
        });
    }

    /** A dispensed bucket with durability loses one point when it is emptied. */
    public static void dispenserDamagesDurabilityBucket(GameTestHelper helper) {
        DispenserBlockEntity dispenser = dispense(helper, TestUtils.withFluid(TestBuckets.DURABILITY_BUCKET, Fluids.WATER));
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(TARGET).getFluidState().isSource(), "water was not placed");
            TestUtils.assertEquals(helper, dispenser.getItem(0).getDamageValue(), 1, "damage of the dispenser bucket");
        });
    }

    /** Like a vanilla water bucket, a dispensed water bucket waterlogs the block in front of it. */
    public static void dispenserWaterlogsBlocks(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.OAK_SLAB);
        DispenserBlockEntity dispenser = dispense(helper, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER));
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(TARGET, SlabBlock.WATERLOGGED, true);
            helper.assertTrue(BucketLibUtil.isEmpty(dispenser.getItem(0)), "dispenser bucket is not empty: " + TestUtils.describe(dispenser.getItem(0)));
        });
    }

    /** Like a vanilla bucket, a dispensed empty bucket takes the water of a waterlogged block. */
    public static void dispenserPicksUpWaterOfWaterloggedBlocks(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.WATERLOGGED, true));
        DispenserBlockEntity dispenser = dispense(helper, TestUtils.bucket(TestBuckets.TEST_BUCKET));
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(TARGET, SlabBlock.WATERLOGGED, false);
            TestUtils.assertEquals(helper, BucketLibUtil.getFluid(dispenser.getItem(0)), Fluids.WATER, "fluid in the dispenser bucket");
        });
    }

    // --- entities ----------------------------------------------------------------------------------------------------

    private static void assertNoPickup(GameTestHelper helper, ItemStack stack) {
        SulfurCube cube = helper.spawnWithNoFreeWill(EntityTypes.SULFUR_CUBE, TestUtils.CENTER);
        cube.setSize(2, true);
        cube.setBaby(false);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, stack);
        TestUtils.interact(player, cube);
        boolean removed = cube.isRemoved();
        boolean picked = BucketLibUtil.containsEntityType(player.getMainHandItem());
        cube.discard();
        helper.assertFalse(removed || picked, TestUtils.describe(stack) + " picked up a sulfur cube: " + TestUtils.describe(player.getMainHandItem()));
    }

    /**
     * Buckets with content (milk, powder snow, a fluid) do not pick up entities, also not sulfur cubes (whose vanilla
     * bucket has no fluid).
     */
    public static void filledBucketsDoNotPickUpEntities(GameTestHelper helper) {
        assertNoPickup(helper, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER));
        KnownIssues.check(helper, "KI-22 buckets with powder snow or milk pick up sulfur cubes", Platforms.ALL, () ->
                assertNoPickup(helper, TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW)));
        // with the NeoForge milk fluid, milk is a fluid and the bucket is not "fluid empty"
        KnownIssues.check(helper, "KI-22 buckets with powder snow or milk pick up sulfur cubes", Services.FLUID.hasMilkFluid() ? Set.of() : Platforms.ALL, () ->
                assertNoPickup(helper, TestUtils.withMilk(TestBuckets.TEST_BUCKET)));
        helper.succeed();
    }

    /** Like a vanilla water bucket in creative mode: the water bucket stays, the bucket with the fish is added. */
    public static void creativePickupKeepsTheWaterBucket(GameTestHelper helper) {
        var salmon = helper.spawnWithNoFreeWill(EntityTypes.SALMON, TestUtils.CENTER);
        ServerPlayer player = TestUtils.player(helper, GameType.CREATIVE, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER));
        TestUtils.interact(player, salmon);
        helper.assertTrue(salmon.isRemoved(), "salmon was not picked up in creative mode");
        ItemStack hand = player.getMainHandItem();
        helper.assertTrue(BucketLibUtil.getFluid(hand) == Fluids.WATER && !BucketLibUtil.containsEntityType(hand), "hand does not keep the water bucket: " + TestUtils.describe(hand));
        helper.assertTrue(player.getInventory().getNonEquipmentItems().stream().anyMatch(s -> !s.isEmpty() && BucketLibUtil.getEntityType(s) == EntityTypes.SALMON), "salmon bucket is not in the inventory");
        helper.succeed();
    }

    /** A bucket that denies salmon picks up other fish. */
    public static void antiSalmonBucketPicksUpCod(GameTestHelper helper) {
        for (var bucket : List.of(TestBuckets.ANTI_SALMON_BUCKET, TestBuckets.ANTI_SALMON_TAG_BUCKET)) {
            var cod = helper.spawnWithNoFreeWill(EntityTypes.COD, TestUtils.CENTER);
            ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.withFluid(bucket, Fluids.WATER));
            TestUtils.interact(player, cod);
            TestUtils.assertEquals(helper, BucketLibUtil.getEntityType(player.getMainHandItem()), EntityTypes.COD, "entity in the " + bucket.name());
        }
        helper.succeed();
    }

    // --- properties --------------------------------------------------------------------------------------------------

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

    // --- infinity ----------------------------------------------------------------------------------------------------

    /** A bucket with water and an entity is no infinite water bucket, it cannot be enchanted with Infinity. */
    public static void entityBucketsAreNotEnchantable(GameTestHelper helper) {
        var infinity = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.INFINITY);
        ItemStack salmon = TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.SALMON);
        helper.assertTrue(de.cech12.bucketlib.gametest.GameTestPlatform.INSTANCE.canEnchant(TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER), infinity), "test setup broken: water bucket cannot get Infinity");
        KnownIssues.run(helper, "KI-23 buckets with water and an entity can be enchanted with Infinity", Platforms.ALL, () ->
                helper.assertFalse(de.cech12.bucketlib.gametest.GameTestPlatform.INSTANCE.canEnchant(salmon, infinity), "bucket with water and salmon can be enchanted with Infinity"));
    }

    // --- blocks ------------------------------------------------------------------------------------------------------

    public static void blockPicksUpOneOfStackedBuckets(GameTestHelper helper) {
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

    public static void blockCreativePickupKeepsTheEmptyBucket(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.POWDER_SNOW);
        ServerPlayer player = TestUtils.player(helper, GameType.CREATIVE, TestUtils.bucket(TestBuckets.TEST_BUCKET));
        TestUtils.lookDownAt(helper, player, TARGET);
        TestUtils.use(helper, player);
        helper.assertBlockPresent(Blocks.AIR, TARGET);
        helper.assertTrue(BucketLibUtil.isEmpty(player.getMainHandItem()), "the empty bucket in the hand was filled in creative mode");
        helper.assertTrue(player.getInventory().getNonEquipmentItems().stream().anyMatch(s -> !s.isEmpty() && BucketLibUtil.getBlock(s) == Blocks.POWDER_SNOW), "powder snow bucket is not in the inventory");
        helper.succeed();
    }

    // --- contents ----------------------------------------------------------------------------------------------------

    /** Buckets with the same content are equal (they stack when empty and match recipes), different contents are not. */
    public static void stacksCompareByContent(GameTestHelper helper) {
        helper.assertTrue(ItemStack.isSameItemSameComponents(TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER), TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER)), "two water buckets differ");
        helper.assertFalse(ItemStack.isSameItemSameComponents(TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER), TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.LAVA)), "water and lava bucket are equal");
        helper.assertFalse(ItemStack.isSameItemSameComponents(TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.COD), TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.SALMON)), "cod and salmon bucket are equal");
        helper.assertFalse(ItemStack.isSameItemSameComponents(TestUtils.bucket(TestBuckets.TEST_BUCKET), TestUtils.withMilk(TestBuckets.TEST_BUCKET)), "empty and milk bucket are equal");
        helper.assertTrue(ItemStack.isSameItemSameComponents(BucketLibUtil.removeFluid(TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER), helper.getLevel(), null), TestUtils.bucket(TestBuckets.TEST_BUCKET)), "emptied bucket differs from a new bucket");
        helper.succeed();
    }

}
