package cech12.bucketlib;

import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.GameTestHolder;

@SuppressWarnings("unused")
@GameTestHolder(BucketLib.MOD_ID)
public class WaterloggedTests {

    @GameTest(template = "interact")
    public static void testWaterBucketWaterloggsWaterloggableBlock(GameTestHelper test) {
        ItemStack bucket = BucketLibUtil.addFluid(new ItemStack(BucketLibTestMod.TEST_BUCKET.get()), Fluids.WATER);
        BlockPos waterlogPosition = new BlockPos(1, 1, 0);
        BucketLibTestHelper.useItemStackOnBlock(test, bucket, waterlogPosition, true);
        test.succeedWhen(() -> test.assertBlockProperty(waterlogPosition, BlockStateProperties.WATERLOGGED, property -> property,
                "Waterloggable block was not waterlogged after using a water test bucket."));
    }

    @GameTest(template = "interact")
    public static void testEmptyBucketEmptiesWaterloggedBlock(GameTestHelper test) {
        ItemStack bucket = new ItemStack(BucketLibTestMod.TEST_BUCKET.get());
        BlockPos waterloggedPosition = new BlockPos(1, 1, 0);
        BlockState blockState = test.getBlockState(waterloggedPosition);
        blockState.setValue(BlockStateProperties.WATERLOGGED, true);
        BucketLibTestHelper.useItemStackOnBlock(test, bucket, waterloggedPosition, true);
        test.succeedWhen(() -> test.assertBlockProperty(waterloggedPosition, BlockStateProperties.WATERLOGGED, property -> !property,
                "Waterlogged block was not emptied after using an empty test bucket."));
    }

    @GameTest(template = "dispenser")
    public static void testWaterBucketWaterloggsWaterloggableBlockWithDispenser(GameTestHelper test) {
        ItemStack bucket = BucketLibUtil.addFluid(new ItemStack(BucketLibTestMod.TEST_BUCKET.get()), Fluids.WATER);
        BlockPos dispenserPosition = new BlockPos(1, 1, 1);
        BlockPos buttonPosition = new BlockPos(0, 1, 1);
        BlockPos waterlogPosition = new BlockPos(1, 1, 0);
        BucketLibTestHelper.addItemStackToDispenser(test, bucket, dispenserPosition);
        test.pressButton(buttonPosition);
        test.succeedWhen(() -> test.assertBlockProperty(waterlogPosition, BlockStateProperties.WATERLOGGED, property -> property,
                "Waterloggable block was not waterlogged after using a water test bucket with a dispenser."));
    }

    @GameTest(template = "dispenser")
    public static void testEmptyBucketEmptiesWaterloggedBlockWithDispenser(GameTestHelper test) {
        ItemStack bucket = new ItemStack(BucketLibTestMod.TEST_BUCKET.get());
        BlockPos dispenserPosition = new BlockPos(1, 1, 1);
        BlockPos buttonPosition = new BlockPos(0, 1, 1);
        BlockPos waterloggedPosition = new BlockPos(1, 1, 0);
        BlockState blockState = test.getBlockState(waterloggedPosition);
        blockState.setValue(BlockStateProperties.WATERLOGGED, true);
        BucketLibTestHelper.addItemStackToDispenser(test, bucket, dispenserPosition);
        test.pressButton(buttonPosition);
        test.succeedWhen(() -> test.assertBlockProperty(waterloggedPosition, BlockStateProperties.WATERLOGGED, property -> !property,
                "Waterlogged block was not emptied after using an empty test bucket with a dispenser."));
    }

}
