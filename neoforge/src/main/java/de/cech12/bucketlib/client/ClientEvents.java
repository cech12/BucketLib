package de.cech12.bucketlib.client;

import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.client.model.UniversalBucketModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;

@SuppressWarnings("unused")
@EventBusSubscriber(modid= BucketLib.MOD_ID, value= Dist.CLIENT)
public class ClientEvents {

    private ClientEvents() {}

    @SubscribeEvent
    static void registerItemModels(RegisterItemModelsEvent event) {
        event.register(BucketLib.id("universal_bucket"), UniversalBucketModel.Unbaked.MAP_CODEC);
    }

    @SubscribeEvent
    static void registerItemModels(ModelEvent.RegisterLoaders event) {
        //event.register(BucketLib.id("universal_bucket"), UniversalBucketModel.Unbaked.MAP_CODEC);
    }
}
