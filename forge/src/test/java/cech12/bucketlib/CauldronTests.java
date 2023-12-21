package cech12.bucketlib;

import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.GameTestHolder;

@SuppressWarnings("unused")
@GameTestHolder(BucketLib.MOD_ID)
public class CauldronTests {

    private static final BlockPos CAULDRON_POSITION = new BlockPos(1, 2, 1);

    @GameTest(template = "empty")
    public static void testWaterBucketOnEmptyCauldron(GameTestHelper test) {
        ItemStack bucket = BucketLibUtil.addFluid(new ItemStack(BucketLibTestMod.TEST_BUCKET.get()), Fluids.WATER);
        BucketLibTestHelper.useItemStackOnBlock(test, bucket, CAULDRON_POSITION, true);
        test.succeedIf(() -> test.assertBlockState(CAULDRON_POSITION,
                blockState -> blockState.getBlock() == Blocks.WATER_CAULDRON,
                () -> "Empty cauldron was not filled with water after using a water test bucket."));
    }

    /*
    @GameTestGenerator
    public static List<TestFunction> generateEmptyBucketOnFilledCauldronTests() {
        List<TestFunction> testFunctions = new ArrayList<>();
        List<Item> successBuckets = Arrays.asList(BucketLibTestMod.TEST_BUCKET.get(), BucketLibTestMod.WATER_ALLOWING_BUCKET.get(), BucketLibTestMod.WATER_ALLOWING_TAG_BUCKET.get());
        List<Item> failBuckets = Arrays.asList(BucketLibTestMod.WATER_DENYING_BUCKET.get(), BucketLibTestMod.WATER_DENYING_TAG_BUCKET.get());
        List<Item> allBuckets = new ArrayList<>(successBuckets);
        allBuckets.addAll(failBuckets);
        int[] stackSizes = {1, 2};
        boolean[] creativeStates = { false, true };
        for (Item bucketItem : allBuckets) {
            for (int stackSize : stackSizes) {
                for (boolean isCreative : creativeStates) {
                    String testName = "test" + ((isCreative) ? "creative" : "survival") + "empty" + Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(bucketItem)).getPath() + "withstack" + stackSize + "onfilledcauldron";
                    testFunctions.add(new TestFunction(
                            "defaultBatch",
                            testName,
                            new ResourceLocation(BucketLib.MOD_ID, "cauldrontests.water_full").toString(),
                            Rotation.NONE,
                            100,
                            0,
                            true,
                            test -> {
                                ItemStack bucket = new ItemStack(bucketItem, stackSize);
                                boolean isFailBucket = failBuckets.contains(bucketItem);
                                PlayerInteractionResult result = BucketLibTestHelper.useItemStackOnBlock(test, bucket, CAULDRON_POSITION, isCreative);
                                if ((test.getBlockState(CAULDRON_POSITION).getBlock() == Blocks.CAULDRON) == isFailBucket) {
                                    test.fail("Filled water cauldron was " + (isFailBucket ? "not " : "") + "emptied after using an empty " + Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(bucketItem)).getPath());
                                }
                                if ((BucketLibUtil.getFluid(result.getObject()) == Fluids.WATER) == (isFailBucket || isCreative || stackSize > 1)) {
                                    test.fail("The bucket in main hand does " + (isCreative ? "" : "not ") + "contain water after " + (isCreative ? "creative" : "survival") + " interacting with a filled water cauldron with a stack size of " + stackSize);
                                }
                                if ((isFailBucket || isCreative) && result.getObject().getCount() != stackSize) {
                                    test.fail("The bucket stack size " + result.getObject().getCount() + " in main hand after creative interacting with a filled water cauldron with a stack size of " + stackSize + " is not the same as before");
                                }
                                if (!(isFailBucket || isCreative) && stackSize > 1 && result.getObject().getCount() != stackSize - 1) {
                                    test.fail("The bucket stack size " + result.getObject().getCount() + " in main hand after survival interacting with a filled water cauldron with a stack size of " + stackSize + " is not one times lower");
                                }
                                if ((isFailBucket || isCreative || stackSize > 1) && !BucketLibTestHelper.hasSpecificBucket(result.getPlayer(), BucketLibUtil::isEmpty)) {
                                    test.fail("The player doesn't have an empty bucket after " + (isCreative ? "creative" : "survival") + " interacting with a filled water cauldron with a stack size of " + stackSize);
                                }
                                if (!(isFailBucket || isCreative) && !BucketLibTestHelper.hasSpecificBucket(result.getPlayer(), itemStack -> itemStack.getCount() == 1 && BucketLibUtil.getFluid(itemStack) == Fluids.WATER)) { //in creative mode, a vanilla water bucket is generated
                                    test.fail("The player doesn't have bucket filled with water after survival interacting with a filled water cauldron with a stack size of " + stackSize);
                                }
                                test.succeed();
                            }
                    ));
                }
            }
        }
        return testFunctions;
    }
     */

}
