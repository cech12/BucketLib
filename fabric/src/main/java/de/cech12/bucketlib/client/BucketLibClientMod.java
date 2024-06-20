package de.cech12.bucketlib.client;

import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.client.model.UniversalBucketUnbakedModel;
import de.cech12.bucketlib.mixin.BlockModelAccessor;
import de.cech12.bucketlib.platform.Services;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class BucketLibClientMod implements ClientModInitializer, ModelLoadingPlugin {

    public static final ResourceLocation UNIVERSAL_BUCKET_MODEL = BucketLib.id("item/universal_bucket");

    private static final Map<ResourceLocation, UniversalBucketUnbakedModel> MODELS = new HashMap<>();

    @Override
    public void onInitializeClient() {
        ModelLoadingPlugin.register(this);
        BucketLibMod.getRegisteredBuckets().forEach(item -> {
            //register item colors
            ColorProviderRegistry.ITEM.register((stack, layer) -> {
                if (layer == 0 && item.isDyeable()) {
                    return DyedItemColor.getOrDefault(stack, item.getDefaultColor());
                }
                if (layer == 1) {
                    Fluid fluid = Services.FLUID.getContainedFluid(stack);
                    FluidRenderHandler fluidRenderHandler;
                    if (fluid != Fluids.EMPTY && (fluidRenderHandler = FluidRenderHandlerRegistry.INSTANCE.get(fluid)) != null) {
                        return FastColor.ARGB32.color(255, fluidRenderHandler.getFluidColor(null, null, fluid.defaultFluidState()));
                    }
                }
                return -1;
            }, item);
        });

    }

    @Override
    public void onInitializeModelLoader(ModelLoadingPlugin.Context pluginContext) {
        pluginContext.modifyModelOnLoad().register((original, context) -> {
            ResourceLocation location = context.resourceId();
            if (original instanceof BlockModel blockModel) {
                while (location != null) {
                    if (location.equals(UNIVERSAL_BUCKET_MODEL)) {
                        var model = new UniversalBucketUnbakedModel((BlockModel) original);
                        if (model.getParentLocation() != null) {
                            MODELS.put(((BlockModelAccessor)model).bucketlib_getParentLocation(), model);
                        }
                        return model;
                    }
                    location = blockModel != null ? ((BlockModelAccessor) blockModel).bucketlib_getParentLocation() : null;
                    blockModel = blockModel != null ?  ((BlockModelAccessor) blockModel).bucketlib_getParent() : null;
                }
            }
            return original;
        });
    }

    public static UnbakedModel getModel(ResourceLocation location) {
        return MODELS.get(location);
    }
}
