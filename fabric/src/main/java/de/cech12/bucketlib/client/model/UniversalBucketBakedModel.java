package de.cech12.bucketlib.client.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class UniversalBucketBakedModel implements BakedModel {

    private final List<BakedQuad> bakedQuads;
    private final ItemTransforms itemTransforms;
    private final TextureAtlasSprite particleSprite;

    public UniversalBucketBakedModel(List<BakedQuad> bakedQuads, ItemTransforms itemTransforms, TextureAtlasSprite particleSprite) {
        this.bakedQuads = bakedQuads;
        this.itemTransforms = itemTransforms;
        this.particleSprite = particleSprite;
    }

    @NotNull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction direction, @NotNull RandomSource randomSource) {
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

    @NotNull
    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.particleSprite;
    }

    @NotNull
    @Override
    public ItemTransforms getTransforms() {
        return this.itemTransforms;
    }


}
