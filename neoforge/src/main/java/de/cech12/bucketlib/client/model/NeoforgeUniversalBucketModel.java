package de.cech12.bucketlib.client.model;

import com.mojang.math.Transformation;
import de.cech12.bucketlib.api.BucketLibTags;
import de.cech12.bucketlib.platform.Services;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;
import net.neoforged.neoforge.client.color.item.FluidContentsTint;
import net.neoforged.neoforge.client.model.ComposedModelState;
import net.neoforged.neoforge.client.model.UnbakedElementsHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class NeoforgeUniversalBucketModel extends UniversalBucketModel {

    // Depth offsets to prevent Z-fighting
    private static final Transformation DEPTH_OFFSET_TRANSFORM = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1.001f, 1.001f, 1.002f), new Quaternionf());

    public NeoforgeUniversalBucketModel(UniversalBucketModel.Unbaked unbakedModel, BakingContext bakingContext) {
        super(unbakedModel, bakingContext);
    }

    @Override
    List<ItemModel> specialBaking(Fluid fluid, ItemTintSource bucketTint, TextureAtlasSprite baseSprite, TextureAtlasSprite otherContentSprite, TextureAtlasSprite fluidSprite, TextureAtlasSprite fluidMaskSprite, TextureAtlasSprite particleSprite) {
        ModelState state = BlockModelRotation.IDENTITY;
        if (fluid != Fluids.EMPTY && !fluid.defaultFluidState().is(BucketLibTags.Fluids.NO_FLIPPING) && Services.FLUID.isFluidLighterThanAir(fluid)) {
            state = new ComposedModelState(state, new Transformation(null, new Quaternionf(0, 0, 1, 0), null, null));
        }

        List<ItemModel> subModels = new ArrayList<>();
        ModelRenderProperties renderProperties = new ModelRenderProperties(false, particleSprite, itemTransforms);

        Function<ItemStack, RenderType> normalRenderType = (stack) -> NeoForgeRenderTypes.ITEM_UNSORTED_TRANSLUCENT.get();

        if (baseSprite != null) {
            // build base (insidest)
            var unbaked = UnbakedElementsHelper.createUnbakedItemElements(0, baseSprite);
            var quads = UnbakedElementsHelper.bakeElements(unbaked, material -> baseSprite, state);
            subModels.add(new BlockModelWrapper(List.of(bucketTint), quads, renderProperties, normalRenderType));
        }

        if (otherContentSprite != null) {
            var transformedState = new ComposedModelState(state, DEPTH_OFFSET_TRANSFORM);
            var unbaked = UnbakedElementsHelper.createUnbakedItemElements(0, otherContentSprite);
            var quads = UnbakedElementsHelper.bakeElements(unbaked, material -> otherContentSprite, transformedState);

            subModels.add(new BlockModelWrapper(List.of(), quads, renderProperties, normalRenderType));
        } else if (fluidSprite != null && fluidMaskSprite != null) {
            // build liquid layer (inside)
            var transformedState = new ComposedModelState(state, DEPTH_OFFSET_TRANSFORM);
            var unbaked = UnbakedElementsHelper.createUnbakedItemMaskElements(0, fluidMaskSprite); // Use template as mask
            var quads = UnbakedElementsHelper.bakeElements(unbaked, material -> fluidSprite, transformedState); // Bake with fluid texture

            var emissive = fluid.getFluidType().getLightLevel() > 0;
            Function<ItemStack, RenderType> renderType = (stack) -> emissive ? NeoForgeRenderTypes.BLOCK_ITEM_UNLIT_TRANSLUCENT.get() : NeoForgeRenderTypes.BLOCK_ITEM_UNSORTED_TRANSLUCENT.get();
            if (emissive) {
                quads = new ArrayList<>(quads);
                quads.replaceAll(NeoforgeUniversalBucketModel::setMaxEmissivity);
            }

            subModels.add(new BlockModelWrapper(List.of(FluidContentsTint.INSTANCE), quads, renderProperties, renderType));
        }

        return subModels;
    }

    private static BakedQuad setMaxEmissivity(BakedQuad quad) {
        return new BakedQuad(
                quad.position0(),
                quad.position1(),
                quad.position2(),
                quad.position3(),
                quad.packedUV0(),
                quad.packedUV1(),
                quad.packedUV2(),
                quad.packedUV3(),
                quad.tintIndex(),
                quad.direction(),
                quad.sprite(),
                quad.shade(),
                Level.MAX_BRIGHTNESS,
                quad.bakedNormals(),
                quad.bakedColors(),
                quad.hasAmbientOcclusion());
    }
}
