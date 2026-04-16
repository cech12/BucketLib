package de.cech12.bucketlib.platform;

import de.cech12.bucketlib.client.model.FabricUniversalBucketModel;
import de.cech12.bucketlib.client.model.UniversalBucketModel;
import de.cech12.bucketlib.platform.services.IClientHelper;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.SpriteGetter;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

public class FabricClientHelper implements IClientHelper {

    @Override
    public TextureAtlasSprite getFluidTextureMaterial(SpriteGetter spriteGetter, Fluid fluid) {
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
    public UniversalBucketModel createItemModel(@NotNull UniversalBucketModel.Unbaked unbakedModel, @NotNull ItemModel.BakingContext bakingContext) {
        return new FabricUniversalBucketModel(unbakedModel, bakingContext);
    }

}