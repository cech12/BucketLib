package de.cech12.bucketlib.platform;

import de.cech12.bucketlib.client.model.NeoforgeUniversalBucketModel;
import de.cech12.bucketlib.client.model.UniversalBucketModel;
import de.cech12.bucketlib.platform.services.IClientHelper;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.SpriteGetter;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

public class NeoforgeClientHelper implements IClientHelper {

    @Override
    public TextureAtlasSprite getFluidTextureMaterial(SpriteGetter spriteGetter, Fluid fluid) {
        return spriteGetter.get(ClientHooks.getBlockMaterial(IClientFluidTypeExtensions.of(fluid).getStillTexture()));
    }

    @Override
    public UniversalBucketModel createItemModel(UniversalBucketModel.Unbaked unbakedModel, ItemModel.BakingContext bakingContext, ItemTransforms itemTransforms) {
        return new NeoforgeUniversalBucketModel(unbakedModel, bakingContext, itemTransforms);
    }

}