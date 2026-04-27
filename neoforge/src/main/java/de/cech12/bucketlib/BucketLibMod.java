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
import de.cech12.bucketlib.item.UniversalBucketFluidHandler;
import de.cech12.bucketlib.item.crafting.BucketFillingShapedRecipe;
import de.cech12.bucketlib.item.crafting.BucketFillingShapelessRecipe;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.RegistryUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
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

        RECIPE_SERIALIZERS.register("bucket_filling_shaped", () -> BucketFillingShapedRecipe.SERIALIZER);
        RECIPE_SERIALIZERS.register("bucket_filling_shapeless", () -> BucketFillingShapelessRecipe.SERIALIZER);
        INGREDIENT_TYPES.register("block", () -> BlockIngredient.TYPE);
        INGREDIENT_TYPES.register("empty", () -> EmptyIngredient.TYPE);
        INGREDIENT_TYPES.register("entity", () -> EntityIngredient.TYPE);
        INGREDIENT_TYPES.register("fluid", () -> FluidIngredient.TYPE);
        INGREDIENT_TYPES.register("milk", () -> MilkIngredient.TYPE);
    }

    //TODO remove test
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, BucketLib.MOD_ID);
    public static final DeferredHolder<Item, ?> TEST_BUCKET = ITEMS.register("test_bucket", () -> new UniversalBucketItem(ResourceKey.create(BuiltInRegistries.ITEM.key(), BucketLib.id("test_bucket")), new UniversalBucketItem.Properties()));

    private void register(RegisterCapabilitiesEvent evt) {
        BucketLibApi.registerBucket(evt, TEST_BUCKET.getId());
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

        //TODO remove test
        ITEMS.register(eventBus);
        eventBus.addListener(this::register);
    }

    public static List<UniversalBucketItem> getRegisteredBuckets() {
        return Collections.unmodifiableList(buckets);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        //Ensure that the tags are initialized
        BucketLibTags.init();
    }

    public static void processRegistration(RegisterCapabilitiesEvent event, Identifier bucketLocation) {
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
        event.registerItem(Capabilities.Fluid.ITEM, (stack, ctx) -> new UniversalBucketFluidHandler(ctx, FLUID_COMPONENT.get()), bucket);
        //register dispense behaviour
        DispenserBlock.registerBehavior(bucket, UniversalBucketDispenseBehaviour.getInstance());
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
