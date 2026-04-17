package de.cech12.bucketlib.client;

import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.client.model.UniversalBucketModel;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.item.ItemModels;

@SuppressWarnings("unused")
public class BucketLibClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ItemModels.ID_MAPPER.put(BucketLib.id("universal_bucket"), UniversalBucketModel.Unbaked.MAP_CODEC);
    }

}
