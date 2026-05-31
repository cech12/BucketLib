package de.cech12.bucketlib.mixin;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractFurnaceBlockEntity.class})
public abstract class AbstractFurnaceBlockEntityMixin {

    @Shadow
    protected abstract NonNullList<ItemStack> getItems();

    @Inject(at = @At("HEAD"), method = "burn")
    private static void burnProxy(final NonNullList<ItemStack> items, final ItemStack inputItemStack, final ItemStack result, CallbackInfo ci) {
        ItemStack stack = items.get(1);
        if (inputItemStack.is(Items.WET_SPONGE) && !stack.isEmpty() && stack.getItem() instanceof UniversalBucketItem && stack.getCount() == 1 && BucketLibUtil.isEmpty(stack)) {
            items.set(1, BucketLibUtil.addFluid(stack, Fluids.WATER));
        }
    }

    @Inject(at = @At("RETURN"), method = "canTakeItemThroughFace", cancellable = true)
    public void canTakeItemThroughFaceProxy(final int slot, final ItemStack itemStack, final Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && direction == Direction.DOWN && slot == 1 && !itemStack.isEmpty() && itemStack.getItem() instanceof UniversalBucketItem
                && (BucketLibUtil.isEmpty(itemStack) || BucketLibUtil.getFluid(itemStack) == Fluids.WATER)
        ) {
            cir.setReturnValue(true);
        }
    }

    @Inject(at = @At("RETURN"), method = "canPlaceItem", cancellable = true)
    public void canPlaceItemProxy(final int slot, final ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && slot == 1 && !itemStack.isEmpty() && itemStack.getItem() instanceof UniversalBucketItem bucketItem
                && BucketLibUtil.isEmpty(itemStack) && bucketItem.canHoldFluid(Fluids.WATER) && getItems().get(1).getItem() != itemStack.getItem()
        ) {
            cir.setReturnValue(true);
        }
    }

}
