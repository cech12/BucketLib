package de.cech12.bucketlib;

import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.api.BucketLibTags;
import de.cech12.bucketlib.api.crafting.BlockIngredient;
import de.cech12.bucketlib.api.crafting.EmptyIngredient;
import de.cech12.bucketlib.api.crafting.EntityIngredient;
import de.cech12.bucketlib.api.crafting.FluidIngredient;
import de.cech12.bucketlib.api.crafting.MilkIngredient;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.item.UniversalBucketDispenseBehaviour;
import de.cech12.bucketlib.item.UniversalBucketFluidStorage;
import de.cech12.bucketlib.item.crafting.BucketDyeingRecipe;
import de.cech12.bucketlib.item.crafting.BucketFillingShapedRecipe;
import de.cech12.bucketlib.item.crafting.BucketFillingShapelessRecipe;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BucketLibMod implements ModInitializer {

    public static ServerLevel SERVER_LEVEL;

    static {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, new ResourceLocation(BucketLib.MOD_ID, "bucket_dyeing"), BucketDyeingRecipe.Serializer.INSTANCE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, new ResourceLocation(BucketLib.MOD_ID, "bucket_filling_shaped"), BucketFillingShapedRecipe.Serializer.INSTANCE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, new ResourceLocation(BucketLib.MOD_ID, "bucket_filling_shapeless"), BucketFillingShapelessRecipe.Serializer.INSTANCE);
        CustomIngredientSerializer.register(BlockIngredient.Serializer.INSTANCE);
        CustomIngredientSerializer.register(EmptyIngredient.Serializer.INSTANCE);
        CustomIngredientSerializer.register(EntityIngredient.Serializer.INSTANCE);
        CustomIngredientSerializer.register(FluidIngredient.Serializer.INSTANCE);
        CustomIngredientSerializer.register(MilkIngredient.Serializer.INSTANCE);
    }

    private static final List<UniversalBucketItem> BUCKETS = new ArrayList<>();

    public BucketLibMod() {
        //remember server level to have an easy getter
        ServerTickEvents.END_SERVER_TICK.register(server -> SERVER_LEVEL = server.getLevel(Level.OVERWORLD));
    }

    @Override
    public void onInitialize() {
        CommonLoader.init();
        //Ensure that the tags are initialized
        BucketLibTags.init();
    }

    public static void addBucket(UniversalBucketItem bucket) {
        BUCKETS.add(bucket);
        //register dispense behaviour
        DispenserBlock.registerBehavior(bucket, UniversalBucketDispenseBehaviour.getInstance());
        // Register bucket storage
        FluidStorage.ITEM.registerForItems((stack, context) -> {
            if (stack.getItem() instanceof UniversalBucketItem) {
                return new UniversalBucketFluidStorage(context);
            }
            return null;
        }, bucket);
    }

    public static List<UniversalBucketItem> getRegisteredBuckets() {
        return Collections.unmodifiableList(BUCKETS);
    }

}
