package de.cech12.bucketlib.mixin;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.RegistryUtil;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.axolotl.AxolotlAi;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(AxolotlAi.class)
public class AxolotlAiMixin {

    @Unique
    private static final Predicate<ItemStack> bucketLib$predicate = stack -> {
        EntityType<?> entityType;
        RegistryUtil.BucketEntity bucketEntity;
        if (stack.getItem() instanceof UniversalBucketItem
                && (entityType = BucketLibUtil.getEntityType(stack)) != null
                && (bucketEntity = RegistryUtil.getBucketEntity(entityType)) != null) {
            return new ItemStack(bucketEntity.bucketItem()).is(ItemTags.AXOLOTL_FOOD);
        }
        return false;
    };

    @Inject(at = @At("RETURN"), method = "getTemptations", cancellable = true)
    private static void getTemptationsProxy(CallbackInfoReturnable<Predicate<ItemStack>> cir) {
        cir.setReturnValue(cir.getReturnValue().or(bucketLib$predicate));
    }

}
