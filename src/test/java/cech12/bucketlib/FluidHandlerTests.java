package cech12.bucketlib;

import cech12.bucketlib.api.BucketLibApi;
import cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("unused")
@GameTestHolder(BucketLibApi.MOD_ID)
public class FluidHandlerTests {

    private static final Fluid[] FLUIDS = { Fluids.WATER, Fluids.LAVA };

    private static Fluid[] getFluids() {
        if (BucketLibTestMod.MILK_ENABLED) {
            return new Fluid[] { Fluids.WATER, Fluids.LAVA, ForgeMod.MILK.get() };
        }
        return FLUIDS;
    }

    @GameTestGenerator
    public static List<TestFunction> generateEmptyBucketFluidHandlerTests() {
        List<TestFunction> testFunctions = new ArrayList<>();
        for (Fluid fluid : getFluids()) {
            String fluidName = Objects.requireNonNull(fluid.getRegistryName()).getPath();
            String testName = "testfluidhandlerofemptybucketwith" + fluidName;
            testFunctions.add(new TestFunction(
                    "defaultBatch",
                    testName,
                    new ResourceLocation(BucketLibApi.MOD_ID, "cauldrontests.empty").toString(),
                    Rotation.NONE,
                    100,
                    0,
                    true,
                    test -> {
                        ItemStack bucket = new ItemStack(BucketLibTestMod.TEST_BUCKET.get());
                        if (!FluidUtil.getFluidHandler(bucket).isPresent()) {
                            test.fail("Empty Bucket has no fluid handler.");
                        }
                        FluidUtil.getFluidHandler(bucket).ifPresent(fluidHandler -> {
                            FluidStack drainedStack = fluidHandler.drain(FluidAttributes.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
                            if (!drainedStack.isEmpty()) {
                                test.fail("Something was drained of an empty Bucket. FluidStack: " + drainedStack.getFluid().toString() + " - " + drainedStack.getAmount());
                            }
                            int filledAmount = fluidHandler.fill(new FluidStack(fluid, FluidAttributes.BUCKET_VOLUME - 1), IFluidHandler.FluidAction.EXECUTE);
                            if (filledAmount > 0) {
                                test.fail("Empty Bucket was filled although less than the bucket volume was filled " + fluidName + ". Amount: " + filledAmount);
                            }
                            filledAmount = fluidHandler.fill(new FluidStack(fluid, FluidAttributes.BUCKET_VOLUME + 1), IFluidHandler.FluidAction.EXECUTE);
                            if (filledAmount != FluidAttributes.BUCKET_VOLUME) {
                                test.fail("Empty Bucket was not completely filled with " + fluidName + ". Amount: " + filledAmount);
                            }
                        });
                        test.succeed();
                    }
            ));
        }
        return testFunctions;
    }

    @GameTestGenerator
    public static List<TestFunction> generateFluidBucketFluidHandlerTests() {
        List<TestFunction> testFunctions = new ArrayList<>();
        for (Fluid bucketFluid : FLUIDS) {
            for (Fluid fluid : getFluids()) {
                String bucketFluidName = Objects.requireNonNull(bucketFluid.getRegistryName()).getPath();
                String fluidName = Objects.requireNonNull(fluid.getRegistryName()).getPath();
                String testName = "testfluidhandlerof" + bucketFluidName + "bucketwith" + fluidName;
                testFunctions.add(new TestFunction(
                        "defaultBatch",
                        testName,
                        new ResourceLocation(BucketLibApi.MOD_ID, "cauldrontests.empty").toString(),
                        Rotation.NONE,
                        100,
                        0,
                        true,
                        test -> {
                            ItemStack bucket = new ItemStack(BucketLibTestMod.TEST_BUCKET.get());
                            bucket = BucketLibUtil.addFluid(bucket, bucketFluid);
                            if (!FluidUtil.getFluidHandler(bucket).isPresent()) {
                                test.fail(bucketFluidName + " bucket has no fluid handler.");
                            }
                            FluidUtil.getFluidHandler(bucket).ifPresent(fluidHandler -> {
                                int filledAmount = fluidHandler.fill(new FluidStack(fluid, FluidAttributes.BUCKET_VOLUME - 1), IFluidHandler.FluidAction.EXECUTE);
                                if (filledAmount > 0) {
                                    test.fail(bucketFluidName + " bucket could be filled although it should not be filled. (less than the bucket volume was filled) " + fluidName + ". Amount: " + filledAmount);
                                }
                                filledAmount = fluidHandler.fill(new FluidStack(fluid, FluidAttributes.BUCKET_VOLUME + 1), IFluidHandler.FluidAction.EXECUTE);
                                if (filledAmount > 0) {
                                    test.fail(bucketFluidName + " bucket could be filled with " + fluidName + " although it should not be filled. (more than the bucket volume was filled) " + " Amount: " + filledAmount);
                                }
                                FluidStack drainedStack = fluidHandler.drain(FluidAttributes.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
                                if (drainedStack.getFluid() != bucketFluid || drainedStack.getAmount() != FluidAttributes.BUCKET_VOLUME) {
                                    test.fail("The drained FluidStack of a " + bucketFluidName + " bucket does not match the expected fluid. Drained: " + drainedStack.getFluid().toString() + " - " + drainedStack.getAmount());
                                }
                            });
                            test.succeed();
                        }
                ));
            }
        }
        return testFunctions;
    }

    @GameTestGenerator
    public static List<TestFunction> generateMilkBucketFluidHandlerTests() {
        List<TestFunction> testFunctions = new ArrayList<>();
        for (Fluid fluid : getFluids()) {
            String fluidName = Objects.requireNonNull(fluid.getRegistryName()).getPath();
            String testName = "testfluidhandlerofmilkbucketwith" + fluidName;
            testFunctions.add(new TestFunction(
                    "defaultBatch",
                    testName,
                    new ResourceLocation(BucketLibApi.MOD_ID, "cauldrontests.empty").toString(),
                    Rotation.NONE,
                    100,
                    0,
                    true,
                    test -> {
                        ItemStack bucket = new ItemStack(BucketLibTestMod.TEST_BUCKET.get());
                        bucket = BucketLibUtil.addMilk(bucket);
                        if (!FluidUtil.getFluidHandler(bucket).isPresent()) {
                            test.fail("milk bucket has no fluid handler.");
                        }
                        FluidUtil.getFluidHandler(bucket).ifPresent(fluidHandler -> {
                            int filledAmount = fluidHandler.fill(new FluidStack(fluid, FluidAttributes.BUCKET_VOLUME - 1), IFluidHandler.FluidAction.EXECUTE);
                            if (filledAmount > 0) {
                                test.fail("Milk bucket could be filled with " + fluidName + " although it should not be filled. (less than the bucket volume was filled) " + fluidName + ". Amount: " + filledAmount);
                            }
                            filledAmount = fluidHandler.fill(new FluidStack(fluid, FluidAttributes.BUCKET_VOLUME + 1), IFluidHandler.FluidAction.EXECUTE);
                            if (filledAmount > 0) {
                                test.fail("Milk Bucket could be filled with " + fluidName + ". (more than the bucket volume was filled) Amount: " + filledAmount);
                            }
                            FluidStack drainedStack = fluidHandler.drain(FluidAttributes.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
                            if (BucketLibTestMod.MILK_ENABLED) {
                                if (drainedStack.getFluid() != ForgeMod.MILK.get() || drainedStack.getAmount() != FluidAttributes.BUCKET_VOLUME) {
                                    test.fail("The drained FluidStack of a milk bucket does not match the expected fluid. Drained: " + drainedStack.getFluid().toString() + " - " + drainedStack.getAmount());
                                }
                            } else {
                                if (!drainedStack.isEmpty()) {
                                    test.fail("Something was drained of an milk Bucket. FluidStack: " + drainedStack.getFluid().toString() + " - " + drainedStack.getAmount());
                                }
                            }
                        });
                        if (BucketLibTestMod.MILK_ENABLED && BucketLibUtil.containsMilk(bucket)) {
                            test.fail("Milk Bucket contains milk after it was drained.");
                        }
                        test.succeed();
                    }
            ));
        }
        return testFunctions;
    }

    @GameTestGenerator
    public static List<TestFunction> generateAxolotlBucketFluidHandlerTests() {
        List<TestFunction> testFunctions = new ArrayList<>();
        for (Fluid fluid : getFluids()) {
            String fluidName = Objects.requireNonNull(fluid.getRegistryName()).getPath();
            String testName = "testfluidhandlerofaxolotlbucketwith" + fluidName;
            testFunctions.add(new TestFunction(
                    "defaultBatch",
                    testName,
                    new ResourceLocation(BucketLibApi.MOD_ID, "cauldrontests.empty").toString(),
                    Rotation.NONE,
                    100,
                    0,
                    true,
                    test -> {
                        ItemStack bucket = new ItemStack(BucketLibTestMod.TEST_BUCKET.get());
                        bucket = BucketLibUtil.addFluid(bucket, Fluids.WATER);
                        bucket = BucketLibUtil.addEntityType(bucket, EntityType.AXOLOTL);
                        if (!FluidUtil.getFluidHandler(bucket).isPresent()) {
                            test.fail("axolotl bucket has no fluid handler.");
                        }
                        FluidUtil.getFluidHandler(bucket).ifPresent(fluidHandler -> {
                            FluidStack drainedStack = fluidHandler.drain(FluidAttributes.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
                            if (!drainedStack.isEmpty()) {
                                test.fail("Something was drained of an axolotl Bucket. FluidStack: " + drainedStack.getFluid().toString() + " - " + drainedStack.getAmount());
                            }
                            int filledAmount = fluidHandler.fill(new FluidStack(fluid, FluidAttributes.BUCKET_VOLUME - 1), IFluidHandler.FluidAction.EXECUTE);
                            if (filledAmount > 0) {
                                test.fail("Axolotl bucket could be filled with " + fluidName + " although it should not be filled. (less than the bucket volume was filled). Amount: " + filledAmount);
                            }
                            filledAmount = fluidHandler.fill(new FluidStack(fluid, FluidAttributes.BUCKET_VOLUME + 1), IFluidHandler.FluidAction.EXECUTE);
                            if (filledAmount > 0) {
                                test.fail("Axolotl Bucket could be filled with " + fluidName + " although it should not be filled. (more than the bucket volume was filled) Amount: " + filledAmount);
                            }
                        });
                        test.succeed();
                    }
            ));
        }
        return testFunctions;
    }

}
