package de.cech12.bucketlib;

import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.api.BucketLibApi;
import de.cech12.bucketlib.api.BucketLibComponents;
import de.cech12.bucketlib.api.BucketLibTags;
import de.cech12.bucketlib.api.crafting.BlockIngredient;
import de.cech12.bucketlib.api.crafting.EmptyIngredient;
import de.cech12.bucketlib.api.crafting.EntityIngredient;
import de.cech12.bucketlib.api.crafting.FluidIngredient;
import de.cech12.bucketlib.api.crafting.MilkIngredient;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.item.UniversalBucketDispenseBehaviour;
import de.cech12.bucketlib.item.crafting.BucketFillingShapedRecipe;
import de.cech12.bucketlib.item.crafting.BucketFillingShapelessRecipe;
import de.cech12.bucketlib.platform.Services;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.RegistryUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.crafting.ingredients.IIngredientSerializer;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mod(BucketLib.MOD_ID)
public class BucketLibMod {

    public static DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE.key(), BucketLib.MOD_ID);
    public static DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, BucketLib.MOD_ID);
    public static DeferredRegister<IIngredientSerializer<?>> INGREDIENT_SERIALIZERS = DeferredRegister.create(ForgeRegistries.Keys.INGREDIENT_SERIALIZERS, BucketLib.MOD_ID);

    //TODO wait until forge re-adds capability system
    public static DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID_COMPONENT = DATA_COMPONENT_TYPES.register(BucketLibComponents.FLUID_LOCATION.getPath(), () -> new DataComponentType.Builder<SimpleFluidContent>().persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC).build());

    static {
        DATA_COMPONENT_TYPES.register(BucketLibComponents.BUCKET_CONTENT_LOCATION.getPath(), () -> new DataComponentType.Builder<CustomData>().persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC).build());

        RECIPE_SERIALIZERS.register("bucket_filling_shaped", () -> BucketFillingShapedRecipe.Serializer.INSTANCE);
        RECIPE_SERIALIZERS.register("bucket_filling_shapeless", () -> BucketFillingShapelessRecipe.Serializer.INSTANCE);
        INGREDIENT_SERIALIZERS.register("block", () -> BlockIngredient.SERIALIZER);
        INGREDIENT_SERIALIZERS.register("empty", () -> EmptyIngredient.SERIALIZER);
        INGREDIENT_SERIALIZERS.register("entity", () -> EntityIngredient.SERIALIZER);
        INGREDIENT_SERIALIZERS.register("fluid", () -> FluidIngredient.SERIALIZER);
        INGREDIENT_SERIALIZERS.register("milk", () -> MilkIngredient.SERIALIZER);
    }

    private static final Logger LOGGER = LogManager.getLogger();

    private static final List<UniversalBucketItem> buckets = new ArrayList<>();

    public BucketLibMod() {
        CommonLoader.init();

        final IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.addListener(this::commonSetup);
        eventBus.addListener(this::processIMC);
        eventBus.addListener(this::addItemsToTabs);

        //dye recipe serializer
        RECIPE_SERIALIZERS.register(eventBus);
        //ingredient serializer
        INGREDIENT_SERIALIZERS.register(eventBus);
    }

    public static List<UniversalBucketItem> getRegisteredBuckets() {
        return Collections.unmodifiableList(buckets);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        //Ensure that the tags are initialized
        BucketLibTags.init();
    }

    private void processIMC(final InterModProcessEvent event) {
        event.getIMCStream().forEach(imcMessage -> {
            if (!BucketLib.MOD_ID.equals(imcMessage.modId())) {
                LOGGER.warn("Bucket could not be registered. The mod id \"{}\" of the IMCMessage is not \"{}\"", imcMessage.modId(), BucketLib.MOD_ID);
                return;
            }
            if (!BucketLibApi.REGISTER_BUCKET.equals(imcMessage.method())) {
                LOGGER.warn("Bucket could not be registered. The method \"{}\" of the IMCMessage is not \"{}\"", imcMessage.method(), BucketLibApi.REGISTER_BUCKET);
                return;
            }
            if (imcMessage.messageSupplier().get() instanceof ResourceLocation bucketLocation && bucketLocation != null) {
                Item bucketItem = ForgeRegistries.ITEMS.getValue(bucketLocation);
                if (bucketItem == null) {
                    LOGGER.info("Bucket could not be registered. The given ResourceLocation \"{}\" does not match any registered item in Forge registry.", bucketLocation);
                    return;
                }
                if (bucketItem instanceof UniversalBucketItem bucket) {
                    registerBucket(bucket);
                } else {
                    LOGGER.info("Bucket could not be registered. The item \"{}\" is not a {}.", bucketLocation, UniversalBucketItem.class.getName());
                }
            } else {
                LOGGER.warn("Bucket could not be registered. The message supplier of the IMCMessage does not contain a ResourceLocation");
            }
        });
    }

    private void registerBucket(UniversalBucketItem bucket) {
        buckets.add(bucket);
        //register dispense behaviour
        DispenserBlock.registerBehavior(bucket, UniversalBucketDispenseBehaviour.getInstance());
        //register color
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Minecraft.getInstance().getItemColors().register((stack, layer) -> {
            if (layer == 0 && bucket.isDyeable()) {
                return DyedItemColor.getOrDefault(stack, bucket.getDefaultColor());
            }
            if (layer == 1) {
                Fluid fluid = Services.FLUID.getContainedFluid(stack);
                if (fluid != Fluids.EMPTY) {
                    return IClientFluidTypeExtensions.of(fluid).getTintColor();
                }
            }
            return -1;
        }, bucket));
    }

    private void addItemsToTabs(BuildCreativeModeTabContentsEvent event) {
        buckets.forEach(bucket -> {
            if (event.getTabKey() == bucket.getCreativeTab()) {
                ItemStack emptyBucket = new ItemStack(bucket);
                //add empty bucket
                event.accept(emptyBucket);
                //add fluid buckets
                for (Fluid fluid : ForgeRegistries.FLUIDS) {
                    if (fluid == Fluids.EMPTY) {
                        continue;
                    }
                    if (ForgeMod.MILK.isPresent() && ForgeMod.MILK.get().isSame(fluid)) {
                        //skip milk fluid
                        continue;
                    }
                    if (bucket.canHoldFluid(fluid)) {
                        event.accept(BucketLibUtil.addFluid(emptyBucket, fluid));
                    }
                }
                //add milk bucket
                event.accept(BucketLibUtil.addMilk(emptyBucket));
                //add entity buckets
                for (RegistryUtil.BucketEntity bucketEntity : RegistryUtil.getBucketEntities()) {
                    if (bucket.canHoldEntity(bucketEntity.entityType()) && bucket.canHoldFluid(bucketEntity.fluid())) {
                        ItemStack filledBucket = BucketLibUtil.addFluid(emptyBucket, bucketEntity.fluid());
                        filledBucket = BucketLibUtil.addEntityType(filledBucket, bucketEntity.entityType());
                        event.accept(filledBucket);
                    }
                }
                //add block buckets
                for (RegistryUtil.BucketBlock bucketBlock : RegistryUtil.getBucketBlocks()) {
                    if (bucket.canHoldBlock(bucketBlock.block())) {
                        event.accept(BucketLibUtil.addBlock(emptyBucket, bucketBlock.block()));
                    }
                }
            }
        });
    }

}
