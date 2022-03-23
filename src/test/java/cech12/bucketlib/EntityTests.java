package cech12.bucketlib;

import cech12.bucketlib.api.BucketLibApi;
import cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@GameTestHolder(BucketLibApi.MOD_ID)
public class EntityTests {

    private static final BlockPos ENTITY_POSITION = new BlockPos(1, 2, 1);

    @GameTestGenerator
    public static List<TestFunction> generateMilkingTests() {
        List<TestFunction> testFunctions = new ArrayList<>();
        EntityType<?>[] entityTypes = {EntityType.COW, EntityType.GOAT};
        int[] stackSizes = {1, 2};
        boolean[] creativeStates = { false, true };
        for (EntityType<?> entityType : entityTypes) {
            for (int stackSize : stackSizes) {
                for (boolean isCreative : creativeStates) {
                    String entityName = Objects.requireNonNull(entityType.getRegistryName()).getPath();
                    String testName = "test" + ((isCreative) ? "creative" : "survival") + "milkinga" + entityName + "withstack" + stackSize;
                    testFunctions.add(new TestFunction(
                            "defaultBatch",
                            testName,
                            new ResourceLocation(BucketLibApi.MOD_ID, "entitytests.pit").toString(),
                            Rotation.NONE,
                            100,
                            0,
                            true,
                            test -> {
                                ItemStack bucket = new ItemStack(BucketLibTestMod.TEST_BUCKET.get(), stackSize);
                                Entity entity = test.spawn(entityType, ENTITY_POSITION);
                                PlayerInteractionResult result = BucketLibTestHelper.useItemOnEntity(test, bucket, entity, isCreative);
                                if (!result.getResult().consumesAction()) {
                                    test.fail("Wrong InteractionResult after using an empty bucket on a " + entityName + ": " + result.getResult());
                                }
                                if (BucketLibUtil.containsMilk(result.getObject()) == (isCreative || stackSize > 1)) {
                                    test.fail("The bucket in main hand does " + (isCreative ? "" : "not ") + "contain milk after " + (isCreative ? "creative" : "survival") + " interacting with a " + entityName + " with a stack size of " + stackSize);
                                }
                                if (isCreative && result.getObject().getCount() != stackSize) {
                                    test.fail("The bucket stack size " + result.getObject().getCount() + " in main hand after creative interacting with a " + entityName + " with a stack size of " + stackSize + " is not the same as before");
                                }
                                if (!isCreative && stackSize > 1 && result.getObject().getCount() != stackSize - 1) {
                                    test.fail("The bucket stack size " + result.getObject().getCount() + " in main hand after survival interacting with a " + entityName + " with a stack size of " + stackSize + " is not one times lower");
                                }
                                if ((isCreative || stackSize > 1) && !BucketLibTestHelper.hasSpecificBucket(result.getPlayer(), BucketLibUtil::isEmpty)) {
                                    test.fail("The player doesn't have an empty bucket after " + (isCreative ? "creative" : "survival") + " interacting with a " + entityName + " with a stack size of " + stackSize);
                                }
                                if (!isCreative && !BucketLibTestHelper.hasSpecificBucket(result.getPlayer(), itemStack -> itemStack.getCount() == 1 && BucketLibUtil.containsMilk(itemStack))) { //in creatice mode, a vanilla milk bucket is generated
                                    test.fail("The player doesn't have bucket filled with milk after survival interacting with a " + entityName + " with a stack size of " + stackSize);
                                }
                                test.succeed();
                            }
                    ));
                }
            }
        }
        return testFunctions;
    }

}
