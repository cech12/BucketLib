package cech12.bucketlib;

import cech12.bucketlib.api.BucketLibApi;
import cech12.bucketlib.api.BucketLibTags;
import cech12.bucketlib.api.crafting.BlockIngredient;
import cech12.bucketlib.api.crafting.EmptyIngredient;
import cech12.bucketlib.api.crafting.EntityIngredient;
import cech12.bucketlib.api.crafting.FluidIngredient;
import cech12.bucketlib.api.crafting.MilkIngredient;
import cech12.bucketlib.config.ServerConfig;
import cech12.bucketlib.api.item.UniversalBucketItem;
import cech12.bucketlib.item.UniversalBucketDispenseBehaviour;
import cech12.bucketlib.item.crafting.BucketDyeingRecipe;
import cech12.bucketlib.util.BucketLibUtil;
import cech12.bucketlib.util.ColorUtil;
import cech12.bucketlib.util.RegistryUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mod(BucketLibApi.MOD_ID)
public class BucketLib {

    public static DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, BucketLibApi.MOD_ID);
    public static RegistryObject<RecipeSerializer<?>> BUCKET_DYEING_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("bucket_dyeing", () -> BucketDyeingRecipe.Serializer.INSTANCE);

    private static final Logger LOGGER = LogManager.getLogger();

    private static final List<UniversalBucketItem> buckets = new ArrayList<>();

    public BucketLib() {
        //Config
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SERVER_CONFIG);
        ServerConfig.loadConfig(ServerConfig.SERVER_CONFIG, FMLPaths.GAMEDIR.get().resolve(FMLConfig.defaultConfigPath()).resolve(BucketLibApi.MOD_ID + "-server.toml"));

        final IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.addListener(this::commonSetup);
        eventBus.addListener(this::processIMC);
        eventBus.addListener(this::addItemsToTabs);

        //dye recipe serializer
        RECIPE_SERIALIZERS.register(eventBus);
        //ingredient serializer
        CraftingHelper.register(BlockIngredient.Serializer.NAME, BlockIngredient.Serializer.INSTANCE);
        CraftingHelper.register(EmptyIngredient.Serializer.NAME, EmptyIngredient.Serializer.INSTANCE);
        CraftingHelper.register(EntityIngredient.Serializer.NAME, EntityIngredient.Serializer.INSTANCE);
        CraftingHelper.register(FluidIngredient.Serializer.NAME, FluidIngredient.Serializer.INSTANCE);
        CraftingHelper.register(MilkIngredient.Serializer.NAME, MilkIngredient.Serializer.INSTANCE);
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
            if (!BucketLibApi.MOD_ID.equals(imcMessage.modId())) {
                LOGGER.warn("Bucket could not be registered. The mod id \"{}\" of the IMCMessage is not \"{}\"", imcMessage.modId(), BucketLibApi.MOD_ID);
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
