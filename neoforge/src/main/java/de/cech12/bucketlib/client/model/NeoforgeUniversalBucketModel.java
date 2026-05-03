package de.cech12.bucketlib.client.model;

import com.mojang.math.Transformation;
import de.cech12.bucketlib.api.BucketLibTags;
import de.cech12.bucketlib.platform.Services;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;
import net.neoforged.neoforge.client.color.item.FluidContentsTint;
import net.neoforged.neoforge.client.model.ComposedModelState;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import net.neoforged.neoforge.client.model.UnbakedElementsHelper;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public class NeoforgeUniversalBucketModel extends UniversalBucketModel {

    //workaround for https://github.com/neoforged/NeoForge/issues/3058
    public static final RenderType BLOCK_ITEM_UNSORTED_UNLIT_TRANSLUCENT = NeoForgeRenderTypes.getUnlitUnsortedTranslucent(TextureAtlas.LOCATION_BLOCKS);

    // Depth offsets to prevent Z-fighting
    private static final Transformation DEPTH_OFFSET_TRANSFORM_FLUID = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1f, 1f, 1.002f), new Quaternionf());
    private static final Transformation DEPTH_OFFSET_TRANSFORM_OTHER_CONTENT = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1.001f, 1.001f, 1.002f), new Quaternionf());

    public NeoforgeUniversalBucketModel(UniversalBucketModel.Unbaked unbakedModel, BakingContext bakingContext, Matrix4fc transformation) {
        super(unbakedModel, bakingContext, transformation);
    }

    @Override
    List<ItemModel> specialBaking(Fluid fluid, ItemTintSource bucketTint, Material.Baked baseSprite, Material.Baked otherContentSprite, Material.Baked fluidSprite, Material.Baked fluidMaskSprite, Material.Baked particleSprite) {
        ModelBaker baker = bakingContext.blockModelBaker();

        ModelState state = BlockModelRotation.IDENTITY;
        if (fluid != Fluids.EMPTY && !fluid.defaultFluidState().is(BucketLibTags.Fluids.NO_FLIPPING) && Services.FLUID.isFluidLighterThanAir(fluid)) {
            state = new ComposedModelState(state, new Transformation(null, new Quaternionf(0, 0, 1, 0), null, null));
        }

        List<ItemModel> subModels = new ArrayList<>();
        ModelRenderProperties renderProperties = new ModelRenderProperties(false, particleSprite, itemTransforms);

        if (baseSprite != null) {
            QuadCollection quads = baker.compute(new ItemModelGenerator.ItemLayerKey(baseSprite, state, 0));
            subModels.add(new CuboidItemModelWrapper(List.of(bucketTint), quads, renderProperties, transformation));
        }

        if (otherContentSprite != null) {
            var transformedState = new ComposedModelState(state, DEPTH_OFFSET_TRANSFORM_OTHER_CONTENT);
            QuadCollection quads = baker.compute(new ItemModelGenerator.ItemLayerKey(otherContentSprite, transformedState, 0));
            subModels.add(new CuboidItemModelWrapper(List.of(), quads, renderProperties, transformation));
        } else if (fluidSprite != null && fluidMaskSprite != null) {
            var transformedState = new ComposedModelState(state, DEPTH_OFFSET_TRANSFORM_FLUID);
            boolean emissive = fluid.getFluidType().getLightLevel() > 0;
            UnaryOperator<BakedQuad.MaterialInfo> materialModifier = emissive ? NeoforgeUniversalBucketModel::setMaxEmissivity : NeoforgeUniversalBucketModel::noEmissivity;
            QuadCollection quads = UnbakedElementsHelper.bakeItemMaskQuads(baker, 0, fluidMaskSprite, fluidSprite, transformedState, ExtraFaceData.DEFAULT, materialModifier); // Use template as mask
            subModels.add(new CuboidItemModelWrapper(List.of(FluidContentsTint.INSTANCE), quads, renderProperties, transformation));
        }

        return subModels;
    }

    private static BakedQuad.MaterialInfo setMaxEmissivity(BakedQuad.MaterialInfo materialInfo) {
        return new BakedQuad.MaterialInfo(
                materialInfo.sprite(),
                materialInfo.layer(),
                BLOCK_ITEM_UNSORTED_UNLIT_TRANSLUCENT,
                materialInfo.tintIndex(),
                materialInfo.shade(),
                Level.MAX_BRIGHTNESS,
                materialInfo.ambientOcclusion());
    }

    private static BakedQuad.MaterialInfo noEmissivity(BakedQuad.MaterialInfo materialInfo) {
        return new BakedQuad.MaterialInfo(
                materialInfo.sprite(),
                materialInfo.layer(),
                BLOCK_ITEM_UNSORTED_UNLIT_TRANSLUCENT,
                materialInfo.tintIndex(),
                materialInfo.shade(),
                materialInfo.lightEmission(),
                materialInfo.ambientOcclusion());
    }
}
