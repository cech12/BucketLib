package de.cech12.bucketlib.client.model;

import com.mojang.math.Transformation;
import de.cech12.bucketlib.api.BucketLibTags;
import de.cech12.bucketlib.platform.Services;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.SpriteGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.SimpleModelState;
import net.neoforged.neoforge.client.model.UnbakedCompositeModel;
import net.neoforged.neoforge.client.model.UnbakedElementsHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class NeoforgeUniversalBucketModel extends UniversalBucketModel {

    // Depth offsets to prevent Z-fighting
    private static final Transformation DEPTH_OFFSET_TRANSFORM = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1.002f), new Quaternionf());

    public NeoforgeUniversalBucketModel(UniversalBucketModel.Unbaked unbakedModel, BakingContext bakingContext, BakedModel baseModel) {
        super(unbakedModel, bakingContext, baseModel);
    }

    private static RenderTypeGroup getLayerRenderTypes(boolean unlit) {
        return new RenderTypeGroup(RenderType.translucent(), unlit ? NeoForgeRenderTypes.ITEM_UNSORTED_UNLIT_TRANSLUCENT.get() : NeoForgeRenderTypes.ITEM_UNSORTED_TRANSLUCENT.get());
    }

    @Override
    BakedModel specialBaking(SpriteGetter spriteGetter, Fluid fluid, TextureAtlasSprite baseSprite, TextureAtlasSprite otherContentSprite, TextureAtlasSprite fluidSprite, TextureAtlasSprite particleSprite, Material fluidMaskLocation) {
        var sprites = bakingContext.blockModelBaker().sprites();

        ModelState state = BlockModelRotation.X0_Y0;
        if (fluid != Fluids.EMPTY && !fluid.defaultFluidState().is(BucketLibTags.Fluids.NO_FLIPPING) && Services.FLUID.isFluidLighterThanAir(fluid)) {
            state = new SimpleModelState(
                    state.getRotation().compose(
                            new Transformation(null, new Quaternionf(0, 0, 1, 0), null, null)));
        }

        // We need to disable GUI 3D and block lighting for this to render properly
        var modelBuilder = UnbakedCompositeModel.Baked.builder(baseModel.useAmbientOcclusion(), baseModel.isGui3d(), false, particleSprite, baseModel.getTransforms());

        var normalRenderTypes = getLayerRenderTypes(false);

        if (baseSprite != null) {
            // build base (insidest)
            var unbaked = UnbakedElementsHelper.createUnbakedItemElements(0, baseSprite);
            var quads = UnbakedElementsHelper.bakeElements(unbaked, material -> baseSprite, state);
            modelBuilder.addQuads(normalRenderTypes, quads);
        }

        if (otherContentSprite != null) {
            //layer 2 to avoid coloring the entity layer
            var transformedState = new SimpleModelState(state.getRotation().compose(DEPTH_OFFSET_TRANSFORM), state.isUvLocked());
            var unbaked = UnbakedElementsHelper.createUnbakedItemElements(2, otherContentSprite);
            TextureAtlasSprite finalOtherContentSprite = otherContentSprite;
            var quads = UnbakedElementsHelper.bakeElements(unbaked, material -> finalOtherContentSprite, transformedState);
            modelBuilder.addQuads(normalRenderTypes, quads);
        } else if (fluidMaskLocation != null && fluidSprite != null) {
            TextureAtlasSprite templateSprite = sprites.get(fluidMaskLocation);
            // build liquid layer (inside)
            var transformedState = new SimpleModelState(state.getRotation().compose(DEPTH_OFFSET_TRANSFORM), state.isUvLocked());
            var unbaked = UnbakedElementsHelper.createUnbakedItemMaskElements(1, templateSprite); // Use template as mask
            var quads = UnbakedElementsHelper.bakeElements(unbaked, material -> fluidSprite, transformedState); // Bake with fluid texture

            var emissive = fluid.getFluidType().getLightLevel() > 0;
            var renderTypes = getLayerRenderTypes(emissive);
            if (emissive) QuadTransformers.settingEmissivity(fluid.getFluidType().getLightLevel()).processInPlace(quads);

            modelBuilder.addQuads(renderTypes, quads);
        }

        modelBuilder.setParticle(particleSprite);

        return modelBuilder.build();
    }

}
