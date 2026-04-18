package de.cech12.bucketlib.mixin;

import de.cech12.bucketlib.api.item.UniversalBucketItem;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemBlockRenderTypes.class)
public class ItemBlockRenderTypesMixin {

    // avoid transparent fluid textures let you look through the bucket base texture
    @Unique
    private static final RenderType UNSORTED_TRANSLUCENT = RenderType.create("bucketlib_entity_unsorted_translucent", 256, true, false, RenderPipelines.ENTITY_TRANSLUCENT, RenderType.CompositeState.builder()
            .setTextureState(new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false))
            .setLightmapState(RenderType.LIGHTMAP)
            .setOverlayState(RenderType.OVERLAY)
            .createCompositeState(true));

    @Inject(method = "getRenderType(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/client/renderer/RenderType;", at = @At("RETURN"), cancellable = true)
    private static void getRenderTypeProxy(ItemStack itemStack, CallbackInfoReturnable<RenderType> cir) {
        if (itemStack.getItem() instanceof UniversalBucketItem)  {
            cir.setReturnValue(UNSORTED_TRANSLUCENT);
        }
    }

}
