package de.cech12.bucketlib.gametest.tests;

import de.cech12.bucketlib.gametest.BucketLibGameTests;
import de.cech12.bucketlib.gametest.TestBuckets;
import de.cech12.bucketlib.gametest.TestUtils;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.inventory.FurnaceFuelSlot;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;

import java.util.Map;
import java.util.Optional;

/**
 * Buckets as furnace fuel (burn time of the fluid, empty bucket as remainder), the wet sponge mechanic and the rules of
 * the fuel slot (hoppers, menus) like with vanilla buckets.
 */
public final class FurnaceTests {

    private static final BlockPos FURNACE = TestUtils.CENTER;

    private FurnaceTests() {}

    public static void register(BucketLibGameTests.TestCollector tests) {
        tests.add("furnace/burn_time_of_contents", FurnaceTests::burnTimeOfContents);
        tests.add("furnace/lava_bucket_burns_and_leaves_the_bucket", FurnaceTests::lavaBucketBurnsAndLeavesTheBucket);
        tests.add("furnace/empty_fuel_bucket_burns", FurnaceTests::emptyFuelBucketBurns);
        tests.add("furnace/wet_sponge_fills_empty_bucket", FurnaceTests::wetSpongeFillsEmptyBucket);
        tests.add("furnace/hoppers_take_empty_and_water_buckets", FurnaceTests::hoppersTakeEmptyAndWaterBuckets);
        tests.add("furnace/fuel_slot_accepts_buckets_like_vanilla", FurnaceTests::fuelSlotAcceptsBucketsLikeVanilla);
        tests.add("furnace/fuel_rules_work_on_a_dedicated_server", FurnaceTests::fuelRulesWorkOnADedicatedServer);
    }

    private static int burnTime(GameTestHelper helper, ItemStack stack) {
        LootContext context = new LootContext.Builder(new LootParams.Builder(helper.getLevel()).create(LootContextParamSets.EMPTY)).create(Optional.empty());
        return ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime, context, 0);
    }

    /**
     * A bucket with a fluid burns as long as the vanilla bucket of the fluid. Empty buckets use the fuel of the
     * properties (default 0), all other contents do not burn.
     */
    public static void burnTimeOfContents(GameTestHelper helper) {
        int lava = burnTime(helper, new ItemStack(Items.LAVA_BUCKET));
        helper.assertTrue(lava > 0, "test setup broken: vanilla lava bucket does not burn");
        Map<String, Integer> expected = new java.util.LinkedHashMap<>();
        expected.put("lava", burnTime(helper, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.LAVA)));
        expected.put("lava in fuel bucket", burnTime(helper, TestUtils.withFluid(TestBuckets.FUEL_BUCKET, Fluids.LAVA)));
        TestUtils.assertEquals(helper, expected.get("lava"), lava, "burn time of a lava bucket");
        TestUtils.assertEquals(helper, expected.get("lava in fuel bucket"), lava, "burn time of a lava fuel bucket");
        TestUtils.assertEquals(helper, burnTime(helper, TestUtils.bucket(TestBuckets.TEST_BUCKET)), 0, "burn time of an empty bucket");
        TestUtils.assertEquals(helper, burnTime(helper, TestUtils.bucket(TestBuckets.FUEL_BUCKET)), 200, "burn time of an empty fuel bucket");
        TestUtils.assertEquals(helper, burnTime(helper, TestUtils.withFluid(TestBuckets.FUEL_BUCKET, Fluids.WATER)), 0, "burn time of a water fuel bucket");
        TestUtils.assertEquals(helper, burnTime(helper, TestUtils.withMilk(TestBuckets.FUEL_BUCKET)), 0, "burn time of a milk fuel bucket");
        TestUtils.assertEquals(helper, burnTime(helper, TestUtils.withBlock(TestBuckets.FUEL_BUCKET, Blocks.POWDER_SNOW)), 0, "burn time of a powder snow fuel bucket");
        TestUtils.assertEquals(helper, burnTime(helper, TestUtils.withEntity(TestBuckets.FUEL_BUCKET, Fluids.WATER, EntityTypes.SALMON)), 0, "burn time of a salmon fuel bucket");
        helper.assertTrue(TestUtils.bucket(TestBuckets.TEST_BUCKET).has(DataComponents.COOKING_FUEL), "bucket has no cooking fuel component");
        helper.succeed();
    }

    private static FurnaceBlockEntity furnace(GameTestHelper helper, ItemStack input, ItemStack fuel) {
        helper.setBlock(FURNACE, Blocks.FURNACE);
        FurnaceBlockEntity furnace = helper.getBlockEntity(FURNACE, FurnaceBlockEntity.class);
        furnace.setItem(0, input);
        furnace.setItem(1, fuel);
        return furnace;
    }

    /** A lava bucket is burned and the empty bucket stays in the fuel slot (crafting remainder). */
    public static void lavaBucketBurnsAndLeavesTheBucket(GameTestHelper helper) {
        FurnaceBlockEntity furnace = furnace(helper, new ItemStack(Items.COBBLESTONE), TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.LAVA));
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(FURNACE, net.minecraft.world.level.block.AbstractFurnaceBlock.LIT, true);
            ItemStack fuel = furnace.getItem(1);
            helper.assertTrue(fuel.is(TestBuckets.TEST_BUCKET.item()) && BucketLibUtil.isEmpty(fuel), "fuel slot does not contain the empty bucket: " + TestUtils.describe(fuel));
        });
    }

    /** An empty bucket with cooking fuel burns and is used up. */
    public static void emptyFuelBucketBurns(GameTestHelper helper) {
        FurnaceBlockEntity furnace = furnace(helper, new ItemStack(Items.COBBLESTONE), TestUtils.bucket(TestBuckets.FUEL_BUCKET));
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(FURNACE, net.minecraft.world.level.block.AbstractFurnaceBlock.LIT, true);
            helper.assertTrue(furnace.getItem(1).isEmpty(), "fuel bucket was not used up: " + furnace.getItem(1));
        });
    }

    /** Like a vanilla bucket, an empty bucket in the fuel slot is filled with water when a wet sponge is dried. */
    public static void wetSpongeFillsEmptyBucket(GameTestHelper helper) {
        FurnaceBlockEntity furnace = furnace(helper, new ItemStack(Items.WET_SPONGE), new ItemStack(Items.COAL));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(furnace.getItem(1).isEmpty(), "coal was not burned"))
                .thenExecute(() -> furnace.setItem(1, TestUtils.bucket(TestBuckets.TEST_BUCKET)))
                .thenWaitUntil(() -> helper.assertTrue(furnace.getItem(2).is(Items.SPONGE), "sponge was not dried"))
                .thenExecute(() -> TestUtils.assertEquals(helper, BucketLibUtil.getFluid(furnace.getItem(1)), Fluids.WATER, "fluid of the bucket in the fuel slot"))
                .thenSucceed();
    }

    /** Hoppers below a furnace take empty and water buckets out of the fuel slot, like vanilla buckets. */
    public static void hoppersTakeEmptyAndWaterBuckets(GameTestHelper helper) {
        FurnaceBlockEntity furnace = furnace(helper, ItemStack.EMPTY, ItemStack.EMPTY);
        helper.assertTrue(furnace.canTakeItemThroughFace(1, new ItemStack(Items.WATER_BUCKET), Direction.DOWN), "test setup broken: vanilla water bucket");
        helper.assertTrue(furnace.canTakeItemThroughFace(1, TestUtils.bucket(TestBuckets.TEST_BUCKET), Direction.DOWN), "empty bucket cannot be taken");
        helper.assertTrue(furnace.canTakeItemThroughFace(1, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER), Direction.DOWN), "water bucket cannot be taken");
        helper.assertFalse(furnace.canTakeItemThroughFace(1, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.LAVA), Direction.DOWN), "lava bucket can be taken");
        helper.assertTrue(furnace.canTakeItemThroughFace(0, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.LAVA), Direction.DOWN), "lava bucket cannot be taken from the input slot");
        helper.succeed();
    }

    /** The fuel slot accepts empty buckets that can hold water (wet sponge), with max stack size 1 like vanilla buckets. */
    public static void fuelSlotAcceptsBucketsLikeVanilla(GameTestHelper helper) {
        helper.assertTrue(FurnaceFuelSlot.isBucket(TestUtils.bucket(TestBuckets.TEST_BUCKET)), "empty bucket is no fuel slot bucket");
        helper.assertFalse(FurnaceFuelSlot.isBucket(TestUtils.bucket(TestBuckets.WATER_DENYING_BUCKET)), "bucket that cannot hold water is a fuel slot bucket");
        helper.assertFalse(FurnaceFuelSlot.isBucket(TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER)), "water bucket is a fuel slot bucket");
        helper.assertFalse(FurnaceFuelSlot.isBucket(ItemStack.EMPTY), "empty stack is a fuel slot bucket");
        helper.assertTrue(FurnaceFuelSlot.isBucket(new ItemStack(Items.BUCKET)), "vanilla bucket is no fuel slot bucket");
        FurnaceBlockEntity furnace = furnace(helper, ItemStack.EMPTY, ItemStack.EMPTY);
        helper.assertTrue(furnace.canPlaceItem(1, TestUtils.bucket(TestBuckets.TEST_BUCKET)), "empty bucket cannot be placed in the fuel slot");
        helper.succeed();
    }

    /**
     * Hoppers and players put buckets into the fuel slot on a dedicated server: the furnace and the furnace menu decide
     * which buckets are fuel. This must work without client classes.
     */
    public static void fuelRulesWorkOnADedicatedServer(GameTestHelper helper) {
        FurnaceBlockEntity furnace = furnace(helper, ItemStack.EMPTY, ItemStack.EMPTY);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, ItemStack.EMPTY);
        FurnaceMenu menu = new FurnaceMenu(1, player.getInventory(), furnace, new SimpleContainerData(4));
        boolean lava = TestUtils.noException(helper, "hopper inserts a lava bucket into the fuel slot", () -> furnace.canPlaceItem(1, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.LAVA)));
        helper.assertTrue(lava, "lava bucket cannot be placed in the fuel slot");
        boolean water = TestUtils.noException(helper, "hopper inserts a water bucket into the fuel slot", () -> furnace.canPlaceItem(1, TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.WATER)));
        helper.assertFalse(water, "water bucket can be placed in the fuel slot");
        boolean menuLava = TestUtils.noException(helper, "player puts a lava bucket into the fuel slot", () -> menu.getSlot(1).mayPlace(TestUtils.withFluid(TestBuckets.TEST_BUCKET, Fluids.LAVA)));
        helper.assertTrue(menuLava, "lava bucket cannot be put into the fuel slot of the menu");
        boolean menuEmpty = TestUtils.noException(helper, "player puts an empty bucket into the fuel slot", () -> menu.getSlot(1).mayPlace(TestUtils.bucket(TestBuckets.TEST_BUCKET)));
        helper.assertTrue(menuEmpty, "empty bucket cannot be put into the fuel slot of the menu");
        helper.succeed();
    }

}
