package cech12.bucketlib;

import cech12.bucketlib.api.BucketLibApi;
import cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;

@GameTestHolder(BucketLibApi.MOD_ID)
public class EntityTests {

    private static final BlockPos ENTITY_POSITION = new BlockPos(1, 2, 1);

    @GameTest(template = "pit")
    public static void testMilkingACow(GameTestHelper test) {
        ItemStack bucket = new ItemStack(BucketLibTestMod.TEST_BUCKET.get());
        Entity entity = test.spawn(EntityType.COW, ENTITY_POSITION);
        InteractionResultHolder<ItemStack> result = BucketLibTestHelper.useItemOnEntity(test, bucket, entity, false);
        if (!result.getResult().consumesAction()) {
            test.fail("Wrong InteractionResult after using an empty bucket on a cow: " + result.getResult());
        }
        if (!BucketLibUtil.containsMilk(result.getObject())) {
            test.fail("The bucket does not contain milk after interacting with a cow.");
        }
        test.succeed();
    }

    @GameTest(template = "pit")
    public static void testMilkingAGoat(GameTestHelper test) {
        ItemStack bucket = new ItemStack(BucketLibTestMod.TEST_BUCKET.get());
        Entity entity = test.spawn(EntityType.GOAT, ENTITY_POSITION);
        InteractionResultHolder<ItemStack> result = BucketLibTestHelper.useItemOnEntity(test, bucket, entity, false);
        if (!result.getResult().consumesAction()) {
            test.fail("Wrong InteractionResult after using an empty bucket on a goat: " + result.getResult());
        }
        if (!BucketLibUtil.containsMilk(result.getObject())) {
            test.fail("The bucket does not contain milk after interacting with a goat.");
        }
        test.succeed();
    }

}
