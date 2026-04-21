package de.cech12.bucketlib.client;

import de.cech12.bucketlib.client.model.FabricUniversalBucketModel;
import de.cech12.bucketlib.client.model.UniversalBucketModel;
import de.cech12.bucketlib.client.services.IClientHelper;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.SpriteGetter;
import net.minecraft.world.level.material.Fluid;

public class FabricClientHelper implements IClientHelper {

    @Override
    public TextureAtlasSprite getFluidTextureMaterial(SpriteGetter spriteGetter, ModelDebugName debugName, Fluid fluid) {
        FluidRenderHandler renderHandler = FluidRenderHandlerRegistry.INSTANCE.get(fluid);
        if (renderHandler == null) {
            return null;
        }
        TextureAtlasSprite[] sprites = renderHandler.getFluidSprites(null, null, fluid.defaultFluidState());
        if (sprites.length > 0) {
            return sprites[0];
        }
        return null;
    }

    @Override
    public UniversalBucketModel createItemModel(UniversalBucketModel.Unbaked unbakedModel, ItemModel.BakingContext bakingContext) {
        return new FabricUniversalBucketModel(unbakedModel, bakingContext);
    }

}