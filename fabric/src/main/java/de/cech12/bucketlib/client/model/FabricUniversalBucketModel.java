package de.cech12.bucketlib.client.model;

import de.cech12.bucketlib.api.BucketLibTags;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.color.item.Constant;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.SpriteGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FabricUniversalBucketModel extends UniversalBucketModel {

    public FabricUniversalBucketModel(@NotNull UniversalBucketModel.Unbaked unbakedModel, @NotNull BakingContext bakingContext, ItemTransforms itemTransforms) {
        super(unbakedModel, bakingContext, itemTransforms);
    }

    @Override
    ItemModel specialBaking(SpriteGetter spriteGetter, Fluid fluid, TextureAtlasSprite baseSprite, TextureAtlasSprite otherContentSprite, TextureAtlasSprite fluidSprite, TextureAtlasSprite particleSprite, Material fluidMaskLocation) {
        ModelState modelState = BlockModelRotation.X0_Y0;
        // if the fluid is lighter than air, will manipulate the initial state to be rotated 180deg to turn it upside down
        if (fluid != Fluids.EMPTY && !fluid.defaultFluidState().is(BucketLibTags.Fluids.NO_FLIPPING) && FluidVariantAttributes.isLighterThanAir(FluidVariant.of(fluid))) {
            modelState = BlockModelRotation.X180_Y0;
        }

        List<BakedQuad> quads = new ArrayList<>();

        if (baseSprite != null) {
            quads.addAll(GeometryUtils.bakeElements(itemTransforms,
                    GeometryUtils.createUnbakedItemElements(0, "base", baseSprite.contents()),
                    baseSprite, modelState));
        }
        if (otherContentSprite != null) {
            quads.addAll(GeometryUtils.bakeElements(itemTransforms,
                    GeometryUtils.createUnbakedItemElements(-1, "content", otherContentSprite.contents()),
                    otherContentSprite, modelState));
        } else if (fluidMaskLocation != null && fluidSprite != null) {
            TextureAtlasSprite templateSprite = spriteGetter.get(fluidMaskLocation);
            quads.addAll(GeometryUtils.bakeElements(itemTransforms,
                    GeometryUtils.createUnbakedItemMaskElements(1, "fluid", templateSprite.contents()),
                    fluidSprite, modelState));
        }

        BakedModel bakedModel = new UniversalBucketBakedModel(quads, itemTransforms, particleSprite);

        return new BlockModelWrapper(bakedModel, List.of(new Constant(-1)));
    }

}