package de.cech12.bucketlib.platform;

import de.cech12.bucketlib.client.model.NeoforgeUniversalBucketModel;
import de.cech12.bucketlib.client.model.UniversalBucketModel;
import de.cech12.bucketlib.client.services.IClientHelper;
import net.minecraft.client.renderer.item.ItemModel;
import org.joml.Matrix4fc;

public class NeoforgeClientHelper implements IClientHelper {

    @Override
    public UniversalBucketModel createItemModel(UniversalBucketModel.Unbaked unbakedModel, ItemModel.BakingContext bakingContext, Matrix4fc transformation) {
        return new NeoforgeUniversalBucketModel(unbakedModel, bakingContext, transformation);
    }

}