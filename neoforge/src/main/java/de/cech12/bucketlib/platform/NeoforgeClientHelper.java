package de.cech12.bucketlib.platform;

import de.cech12.bucketlib.client.model.NeoforgeUniversalBucketModel;
import de.cech12.bucketlib.client.model.UniversalBucketModel;
import de.cech12.bucketlib.client.services.IClientHelper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.SpriteGetter;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

public class NeoforgeClientHelper implements IClientHelper {

    @Override
    public TextureAtlasSprite getFluidTextureMaterial(SpriteGetter spriteGetter, ModelDebugName debugName, Fluid fluid) {
        return spriteGetter.get(ClientHooks.getBlockMaterial(IClientFluidTypeExtensions.of(fluid).getStillTexture()), debugName);
    }

    @Override
    public UniversalBucketModel createItemModel(UniversalBucketModel.Unbaked unbakedModel, ItemModel.BakingContext bakingContext) {
        return new NeoforgeUniversalBucketModel(unbakedModel, bakingContext);
    }

}