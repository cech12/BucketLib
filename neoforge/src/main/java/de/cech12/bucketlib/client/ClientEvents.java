package de.cech12.bucketlib.client;

import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.client.model.NeoforgeUniversalBucketModel;
import de.cech12.bucketlib.client.model.UniversalBucketModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;

@SuppressWarnings("unused")
@EventBusSubscriber(modid= BucketLib.MOD_ID, value= Dist.CLIENT)
public class ClientEvents {

    private ClientEvents() {}

    @SubscribeEvent
    static void registerItemModels(RegisterItemModelsEvent event) {
        event.register(BucketLib.id("universal_bucket"), UniversalBucketModel.Unbaked.MAP_CODEC);
    }

    //workaround for https://github.com/neoforged/NeoForge/issues/3058
    @SubscribeEvent
    static void registerRenderTypes(RegisterRenderBuffersEvent event) {
        event.registerRenderBuffer(NeoforgeUniversalBucketModel.BLOCK_ITEM_UNSORTED_TRANSLUCENT);
        event.registerRenderBuffer(NeoforgeUniversalBucketModel.BLOCK_ITEM_UNSORTED_UNLIT_TRANSLUCENT);
    }

}
