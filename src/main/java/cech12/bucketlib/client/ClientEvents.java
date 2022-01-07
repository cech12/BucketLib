package cech12.bucketlib.client;

import cech12.bucketlib.api.BucketLibApi;
import cech12.bucketlib.client.model.UniversalBucketModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelRegistryEvent;
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

}
