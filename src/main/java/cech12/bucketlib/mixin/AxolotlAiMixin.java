package cech12.bucketlib.mixin;

import cech12.bucketlib.BucketLib;
import cech12.bucketlib.api.item.UniversalBucketItem;
import cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.axolotl.AxolotlAi;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Mixin(AxolotlAi.class)
public class AxolotlAiMixin {

    private static Ingredient axolotlTemptations = null;

    @Inject(at = @At("RETURN"), method = "getTemptations", cancellable = true)
    private static void getTemptationsProxy(CallbackInfoReturnable<Ingredient> cir) {
        if (axolotlTemptations == null) {
            ArrayList<ItemStack> temptationStacks = new ArrayList<>();
            for (ItemStack stack : cir.getReturnValue().getItems()) {
                temptationStacks.add(stack);
                if (stack.getItem() instanceof MobBucketItem mobBucketItem) {
                    Fluid fluid = mobBucketItem.getFluid();
                    EntityType<?> entityType = BucketLibUtil.getEntityTypeOfMobBucketItem(mobBucketItem);
                    for (UniversalBucketItem bucketItem : BucketLib.getRegisteredBuckets()) {
                        if (bucketItem.canHoldFluid(fluid) && bucketItem.canHoldEntity(entityType)) {
                            ItemStack bucket = new ItemStack(bucketItem);
                            bucket = BucketLibUtil.addFluid(bucket, fluid);
                            bucket = BucketLibUtil.addEntityType(bucket, entityType);
                            temptationStacks.add(bucket);
                        }
                    }
                }
            }
            axolotlTemptations = Ingredient.of(temptationStacks.stream());
        }
        cir.setReturnValue(axolotlTemptations);
    }

}
