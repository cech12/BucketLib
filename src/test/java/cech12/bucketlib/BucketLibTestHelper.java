package cech12.bucketlib;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
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

    public static Player useItemStackOnBlock(GameTestHelper helper, ItemStack itemStack, BlockPos pos, boolean isCreative) {
        BlockPos blockpos = helper.absolutePos(pos);
        Player player = isCreative ? helper.makeMockPlayer() : makeMockSurvivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, itemStack.copy());
        //centered one block above looking down
        player.absMoveTo(blockpos.getX() + 0.5D, blockpos.getY() + 1.0D, blockpos.getZ() + 0.5D, 0F, 90F);
        itemStack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        return player;
    }

}
