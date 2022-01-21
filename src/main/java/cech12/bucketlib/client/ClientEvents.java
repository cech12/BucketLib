package cech12.bucketlib.client;

import cech12.bucketlib.api.BucketLibApi;
import cech12.bucketlib.client.model.UniversalBucketModel;
import cech12.bucketlib.util.BucketLibUtil;
import cech12.bucketlib.util.EntityUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid= BucketLibApi.MOD_ID, bus= Mod.EventBusSubscriber.Bus.MOD, value= Dist.CLIENT)
public class ClientEvents {

    private ClientEvents() {}

    @SubscribeEvent
    public static void clientSetup(ModelRegistryEvent event) {
        ModelLoaderRegistry.registerLoader(new ResourceLocation(BucketLibApi.MOD_ID, "universal_bucket"), UniversalBucketModel.Loader.INSTANCE);
    }

    @SubscribeEvent
    public static void onTextureStitch(TextureStitchEvent.Pre event){
        if (event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS)) {
            //TODO powder snow bucket & other mod contents
            //register texture of milk bucket
            event.addSprite(UniversalBucketModel.getContentTexture(BucketLibUtil.MILK_LOCATION));
            //register textures for mob buckets
            for (EntityUtil.BucketEntity bucketEntity : EntityUtil.getBucketEntities()) {
                event.addSprite(UniversalBucketModel.getContentTexture(bucketEntity.getEntityType().getRegistryName()));
            }
        }
    }
}
