package cech12.bucketlib;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Function;

public class BucketLibTestHelper {

    public static Player makeMockCreativePlayer(GameTestHelper helper) {
        return new Player(helper.getLevel(), BlockPos.ZERO, 0.0F, new GameProfile(UUID.randomUUID(), "test-mock-survival-player")) {
            public boolean isSpectator() {
                return false;
            }

            public boolean isCreative() {
                return true;
            }

            @Override
            @NotNull
            public Abilities getAbilities() {
                Abilities abilities = super.getAbilities();
                abilities.instabuild = true;
                return abilities;
            }
        };
    }

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

    public static boolean hasSpecificBucket(Player player, Function<ItemStack, Boolean> testMethod) {
        for (ItemStack itemStack : player.getInventory().items) {
            if (testMethod.apply(itemStack)) {
                return true;
            }
        }
        return false;
    }

    public static PlayerInteractionResult useItemStackOnBlock(GameTestHelper helper, ItemStack itemStack, BlockPos pos, boolean isCreative) {
        BlockPos blockpos = helper.absolutePos(pos);
        Player player = isCreative ? makeMockCreativePlayer(helper) : makeMockSurvivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, itemStack.copy());
        //centered one block above looking down
        player.absMoveTo(blockpos.getX() + 0.5D, blockpos.getY() + 1.0D, blockpos.getZ() + 0.5D, 0F, 90F);
        return new PlayerInteractionResult(player, itemStack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND));
    }

    public static PlayerInteractionResult useItemOnEntity(GameTestHelper helper, ItemStack stack, Entity entity, boolean isCreative) {
        Player player = isCreative ? makeMockCreativePlayer(helper) : makeMockSurvivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        InteractionResult interactionResult = player.interactOn(entity, InteractionHand.MAIN_HAND);
        return new PlayerInteractionResult(player, interactionResult, player.getItemInHand(InteractionHand.MAIN_HAND));
    }

}
