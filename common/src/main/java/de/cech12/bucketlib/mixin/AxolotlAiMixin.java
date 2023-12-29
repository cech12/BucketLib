package de.cech12.bucketlib.mixin;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.platform.Services;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.axolotl.AxolotlAi;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Mixin(AxolotlAi.class)
public class AxolotlAiMixin {

    @Unique
    private static Ingredient bucketLib$axolotlTemptations = null;

    @Inject(at = @At("RETURN"), method = "getTemptations", cancellable = true)
    private static void getTemptationsProxy(CallbackInfoReturnable<Ingredient> cir) {
        if (bucketLib$axolotlTemptations == null) {
            ArrayList<ItemStack> temptationStacks = new ArrayList<>();
            for (ItemStack stack : cir.getReturnValue().getItems()) {
                temptationStacks.add(stack);
                if (stack.getItem() instanceof MobBucketItem mobBucketItem) {
                    Fluid fluid = Services.BUCKET.getFluidOfBucketItem(mobBucketItem);
                    EntityType<?> entityType = Services.BUCKET.getEntityTypeOfMobBucketItem(mobBucketItem);
                    for (UniversalBucketItem bucketItem : Services.REGISTRY.getRegisteredBuckets()) {
                        if (bucketItem.canHoldFluid(fluid) && bucketItem.canHoldEntity(entityType)) {
                            ItemStack bucket = new ItemStack(bucketItem);
                            bucket = BucketLibUtil.addFluid(bucket, fluid);
                            bucket = BucketLibUtil.addEntityType(bucket, entityType);
                            temptationStacks.add(bucket);
                        }
                    }
                }
            }
            bucketLib$axolotlTemptations = Ingredient.of(temptationStacks.stream());
        }
        cir.setReturnValue(bucketLib$axolotlTemptations);
    }

}
