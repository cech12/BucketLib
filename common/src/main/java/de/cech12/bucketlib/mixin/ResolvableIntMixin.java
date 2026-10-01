package de.cech12.bucketlib.mixin;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.platform.Services;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin({ResolvableInt.class})
public interface ResolvableIntMixin {

    @Inject(method = "getFromItem", at = @At("RETURN"), cancellable = true)
    private static void getFromItem(ItemStack itemStack, DataComponentType<CookingFuel> componentType, Function<CookingFuel, ResolvableInt> getter, LootContext context, int defaultValue, CallbackInfoReturnable<Integer> cir) {
        if (itemStack.getItem() instanceof UniversalBucketItem)  {
            //entity buckets should not use the burn time of its fluid
            if (!BucketLibUtil.containsEntityType(itemStack)) {
                Fluid fluid = Services.FLUID.getContainedFluid(itemStack);
                if (fluid != Fluids.EMPTY) {
                    int fluidBurnTime = ResolvableInt.getFromItem(new ItemStack(fluid.getBucket()), componentType, getter, context, 0);
                    if (fluidBurnTime > 0) {
                        cir.setReturnValue(fluidBurnTime);
                        return;
                    }
                }
            }
            if (!BucketLibUtil.isEmpty(itemStack)) {
                //no burn time if the bucket is not empty
                cir.setReturnValue(0);
            }
        }
    }

}
