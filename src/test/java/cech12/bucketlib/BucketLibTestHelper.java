package cech12.bucketlib;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class BucketLibTestHelper {

    public static Player makeMockSurvivalPlayer(GameTestHelper helper) {
        return new Player(helper.getLevel(), BlockPos.ZERO, 0.0F, new GameProfile(UUID.randomUUID(), "test-mock-survival-player")) {
            public boolean isSpectator() {
                return false;
            }

            public boolean isCreative() {
                return false;
            }
        };
    }

    public static InteractionResultHolder<ItemStack> useItemStackOnBlock(GameTestHelper helper, ItemStack itemStack, BlockPos pos, boolean isCreative) {
        BlockPos blockpos = helper.absolutePos(pos);
        Player player = isCreative ? helper.makeMockPlayer() : makeMockSurvivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, itemStack.copy());
        //centered one block above looking down
        player.absMoveTo(blockpos.getX() + 0.5D, blockpos.getY() + 1.0D, blockpos.getZ() + 0.5D, 0F, 90F);
        return itemStack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
    }

    public static InteractionResultHolder<ItemStack> useItemOnEntity(GameTestHelper helper, ItemStack stack, Entity entity, boolean isCreative) {
        Player player = isCreative ? helper.makeMockPlayer() : makeMockSurvivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        InteractionResult interactionResult = player.interactOn(entity, InteractionHand.MAIN_HAND);
        return new InteractionResultHolder<>(interactionResult, player.getItemInHand(InteractionHand.MAIN_HAND));
    }

}
