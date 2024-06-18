package de.cech12.bucketlib.mixin;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.RegistryUtil;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Axolotl.class})
public abstract class AxolotlMixin extends Animal {

	private AxolotlMixin(EntityType<? extends Animal> entityType, Level level) {
		super(entityType, level);
	}

	@Inject(at = @At("RETURN"), method = "isFood", cancellable = true)
	private void isFoodProxy(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue() && stack.getItem() instanceof UniversalBucketItem) {
			EntityType<?> entityType;
			RegistryUtil.BucketEntity bucketEntity;
			if ((entityType = BucketLibUtil.getEntityType(stack)) != null
					&& (bucketEntity = RegistryUtil.getBucketEntity(entityType)) != null) {
				cir.setReturnValue(new ItemStack(bucketEntity.bucketItem()).is(ItemTags.AXOLOTL_FOOD));
			}
		}
	}

	@Inject(at = @At("HEAD"), method = "usePlayerItem", cancellable = true)
	private void usePlayerItemProxy(Player player, InteractionHand hand, ItemStack stack, CallbackInfo ci) {
		if (this.isFood(stack) && stack.getItem() instanceof UniversalBucketItem) {
			player.setItemInHand(hand, BucketLibUtil.removeEntityType(stack, true));
			ci.cancel();
		}
	}

}
