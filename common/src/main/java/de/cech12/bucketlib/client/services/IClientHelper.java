package de.cech12.bucketlib.client.services;

import de.cech12.bucketlib.client.model.UniversalBucketModel;
import net.minecraft.client.renderer.item.ItemModel;
import org.joml.Matrix4fc;

public interface IClientHelper {

    UniversalBucketModel createItemModel(UniversalBucketModel.Unbaked unbakedModel, ItemModel.BakingContext bakingContext, Matrix4fc transformation);

}