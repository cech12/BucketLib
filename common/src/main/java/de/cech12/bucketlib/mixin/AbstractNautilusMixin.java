package de.cech12.bucketlib.mixin;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.RegistryUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractNautilus.class})
public abstract class AbstractNautilusMixin extends TamableAnimal {

	private AbstractNautilusMixin(EntityType<? extends TamableAnimal> entityType, Level level) {
		super(entityType, level);
	}

	@Inject(at = @At("RETURN"), method = "isFood", cancellable = true)
	private void isFoodProxy(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue() && stack.getItem() instanceof UniversalBucketItem) {
			EntityType<?> entityType;
			RegistryUtil.BucketEntity bucketEntity;
			if ((entityType = BucketLibUtil.getEntityType(stack)) != null
					&& (bucketEntity = RegistryUtil.getBucketEntity(entityType)) != null) {
				TagKey<Item> checkTag = !this.isTame() && !this.isBaby() ? ItemTags.NAUTILUS_TAMING_ITEMS : ItemTags.NAUTILUS_FOOD;
				cir.setReturnValue(new ItemStack(bucketEntity.bucketItem()).is(checkTag));
			}
		}
	}

	@Inject(at = @At("HEAD"), method = "usePlayerItem", cancellable = true)
	private void usePlayerItemProxy(Player player, InteractionHand hand, ItemStack stack, CallbackInfo ci) {
		if (this.isFood(stack) && stack.getItem() instanceof UniversalBucketItem) {
			ServerLevel serverLevel = (player.level() instanceof ServerLevel) ? (ServerLevel) player.level() : null;
			player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, BucketLibUtil.removeEntityData(stack, serverLevel, player, true)));
			ci.cancel();
		}
	}

}
