package de.cech12.bucketlib;

import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.api.BucketLibComponents;
import de.cech12.bucketlib.api.BucketLibTags;
import de.cech12.bucketlib.api.crafting.BlockIngredient;
import de.cech12.bucketlib.api.crafting.EmptyIngredient;
import de.cech12.bucketlib.api.crafting.EntityIngredient;
import de.cech12.bucketlib.api.crafting.FluidIngredient;
import de.cech12.bucketlib.api.crafting.MilkIngredient;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.item.UniversalBucketDispenseBehaviour;
import de.cech12.bucketlib.item.UniversalBucketFluidHandler;
import de.cech12.bucketlib.item.crafting.BucketFillingShapedRecipe;
import de.cech12.bucketlib.item.crafting.BucketFillingShapelessRecipe;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.RegistryUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Mod(BucketLib.MOD_ID)
public class BucketLibMod {

    public static DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, BucketLib.MOD_ID);
    public static DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, BucketLib.MOD_ID);
    public static DeferredRegister<IngredientType<?>> INGREDIENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, BucketLib.MOD_ID);

    public static DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID_COMPONENT = DATA_COMPONENT_TYPES.register(BucketLibComponents.FLUID_LOCATION.getPath(), () -> new DataComponentType.Builder<SimpleFluidContent>().persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC).build());

    static {
        DATA_COMPONENT_TYPES.register(BucketLibComponents.BUCKET_CONTENT_LOCATION.getPath(), () -> BucketLibComponents.BUCKET_CONTENT);

        RECIPE_SERIALIZERS.register("bucket_filling_shaped", () -> BucketFillingShapedRecipe.Serializer.INSTANCE);
        RECIPE_SERIALIZERS.register("bucket_filling_shapeless", () -> BucketFillingShapelessRecipe.Serializer.INSTANCE);
        INGREDIENT_TYPES.register("block", () -> BlockIngredient.TYPE);
        INGREDIENT_TYPES.register("empty", () -> EmptyIngredient.TYPE);
        INGREDIENT_TYPES.register("entity", () -> EntityIngredient.TYPE);
        INGREDIENT_TYPES.register("fluid", () -> FluidIngredient.TYPE);
        INGREDIENT_TYPES.register("milk", () -> MilkIngredient.TYPE);
    }

    private static final Logger LOGGER = LogManager.getLogger();

    private static final List<UniversalBucketItem> buckets = new ArrayList<>();

    public BucketLibMod(IEventBus eventBus) {
        CommonLoader.init();

        eventBus.addListener(this::commonSetup);
        eventBus.addListener(this::addItemsToTabs);

        //data components
        DATA_COMPONENT_TYPES.register(eventBus);
        //dye recipe serializer
        RECIPE_SERIALIZERS.register(eventBus);
        //ingredient serializer
        INGREDIENT_TYPES.register(eventBus);
    }

    public static List<UniversalBucketItem> getRegisteredBuckets() {
        return Collections.unmodifiableList(buckets);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        //Ensure that the tags are initialized
        BucketLibTags.init();
    }

    public static void processRegistration(RegisterCapabilitiesEvent event, ResourceLocation bucketLocation) {
        if (bucketLocation != null) {
            Optional<Item> bucketItem = BuiltInRegistries.ITEM.getOptional(bucketLocation);
            if (bucketItem.isEmpty()) {
                LOGGER.info("Bucket could not be registered. The given ResourceLocation \"{}\" does not match any registered item in Forge registry.", bucketLocation);
                return;
            }
            if (bucketItem.get() instanceof UniversalBucketItem bucket) {
                registerBucket(event, bucket);
            } else {
                LOGGER.info("Bucket could not be registered. The item \"{}\" is not a {}.", bucketLocation, UniversalBucketItem.class.getName());
            }
        } else {
            LOGGER.warn("Bucket could not be registered. The message supplier of the IMCMessage does not contain a ResourceLocation");
        }
    }

    private static void registerBucket(RegisterCapabilitiesEvent event, UniversalBucketItem bucket) {
        buckets.add(bucket);
        //register item capability
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> new UniversalBucketFluidHandler(stack), bucket);
        //register dispense behaviour
        DispenserBlock.registerBehavior(bucket, UniversalBucketDispenseBehaviour.getInstance());
        //register color
        if (FMLEnvironment.dist.isClient()) {
            Minecraft.getInstance().getItemColors().register((stack, layer) -> {
                if (layer == 0 && bucket.isDyeable()) {
                    return DyedItemColor.getOrDefault(stack, bucket.getDefaultColor());
                }
                if (layer == 1) {
                    return FluidUtil.getFluidContained(stack)
                            .map(fluidStack -> IClientFluidTypeExtensions.of(fluidStack.getFluid()).getTintColor(fluidStack))
                            .orElse(-1);
                }
                return -1;
            }, bucket);
        }
    }

    private void addItemsToTabs(BuildCreativeModeTabContentsEvent event) {
        buckets.forEach(bucket -> {
            if (event.getTabKey() == bucket.getCreativeTab()) {
                ItemStack emptyBucket = new ItemStack(bucket);
                //add empty bucket
                event.accept(emptyBucket);
                //add fluid buckets
                for (Fluid fluid : BuiltInRegistries.FLUID) {
                    if (fluid == Fluids.EMPTY) {
                        continue;
                    }
                    if (NeoForgeMod.MILK.isBound() && NeoForgeMod.MILK.get().isSame(fluid)) {
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
