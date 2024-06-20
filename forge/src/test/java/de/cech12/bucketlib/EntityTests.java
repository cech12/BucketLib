package de.cech12.bucketlib;

import de.cech12.bucketlib.api.BucketLib;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.gametest.GameTestHolder;

@SuppressWarnings("unused")
@GameTestHolder(BucketLib.MOD_ID)
public class EntityTests {

    private static final BlockPos ENTITY_POSITION = new BlockPos(1, 2, 1);

    private static final EntityType<?>[] MILKABLE_ENTITIES = {EntityType.COW, EntityType.GOAT};
    private static final EntityType<?>[] BUCKETABLE_ENTITIES = {EntityType.AXOLOTL, EntityType.COD, EntityType.PUFFERFISH, EntityType.SALMON, EntityType.TADPOLE, EntityType.TROPICAL_FISH};

    /*
    @GameTestGenerator
    public static List<TestFunction> generateMilkingTests() {
        List<TestFunction> testFunctions = new ArrayList<>();
        int[] stackSizes = {1, 2};
        boolean[] creativeStates = { false, true };
        for (EntityType<?> entityType : MILKABLE_ENTITIES) {
            for (int stackSize : stackSizes) {
                for (boolean isCreative : creativeStates) {
                    String entityName = Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(entityType)).getPath();
                    String testName = "test" + ((isCreative) ? "creative" : "survival") + "milking" + entityName + "withstack" + stackSize;
                    testFunctions.add(new TestFunction(
                            "defaultBatch",
                            testName,
                            BucketLib.id("entitytests.pit").toString(),
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

    @GameTestGenerator
    public static List<TestFunction> generateMilkingWithAntiMilkBucketTests() {
        List<TestFunction> testFunctions = new ArrayList<>();
        for (EntityType<?> entityType : MILKABLE_ENTITIES) {
            for (boolean isCreative : new boolean[]{false, true}) {
                String entityName = Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(entityType)).getPath();
                String testName = "test" + ((isCreative) ? "creative" : "survival") + "milking" + entityName + "withantimilkbucket";
                testFunctions.add(new TestFunction(
                        "defaultBatch",
                        testName,
                        BucketLib.id("entitytests.waterpit").toString(),
                        Rotation.NONE,
                        100,
                        0,
                        true,
                        test -> {
                            ItemStack bucket = new ItemStack(BucketLibTestMod.ANTI_MILK_BUCKET.get());
                            Entity entity = test.spawn(entityType, ENTITY_POSITION);
                            PlayerInteractionResult result = BucketLibTestHelper.useItemOnEntity(test, bucket, entity, isCreative);
                            if (result.getResult().consumesAction()) {
                                test.fail("Wrong InteractionResult after using anti milk bucket on a " + entityName + ": " + result.getResult());
                            }
                            test.succeed();
                        }
                ));
            }
        }
        return testFunctions;
    }

    @GameTestGenerator
    public static List<TestFunction> generateDrinkMilkTests() {
        List<TestFunction> testFunctions = new ArrayList<>();
        boolean[] creativeStates = { false, true };
        for (boolean isCreative : creativeStates) {
            String testName = "test" + ((isCreative) ? "creative" : "survival") + "drinkmilk";
            testFunctions.add(new TestFunction(
                    "defaultBatch",
                    testName,
                    BucketLib.id("entitytests.pit").toString(),
                    Rotation.NONE,
                    100,
                    0,
                    true,
                    test -> {
                        Player player = BucketLibTestHelper.createPlayer(test, isCreative);
                        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 2000, 1, false, false, true));
                        ItemStack bucket = BucketLibUtil.addMilk(new ItemStack(BucketLibTestMod.TEST_BUCKET.get()));
                        ItemStack result = bucket.finishUsingItem(player.level(), player);
                        if (BucketLibUtil.containsMilk(result) != isCreative) {
                            test.fail("The bucket in main hand does " + (isCreative ? "" : "not ") + "contain milk after " + (isCreative ? "creative" : "survival") + " using a milk bucket");
                        }
                        if (player.getEffect(MobEffects.BLINDNESS) != null) {
                            test.fail("The potion effects of the player were not cured after using a milk bucket.");
                        }
                        test.succeed();
                    }
            ));
        }
        return testFunctions;
    }

    @GameTestGenerator
    public static List<TestFunction> generatePickupEntityWithStandardBucketTests() {
        List<TestFunction> testFunctions = new ArrayList<>();
        Fluid[] fluids = {Fluids.EMPTY, Fluids.WATER, Fluids.LAVA };
        for (EntityType<?> entityType : BUCKETABLE_ENTITIES) {
            for (Fluid fluid : fluids) {
                for (boolean isCreative : new boolean[]{false, true}) {
                    String entityName = Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(entityType)).getPath();
                    String fluidName = fluid != Fluids.EMPTY ? Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(fluid)).getPath() : "empty";
                    String testName = "test" + ((isCreative) ? "creative" : "survival") + "pickup" + entityName + "with" + fluidName + "bucket";
                    testFunctions.add(new TestFunction(
                            "defaultBatch",
                            testName,
                            BucketLib.id("entitytests.waterpit").toString(),
                            Rotation.NONE,
                            100,
                            0,
                            true,
                            test -> {
                                ItemStack bucket = new ItemStack(BucketLibTestMod.TEST_BUCKET.get());
                                if (fluid != Fluids.EMPTY) {
                                    bucket = BucketLibUtil.addFluid(bucket, fluid);
                                }
                                Entity entity = test.spawn(entityType, ENTITY_POSITION);
                                PlayerInteractionResult result = BucketLibTestHelper.useItemOnEntity(test, bucket, entity, isCreative);
                                if (result.getResult().consumesAction() == (fluid != Fluids.WATER)) {
                                    test.fail("Wrong InteractionResult after using " + fluidName + " bucket on a " + entityName + ": " + result.getResult());
                                }
                                if ((BucketLibUtil.getEntityType(result.getObject()) == entityType) == (isCreative || fluid != Fluids.WATER)) {
                                    test.fail("The bucket in main hand does " + ((isCreative || fluid != Fluids.WATER) ? "" : "not ") + "contain a " + entityName + " after " + (isCreative ? "creative" : "survival") + " interacting with it with " + fluidName + " bucket");
                                }
                                if (isCreative && !BucketLibTestHelper.hasSpecificBucket(result.getPlayer(), (itemStack) -> BucketLibUtil.getFluid(itemStack) == fluid)) {
                                    test.fail("The player doesn't have " + fluidName + " bucket after creative interacting with a " + entityName + " with " + fluidName + " bucket");
                                }
                                if ((fluid == Fluids.WATER) && !BucketLibTestHelper.hasSpecificBucket(result.getPlayer(), (itemStack) -> BucketLibUtil.getEntityType(itemStack) == entityType)) {
                                    test.fail("The player doesn't have " + entityName + " bucket after " + (isCreative ? "creative" : "survival") + " interacting with a " + entityName + " with water bucket");
                                }
                                if (result.getObject().getCount() != 1) {
                                    test.fail("The bucket stack size " + result.getObject().getCount() + " in main hand after " + (isCreative ? "creative" : "survival") + " interacting with a " + entityName + " with " + fluidName + " bucket is not the same as before");
                                }
                                test.succeed();
                            }
                    ));
                }
            }
        }
        return testFunctions;
    }

    @GameTestGenerator
    public static List<TestFunction> generatePickupEntityWithNoEntitiesBucketTests() {
        List<TestFunction> testFunctions = new ArrayList<>();
        for (EntityType<?> entityType : BUCKETABLE_ENTITIES) {
            for (boolean isCreative : new boolean[]{false, true}) {
                String entityName = Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(entityType)).getPath();
                String testName = "test" + ((isCreative) ? "creative" : "survival") + "pickup" + entityName + "withnoentitybucket";
                testFunctions.add(new TestFunction(
                        "defaultBatch",
                        testName,
                        BucketLib.id("entitytests.waterpit").toString(),
                        Rotation.NONE,
                        100,
                        0,
                        true,
                        test -> {
                            ItemStack bucket = new ItemStack(BucketLibTestMod.NO_ENTITIES_BUCKET.get());
                            bucket = BucketLibUtil.addFluid(bucket, Fluids.WATER);
                            Entity entity = test.spawn(entityType, ENTITY_POSITION);
                            PlayerInteractionResult result = BucketLibTestHelper.useItemOnEntity(test, bucket, entity, isCreative);
                            if (result.getResult().consumesAction()) {
                                test.fail("Wrong InteractionResult after using no entities bucket on a " + entityName + ": " + result.getResult());
                            }
                            test.succeed();
                        }
                ));
            }
        }
        return testFunctions;
    }

    @GameTestGenerator
    public static List<TestFunction> generatePickupEntityWithAntiSalmonBucketTests() {
        List<TestFunction> testFunctions = new ArrayList<>();
        for (EntityType<?> entityType : BUCKETABLE_ENTITIES) {
            for (boolean isCreative : new boolean[]{false, true}) {
                String entityName = Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(entityType)).getPath();
                String testName = "test" + ((isCreative) ? "creative" : "survival") + "pickup" + entityName + "withantisalmonbucket";
                testFunctions.add(new TestFunction(
                        "defaultBatch",
                        testName,
                        BucketLib.id("entitytests.waterpit").toString(),
                        Rotation.NONE,
                        100,
                        0,
                        true,
                        test -> {
                            ItemStack bucket = new ItemStack(BucketLibTestMod.ANTI_SALMON_BUCKET.get());
                            bucket = BucketLibUtil.addFluid(bucket, Fluids.WATER);
                            Entity entity = test.spawn(entityType, ENTITY_POSITION);
                            PlayerInteractionResult result = BucketLibTestHelper.useItemOnEntity(test, bucket, entity, isCreative);
                            if (result.getResult().consumesAction() == (entityType == EntityType.SALMON)) {
                                test.fail("Wrong InteractionResult after using anti salmon bucket on a " + entityName + ": " + result.getResult());
                            }
                            test.succeed();
                        }
                ));
            }
        }
        return testFunctions;
    }

    @GameTestGenerator
    public static List<TestFunction> generatePickupEntityWithOnlyPufferBucketTests() {
        List<TestFunction> testFunctions = new ArrayList<>();
        for (EntityType<?> entityType : BUCKETABLE_ENTITIES) {
            for (boolean isCreative : new boolean[]{false, true}) {
                String entityName = Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(entityType)).getPath();
                String testName = "test" + ((isCreative) ? "creative" : "survival") + "pickup" + entityName + "withonlyoufferbucket";
                testFunctions.add(new TestFunction(
                        "defaultBatch",
                        testName,
                        BucketLib.id("entitytests.waterpit").toString(),
                        Rotation.NONE,
                        100,
                        0,
                        true,
                        test -> {
                            ItemStack bucket = new ItemStack(BucketLibTestMod.ONLY_PUFFER_BUCKET.get());
                            bucket = BucketLibUtil.addFluid(bucket, Fluids.WATER);
                            Entity entity = test.spawn(entityType, ENTITY_POSITION);
                            PlayerInteractionResult result = BucketLibTestHelper.useItemOnEntity(test, bucket, entity, isCreative);
                            if (result.getResult().consumesAction() == (entityType != EntityType.PUFFERFISH)) {
                                test.fail("Wrong InteractionResult after using only puffer bucket on a " + entityName + ": " + result.getResult());
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
