package de.cech12.bucketlib;

import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.api.BucketLibApi;
import de.cech12.bucketlib.api.BucketLibTags;
import de.cech12.bucketlib.api.crafting.FluidIngredient;
import de.cech12.bucketlib.api.crafting.MilkIngredient;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.item.UniversalBucketDispenseBehaviour;
import de.cech12.bucketlib.item.crafting.BucketDyeingRecipe;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.ColorUtil;
import de.cech12.bucketlib.util.RegistryUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.DistExecutor;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.fluids.FluidUtil;
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

    public static DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, BucketLib.MOD_ID);
    public static DeferredRegister<IngredientType<?>> INGREDIENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, BucketLib.MOD_ID);

    static {
        RECIPE_SERIALIZERS.register("bucket_dyeing", () -> BucketDyeingRecipe.Serializer.INSTANCE);
        INGREDIENT_TYPES.register("fluid", () -> FluidIngredient.TYPE);
        INGREDIENT_TYPES.register("milk", () -> MilkIngredient.TYPE);
    }

    private static final Logger LOGGER = LogManager.getLogger();

    private static final List<UniversalBucketItem> buckets = new ArrayList<>();

    public BucketLibMod(IEventBus eventBus) {
        CommonLoader.init();

        eventBus.addListener(this::commonSetup);
        eventBus.addListener(this::processIMC);
        eventBus.addListener(this::addItemsToTabs);

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
                Optional<Item> bucketItem = BuiltInRegistries.ITEM.getOptional(bucketLocation);
                if (bucketItem.isEmpty()) {
                    LOGGER.info("Bucket could not be registered. The given ResourceLocation \"{}\" does not match any registered item in Forge registry.", bucketLocation);
                    return;
                }
                if (bucketItem.get() instanceof UniversalBucketItem bucket) {
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
                return ColorUtil.getColor(stack, bucket.getDefaultColor());
            }
            if (layer == 1) {
                return FluidUtil.getFluidContained(stack)
                        .map(fluidStack -> IClientFluidTypeExtensions.of(fluidStack.getFluid()).getTintColor(fluidStack))
                        .orElse(-1);
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
