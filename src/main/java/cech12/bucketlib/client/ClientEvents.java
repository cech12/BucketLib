package cech12.bucketlib.client;

import cech12.bucketlib.api.BucketLibApi;
import cech12.bucketlib.client.model.UniversalBucketModel;
import cech12.bucketlib.util.BucketLibUtil;
import cech12.bucketlib.util.RegistryUtil;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid= BucketLibApi.MOD_ID, bus= Mod.EventBusSubscriber.Bus.MOD, value= Dist.CLIENT)
public class ClientEvents {

    private ClientEvents() {}

    @SubscribeEvent
    public static void clientSetup(ModelEvent.RegisterGeometryLoaders event) {
        event.register("universal_bucket", UniversalBucketModel.Loader.INSTANCE);
    }

    @SubscribeEvent
    public static void onTextureStitch(TextureStitchEvent.Pre event){
        if (event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS)) {
            //register texture of milk bucket
            event.addSprite(UniversalBucketModel.getContentTexture(BucketLibUtil.MILK_LOCATION));
            //register block buckets
            for (RegistryUtil.BucketBlock bucketBlock : RegistryUtil.getBucketBlocks()) {
                event.addSprite(UniversalBucketModel.getContentTexture(ForgeRegistries.BLOCKS.getKey(bucketBlock.block())));
            }
            //register textures for mob buckets
            for (RegistryUtil.BucketEntity bucketEntity : RegistryUtil.getBucketEntities()) {
                event.addSprite(UniversalBucketModel.getContentTexture(ForgeRegistries.ENTITIES.getKey(bucketEntity.entityType())));
            }
        }
    }
}
