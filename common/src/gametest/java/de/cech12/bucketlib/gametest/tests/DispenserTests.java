package de.cech12.bucketlib.gametest.tests;

import de.cech12.bucketlib.gametest.BucketLibGameTests;
import de.cech12.bucketlib.gametest.KnownIssues;
import de.cech12.bucketlib.gametest.Platforms;
import de.cech12.bucketlib.gametest.TestBuckets;
import de.cech12.bucketlib.gametest.TestUtils;
import de.cech12.bucketlib.item.UniversalBucketDispenseBehaviour;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.material.Fluids;

import java.util.Set;
import net.minecraft.world.level.block.SlabBlock;

/**
 * Dispensers use BucketLib buckets like vanilla buckets: picking up and placing fluids, blocks and entities.
 */
public final class DispenserTests {

    private static final BlockPos DISPENSER = new BlockPos(2, 1, 1);
    private static final BlockPos TARGET = new BlockPos(2, 1, 2);

    private DispenserTests() {}

    public static void register(BucketLibGameTests.TestCollector tests) {
        tests.add("dispenser/picks_up_water", DispenserTests::picksUpWater);
        tests.add("dispenser/picks_up_water_with_stacked_buckets", DispenserTests::picksUpWaterWithStackedBuckets);
        tests.add("dispenser/places_water", DispenserTests::placesWater);
        tests.add("dispenser/picks_up_powder_snow", DispenserTests::picksUpPowderSnow);
        tests.add("dispenser/picks_up_powder_snow_with_stacked_buckets", DispenserTests::picksUpPowderSnowWithStackedBuckets);
        tests.add("dispenser/places_powder_snow", DispenserTests::placesPowderSnow);
        tests.add("dispenser/places_salmon_with_water", DispenserTests::placesSalmonWithWater);
        tests.add("dispenser/places_sulfur_cube", DispenserTests::placesSulfurCube);
        tests.add("dispenser/does_not_pick_up_denied_contents", DispenserTests::doesNotPickUpDeniedContents);
        tests.add("dispenser/drops_unusable_buckets_like_vanilla", DispenserTests::dropsUnusableBucketsLikeVanilla);
        tests.add("dispenser/milk_bucket_does_not_remove_water", DispenserTests::milkBucketDoesNotRemoveWater);
        tests.add("dispenser/ignores_other_items", DispenserTests::ignoresOtherItems);
        tests.add("dispenser/picks_up_and_places_lava", DispenserTests::picksUpAndPlacesLava);
        tests.add("dispenser/cracking_bucket_breaks", DispenserTests::crackingBucketBreaks);
        tests.add("dispenser/damages_durability_bucket", DispenserTests::damagesDurabilityBucket);
        tests.add("dispenser/waterlogs_blocks", DispenserTests::waterlogsBlocks);
        tests.add("dispenser/picks_up_water_of_waterlogged_blocks", DispenserTests::picksUpWaterOfWaterloggedBlocks);
    }

    /** Places a dispenser with the stack that faces the target position (south) and powers it. */
    private static DispenserBlockEntity dispense(GameTestHelper helper, ItemStack stack) {
        helper.setBlock(DISPENSER, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.SOUTH));
        DispenserBlockEntity dispenser = helper.getBlockEntity(DISPENSER, DispenserBlockEntity.class);
        dispenser.setItem(0, stack);
        helper.setBlock(DISPENSER.west(), Blocks.REDSTONE_BLOCK);
        return dispenser;
    }

    public static void picksUpWater(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.WATER);
        DispenserBlockEntity dispenser = dispense(helper, TestUtils.bucket(TestBuckets.TEST_BUCKET));
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, TARGET);
            TestUtils.assertEquals(helper, BucketLibUtil.getFluid(dispenser.getItem(0)), Fluids.WATER, "fluid in the dispenser bucket");
        });
    }

    /** With a stack of empty buckets, the filled bucket is put into another slot of the dispenser. */
    public static void picksUpWaterWithStackedBuckets(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.WATER);
        DispenserBlockEntity dispenser = dispense(helper, TestUtils.bucket(TestBuckets.TEST_BUCKET).copyWithCount(2));
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, TARGET);
            int empty = 0;
            int water = 0;
            for (int slot = 0; slot < dispenser.getContainerSize(); slot++) {
                ItemStack stack = dispenser.getItem(slot);
                if (stack.is(TestBuckets.TEST_BUCKET.item())) {
                    if (BucketLibUtil.isEmpty(stack)) {
                        empty += stack.getCount();
                    } else if (BucketLibUtil.getFluid(stack) == Fluids.WATER) {
                        water += stack.getCount();
                    }
                }
            }
            TestUtils.assertEquals(helper, empty, 1, "empty buckets in the dispenser");
            TestUtils.assertEquals(helper, water, 1, "water buckets in the dispenser");
        });
    }

    public static void placesWater(GameTestHelper helper) {
        DispenserBlockEntity dispenser = dispense(helper, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER));
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(TARGET).getFluidState().isSourceOfType(Fluids.WATER), "no water was placed");
            helper.assertTrue(dispenser.getItem(0).is(TestBuckets.TEST_BUCKET.item()) && BucketLibUtil.isEmpty(dispenser.getItem(0)), "dispenser bucket is not empty: " + TestUtils.describe(dispenser.getItem(0)));
        });
    }

    public static void picksUpPowderSnow(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.POWDER_SNOW);
        DispenserBlockEntity dispenser = dispense(helper, TestUtils.bucket(TestBuckets.TEST_BUCKET));
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, TARGET);
            TestUtils.assertEquals(helper, BucketLibUtil.getBlock(dispenser.getItem(0)), Blocks.POWDER_SNOW, "block in the dispenser bucket");
        });
    }

    public static void picksUpPowderSnowWithStackedBuckets(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.POWDER_SNOW);
        DispenserBlockEntity dispenser = dispense(helper, TestUtils.bucket(TestBuckets.TEST_BUCKET).copyWithCount(2));
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, TARGET);
            int snow = 0;
            int empty = 0;
            for (int slot = 0; slot < dispenser.getContainerSize(); slot++) {
                ItemStack stack = dispenser.getItem(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                if (BucketLibUtil.getBlock(stack) == Blocks.POWDER_SNOW) {
                    snow += stack.getCount();
                } else if (stack.is(TestBuckets.TEST_BUCKET.item())) {
                    empty += stack.getCount();
                }
            }
            TestUtils.assertEquals(helper, snow, 1, "powder snow buckets in the dispenser");
            TestUtils.assertEquals(helper, empty, 1, "empty buckets in the dispenser");
        });
    }

    public static void placesPowderSnow(GameTestHelper helper) {
        DispenserBlockEntity dispenser = dispense(helper, TestUtils.withBlock(TestBuckets.TEST_BUCKET, Blocks.POWDER_SNOW));
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.POWDER_SNOW, TARGET);
            helper.assertTrue(BucketLibUtil.isEmpty(dispenser.getItem(0)), "dispenser bucket is not empty: " + TestUtils.describe(dispenser.getItem(0)));
        });
    }

    public static void placesSalmonWithWater(GameTestHelper helper) {
        DispenserBlockEntity dispenser = dispense(helper, TestUtils.withEntity(TestBuckets.TEST_BUCKET, Fluids.WATER, EntityTypes.SALMON));
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(TARGET).getFluidState().isSourceOfType(Fluids.WATER), "no water was placed");
            Salmon salmon = helper.findOneEntity(EntityTypes.SALMON);
            helper.assertTrue(salmon.fromBucket(), "salmon is not marked as from a bucket");
            helper.assertTrue(BucketLibUtil.isEmpty(dispenser.getItem(0)), "dispenser bucket is not empty: " + TestUtils.describe(dispenser.getItem(0)));
        });
    }

    public static void placesSulfurCube(GameTestHelper helper) {
        DispenserBlockEntity dispenser = dispense(helper, TestUtils.withEntity(TestBuckets.TEST_BUCKET, null, EntityTypes.SULFUR_CUBE));
        helper.succeedWhen(() -> {
            helper.assertEntityPresent(EntityTypes.SULFUR_CUBE);
            helper.assertTrue(BucketLibUtil.isEmpty(dispenser.getItem(0)), "dispenser bucket is not empty: " + TestUtils.describe(dispenser.getItem(0)));
        });
    }

    /** Dispensers respect the properties of the bucket. */
    public static void doesNotPickUpDeniedContents(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.LAVA);
        DispenserBlockEntity dispenser = dispense(helper, TestUtils.bucket(TestBuckets.TEMPERATURE_BUCKET));
        helper.runAfterDelay(5, () -> {
            helper.assertBlockPresent(Blocks.LAVA, TARGET);
            boolean filled = false;
            for (int slot = 0; slot < dispenser.getContainerSize(); slot++) {
                filled |= !dispenser.getItem(slot).isEmpty() && BucketLibUtil.containsFluid(dispenser.getItem(slot));
            }
            helper.assertFalse(filled, "temperature bucket was filled with lava");
            helper.succeed();
        });
    }

    /**
     * Like a vanilla bucket, a bucket that cannot be used (empty bucket in front of air, filled bucket in front of a
     * solid block) is dropped by the dispenser.
     */
    public static void dropsUnusableBucketsLikeVanilla(GameTestHelper helper) {
        DispenserBlockEntity dispenser = dispense(helper, TestUtils.bucket(TestBuckets.TEST_BUCKET));
        helper.runAfterDelay(5, () -> {
            boolean dropped = !helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds()).isEmpty();
            KnownIssues.run(helper, "KI-4 Fabric dispensers keep unusable buckets instead of dropping them", Set.of(Platforms.FABRIC), () ->
                    helper.assertTrue(dropped && dispenser.getItem(0).isEmpty(), "unusable bucket was not dropped by the dispenser"));
        });
    }

    /** A milk bucket in a dispenser in front of a water source does nothing (milk cannot be placed). */
    public static void milkBucketDoesNotRemoveWater(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.WATER);
        DispenserBlockEntity dispenser = dispense(helper, TestUtils.withMilk(TestBuckets.TEST_BUCKET));
        helper.runAfterDelay(5, () -> KnownIssues.run(helper, "KI-5 dispensed milk bucket removes a water source in front of it", Set.of(Platforms.FABRIC), () -> {
            helper.assertBlockPresent(Blocks.WATER, TARGET);
            boolean milk = false;
            for (int slot = 0; slot < dispenser.getContainerSize(); slot++) {
                milk |= !dispenser.getItem(slot).isEmpty() && BucketLibUtil.containsMilk(dispenser.getItem(slot));
            }
            milk |= helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds()).stream().anyMatch(e -> BucketLibUtil.containsMilk(e.getItem()));
            helper.assertTrue(milk, "milk bucket was emptied");
        }));
    }

    /** The dispense behaviour of BucketLib passes other items to the fluid container behaviour of the loader. */
    public static void ignoresOtherItems(GameTestHelper helper) {
        helper.setBlock(DISPENSER, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.SOUTH));
        DispenserBlockEntity dispenser = helper.getBlockEntity(DISPENSER, DispenserBlockEntity.class);
        BlockSource source = new BlockSource(helper.getLevel(), helper.absolutePos(DISPENSER), helper.getBlockState(DISPENSER), dispenser);
        ItemStack stone = new ItemStack(Items.STONE);
        ItemStack result = TestUtils.noException(helper, "dispensing stone with the bucket behaviour", () -> UniversalBucketDispenseBehaviour.getInstance().execute(source, stone.copy()));
        // Fabric keeps the stone, NeoForge drops it like the default dispense behaviour (never happens in game: the behaviour is only registered for buckets)
        helper.assertTrue(result.is(Items.STONE) || result.isEmpty(), "stone was changed to " + result);
        helper.succeed();
    }

    public static void picksUpAndPlacesLava(GameTestHelper helper) {
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
    public static void crackingBucketBreaks(GameTestHelper helper) {
        DispenserBlockEntity dispenser = dispense(helper, TestUtils.withFluid(TestBuckets.CRACKING_BUCKET, Fluids.LAVA));
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(TARGET).getFluidState().isSourceOfType(Fluids.LAVA), "lava was not placed");
            helper.assertTrue(dispenser.getItem(0).isEmpty(), "cracking bucket did not break: " + TestUtils.describe(dispenser.getItem(0)));
        });
    }

    /** A dispensed bucket with durability loses one point when it is emptied. */
    public static void damagesDurabilityBucket(GameTestHelper helper) {
        DispenserBlockEntity dispenser = dispense(helper, TestUtils.withFluid(TestBuckets.DURABILITY_BUCKET, Fluids.WATER));
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(TARGET).getFluidState().isSource(), "water was not placed");
            TestUtils.assertEquals(helper, dispenser.getItem(0).getDamageValue(), 1, "damage of the dispenser bucket");
        });
    }

    /** Like a vanilla water bucket, a dispensed water bucket waterlogs the block in front of it. */
    public static void waterlogsBlocks(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.OAK_SLAB);
        DispenserBlockEntity dispenser = dispense(helper, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER));
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(TARGET, SlabBlock.WATERLOGGED, true);
            helper.assertTrue(BucketLibUtil.isEmpty(dispenser.getItem(0)), "dispenser bucket is not empty: " + TestUtils.describe(dispenser.getItem(0)));
        });
    }

    /** Like a vanilla bucket, a dispensed empty bucket takes the water of a waterlogged block. */
    public static void picksUpWaterOfWaterloggedBlocks(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.WATERLOGGED, true));
        DispenserBlockEntity dispenser = dispense(helper, TestUtils.bucket(TestBuckets.TEST_BUCKET));
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(TARGET, SlabBlock.WATERLOGGED, false);
            TestUtils.assertEquals(helper, BucketLibUtil.getFluid(dispenser.getItem(0)), Fluids.WATER, "fluid in the dispenser bucket");
        });
    }

}
