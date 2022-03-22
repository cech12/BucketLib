package cech12.bucketlib;

import cech12.bucketlib.api.BucketLibApi;
import cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.GameTestHolder;

@GameTestHolder(BucketLibApi.MOD_ID)
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

}
