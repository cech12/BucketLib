package de.cech12.bucketlib;

import de.cech12.bucketlib.api.BucketLib;
import net.minecraft.core.BlockPos;
import net.minecraftforge.gametest.GameTestHolder;

@GameTestHolder(BucketLib.MOD_ID)
public class WorldInteractionTests {

    private static final BlockPos POWDER_SNOW_POSITION = new BlockPos(0, 1, 0);

    /*
    @GameTestGenerator
    public static List<TestFunction> generatePowderSnowPickupTests() {
        List<TestFunction> testFunctions = new ArrayList<>();
        int[] stackSizes = {1, 2};
        boolean[] creativeStates = { false, true };
        for (int stackSize : stackSizes) {
            for (boolean isCreative : creativeStates) {
                String testName = "test" + ((isCreative) ? "creative" : "survival") + "pickuppowdersnowwithstack" + stackSize;
                testFunctions.add(new TestFunction(
                        "defaultBatch",
                        testName,
                        new ResourceLocation(BucketLib.MOD_ID, "worldinteractiontests.powder_snow").toString(),
                        Rotation.NONE,
                        100,
                        0,
                        true,
                        test -> {
                            ItemStack bucket = new ItemStack(BucketLibTestMod.TEST_BUCKET.get(), stackSize);
                            PlayerInteractionResult result = BucketLibTestHelper.useItemStackOnBlock(test, bucket, POWDER_SNOW_POSITION, isCreative);
                            if (!result.getResult().consumesAction()) {
                                test.fail("Wrong InteractionResult after using an empty bucket on powder snow: " + result.getResult());
                            }
                            if ((BucketLibUtil.getBlock(result.getObject()) == Blocks.POWDER_SNOW) == (isCreative || stackSize > 1)) {
                                test.fail("The bucket in main hand does " + (isCreative ? "" : "not ") + "contain powder snow after " + (isCreative ? "creative" : "survival") + " interacting with a powder snow block with a stack size of " + stackSize);
                            }
                            if (isCreative && result.getObject().getCount() != stackSize) {
                                test.fail("The bucket stack size " + result.getObject().getCount() + " in main hand after creative interacting with a powder snow block with a stack size of " + stackSize + " is not the same as before");
                            }
                            if (!isCreative && stackSize > 1 && result.getObject().getCount() != stackSize - 1) {
                                test.fail("The bucket stack size " + result.getObject().getCount() + " in main hand after survival interacting with a powder snow block with a stack size of " + stackSize + " is not one times lower");
                            }
                            if ((isCreative || stackSize > 1) && !BucketLibTestHelper.hasSpecificBucket(result.getPlayer(), BucketLibUtil::isEmpty)) {
                                test.fail("The player doesn't have an empty bucket after " + (isCreative ? "creative" : "survival") + " interacting with a powder snow block with a stack size of " + stackSize);
                            }
                            if (!isCreative && !BucketLibTestHelper.hasSpecificBucket(result.getPlayer(), itemStack -> itemStack.getCount() == 1 && BucketLibUtil.getBlock(itemStack) == Blocks.POWDER_SNOW)) { //in creatice mode, a vanilla powder snow bucket is generated
                                test.fail("The player doesn't have bucket filled with powder snow after survival interacting with a powder snow block with a stack size of " + stackSize);
                            }
                            test.succeed();
                        }
                ));
            }
        }
        return testFunctions;
    }
     */

}
