package de.cech12.bucketlib.client.model;

import com.mojang.math.Transformation;
import de.cech12.bucketlib.api.BucketLibTags;
import de.cech12.bucketlib.client.color.BucketFluidTint;
import de.cech12.bucketlib.client.model.neoforge.ComposedModelState;
import de.cech12.bucketlib.client.model.neoforge.UnbakedElementsHelper;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class FabricUniversalBucketModel extends UniversalBucketModel {

    // Depth offsets to prevent Z-fighting
    private static final Transformation DEPTH_OFFSET_TRANSFORM = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1.001f, 1.001f, 1.002f), new Quaternionf());

    public FabricUniversalBucketModel(UniversalBucketModel.Unbaked unbakedModel, BakingContext bakingContext) {
        super(unbakedModel, bakingContext);
    }

    @Override
    List<ItemModel> specialBaking(Fluid fluid, ItemTintSource bucketTint, TextureAtlasSprite baseSprite, TextureAtlasSprite otherContentSprite, TextureAtlasSprite fluidSprite, TextureAtlasSprite fluidMaskSprite, TextureAtlasSprite particleSprite) {
        ModelState state = BlockModelRotation.X0_Y0;
        // if the fluid is lighter than air, will manipulate the initial state to be rotated 180deg to turn it upside down
        if (fluid != Fluids.EMPTY && !fluid.defaultFluidState().is(BucketLibTags.Fluids.NO_FLIPPING) && FluidVariantAttributes.isLighterThanAir(FluidVariant.of(fluid))) {
            state = BlockModelRotation.X180_Y0;
        }

        List<ItemModel> subModels = new ArrayList<>();
        ModelRenderProperties renderProperties = new ModelRenderProperties(false, particleSprite, itemTransforms);

        if (baseSprite != null) {
            // build base (insidest)
            var unbaked = UnbakedElementsHelper.createUnbakedItemElements(0, baseSprite);
            var quads = UnbakedElementsHelper.bakeElements(unbaked, material -> baseSprite, state);
            subModels.add(new BlockModelWrapper(List.of(bucketTint), quads, renderProperties));
        }
        if (otherContentSprite != null) {
            var transformedState = new ComposedModelState(state, DEPTH_OFFSET_TRANSFORM);
            var unbaked = UnbakedElementsHelper.createUnbakedItemElements(0, otherContentSprite);
            var quads = UnbakedElementsHelper.bakeElements(unbaked, material -> otherContentSprite, transformedState);
            subModels.add(new BlockModelWrapper(List.of(), quads, renderProperties));
        } else if (fluidSprite != null && fluidMaskSprite != null) {
            var transformedState = new ComposedModelState(state, DEPTH_OFFSET_TRANSFORM);
            var unbaked = UnbakedElementsHelper.createUnbakedItemMaskElements(0, fluidMaskSprite); // Use template as mask
            var quads = UnbakedElementsHelper.bakeElements(unbaked, material -> fluidSprite, transformedState); // Bake with fluid texture
            subModels.add(new BlockModelWrapper(List.of(BucketFluidTint.INSTANCE), quads, renderProperties));
        }

        return subModels;
    }

}