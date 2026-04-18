package de.cech12.bucketlib.client.model;

import com.mojang.math.Transformation;
import de.cech12.bucketlib.api.BucketLibTags;
import de.cech12.bucketlib.platform.Services;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.color.item.FluidContentsTint;
import net.neoforged.neoforge.client.model.ComposedModelState;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.UnbakedElementsHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class NeoforgeUniversalBucketModel extends UniversalBucketModel {

    // Depth offsets to prevent Z-fighting
    private static final Transformation DEPTH_OFFSET_TRANSFORM = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1.001f, 1.001f, 1.002f), new Quaternionf());

    public NeoforgeUniversalBucketModel(UniversalBucketModel.Unbaked unbakedModel, BakingContext bakingContext) {
        super(unbakedModel, bakingContext);
    }

    private static RenderTypeGroup getLayerRenderTypes(boolean unlit) {
        return new RenderTypeGroup(RenderType.translucent(), unlit ? NeoForgeRenderTypes.ITEM_UNSORTED_UNLIT_TRANSLUCENT.get() : NeoForgeRenderTypes.ITEM_UNSORTED_TRANSLUCENT.get());
    }

    @Override
    List<ItemModel> specialBaking(Fluid fluid, ItemTintSource bucketTint, TextureAtlasSprite baseSprite, TextureAtlasSprite otherContentSprite, TextureAtlasSprite fluidSprite, TextureAtlasSprite fluidMaskSprite, TextureAtlasSprite particleSprite) {
        ModelState state = BlockModelRotation.X0_Y0;
        if (fluid != Fluids.EMPTY && !fluid.defaultFluidState().is(BucketLibTags.Fluids.NO_FLIPPING) && Services.FLUID.isFluidLighterThanAir(fluid)) {
            state = new ComposedModelState(state, new Transformation(null, new Quaternionf(0, 0, 1, 0), null, null));
        }

        List<ItemModel> subModels = new ArrayList<>();
        ModelRenderProperties renderProperties = new ModelRenderProperties(false, particleSprite, itemTransforms);

        var normalRenderTypes = getLayerRenderTypes(false);

        if (baseSprite != null) {
            // build base (insidest)
            var unbaked = UnbakedElementsHelper.createUnbakedItemElements(0, baseSprite);
            var quads = UnbakedElementsHelper.bakeElements(unbaked, material -> baseSprite, state);
            subModels.add(new BlockModelWrapper(List.of(bucketTint), quads, renderProperties, normalRenderTypes.entity()));
        }

        if (otherContentSprite != null) {
            var transformedState = new ComposedModelState(state, DEPTH_OFFSET_TRANSFORM);
            var unbaked = UnbakedElementsHelper.createUnbakedItemElements(0, otherContentSprite);
            var quads = UnbakedElementsHelper.bakeElements(unbaked, material -> otherContentSprite, transformedState);

            subModels.add(new BlockModelWrapper(List.of(), quads, renderProperties, normalRenderTypes.entity()));
        } else if (fluidSprite != null && fluidMaskSprite != null) {
            // build liquid layer (inside)
            var transformedState = new ComposedModelState(state, DEPTH_OFFSET_TRANSFORM);
            var unbaked = UnbakedElementsHelper.createUnbakedItemMaskElements(0, fluidMaskSprite); // Use template as mask
            var quads = UnbakedElementsHelper.bakeElements(unbaked, material -> fluidSprite, transformedState); // Bake with fluid texture

            var emissive = fluid.getFluidType().getLightLevel() > 0;
            var renderTypes = getLayerRenderTypes(emissive);
            if (emissive) QuadTransformers.settingEmissivity(fluid.getFluidType().getLightLevel()).processInPlace(quads);

            subModels.add(new BlockModelWrapper(List.of(FluidContentsTint.INSTANCE), quads, renderProperties, renderTypes.entity()));
        }

        return subModels;
    }

}
