package de.cech12.bucketlib.mixin;

import de.cech12.bucketlib.CommonLoader;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractFurnaceMenu.class})
public class AbstractFurnaceMenuMixin {

    @Final
    @Shadow
    private Container container;

    @Inject(at = @At("RETURN"), method = "isFuel", cancellable = true)
    private void isFuelProxy(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (!itemStack.isEmpty() && itemStack.getItem() instanceof UniversalBucketItem
                && ResolvableInt.getFromItem(itemStack, DataComponents.COOKING_FUEL, CookingFuel::burnTime, CommonLoader.createLootContext(container), 0) <= 0) {
            cir.setReturnValue(false);
        }
    }

}
