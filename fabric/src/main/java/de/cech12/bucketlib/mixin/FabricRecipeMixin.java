package de.cech12.bucketlib.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.ArmorDyeRecipe;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({Recipe.class})
public interface FabricRecipeMixin {

    @ModifyExpressionValue(
            method = "getRemainingItems",
            at = @At(value = "INVOKE", target ="Lnet/minecraft/world/item/Item;hasCraftingRemainingItem()Z"))
    private boolean hasCraftingRemainingItemProxy(boolean original, @Local Item item) {
        if ((Object) this instanceof ArmorDyeRecipe && item instanceof UniversalBucketItem) {
            return false; //avoid bucket duplication when coloring
        }
        return original;
    }

}
