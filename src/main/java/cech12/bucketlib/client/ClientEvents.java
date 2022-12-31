package cech12.bucketlib.client;

import cech12.bucketlib.api.BucketLibApi;
import cech12.bucketlib.client.model.UniversalBucketModel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid= BucketLibApi.MOD_ID, bus= Mod.EventBusSubscriber.Bus.MOD, value= Dist.CLIENT)
public class ClientEvents {

    private ClientEvents() {}

    @SubscribeEvent
    public static void clientSetup(ModelEvent.RegisterGeometryLoaders event) {
        event.register("universal_bucket", UniversalBucketModel.Loader.INSTANCE);
    }

}
