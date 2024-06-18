package de.cech12.bucketlib.client;

import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.client.model.UniversalBucketModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

@SuppressWarnings("unused")
@EventBusSubscriber(modid= BucketLib.MOD_ID, bus= EventBusSubscriber.Bus.MOD, value= Dist.CLIENT)
public class ClientEvents {

    private ClientEvents() {}

    @SubscribeEvent
    public static void clientSetup(ModelEvent.RegisterGeometryLoaders event) {
        event.register(new ResourceLocation(BucketLib.MOD_ID, "universal_bucket"), UniversalBucketModel.Loader.INSTANCE);
    }

}
