package de.cech12.bucketlib.client;

import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.client.model.UniversalBucketUnbakedModel;
import de.cech12.bucketlib.mixin.BlockModelAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class BucketLibClientMod implements ClientModInitializer, ModelLoadingPlugin {

    public static final ResourceLocation UNIVERSAL_BUCKET_MODEL = BucketLib.id("item/universal_bucket");

    private static final Map<ResourceLocation, UniversalBucketUnbakedModel> MODELS = new HashMap<>();

    @Override
    public void onInitializeClient() {
        ModelLoadingPlugin.register(this);
    }

    @Override
    public void initialize(Context pluginContext) {
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
