package de.cech12.bucketlib.client.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class UniversalBucketBakedModel implements BakedModel {

    private final List<BakedQuad> bakedQuads;
    private final ItemTransforms itemTransforms;
    private final ItemOverrides itemOverrides;
    private final TextureAtlasSprite particleSprite;

    public UniversalBucketBakedModel(List<BakedQuad> bakedQuads, ItemTransforms itemTransforms, ItemOverrides itemOverrides, TextureAtlasSprite particleSprite) {
        this.bakedQuads = bakedQuads;
        this.itemTransforms = itemTransforms;
        this.itemOverrides = itemOverrides;
        this.particleSprite = particleSprite;
    }

    @Nonnull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction direction, @Nonnull RandomSource randomSource) {
        return this.bakedQuads;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return false;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Nonnull
    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.particleSprite;
    }

    @Nonnull
    @Override
    public ItemTransforms getTransforms() {
        return this.itemTransforms;
    }

    @Nonnull
    @Override
    public ItemOverrides getOverrides() {
        return this.itemOverrides;
    }

}
