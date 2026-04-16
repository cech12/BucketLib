package de.cech12.bucketlib.mixin;

import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.client.model.UniversalBucketModel;
import net.minecraft.client.renderer.item.ItemModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModels.class)
public class ItemModelsMixin {

    @Inject(method = "bootstrap", at = @At("RETURN"))
    private static void bootstrapProxy(CallbackInfo ci) {
        ItemModels.ID_MAPPER.put(BucketLib.id("universal_bucket"), UniversalBucketModel.Unbaked.MAP_CODEC);
    }

}