package de.cech12.bucketlib.platform.services;

import de.cech12.bucketlib.client.model.UniversalBucketModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.SpriteGetter;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

public interface IClientHelper {

    TextureAtlasSprite getFluidTextureMaterial(SpriteGetter spriteGetter, Fluid fluid);

    UniversalBucketModel createItemModel(@NotNull UniversalBucketModel.Unbaked unbakedModel, @NotNull ItemModel.BakingContext bakingContext);

}