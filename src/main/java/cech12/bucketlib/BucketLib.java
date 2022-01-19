package cech12.bucketlib;

import cech12.bucketlib.api.BucketLibApi;
import cech12.bucketlib.api.BucketLibTags;
import cech12.bucketlib.config.ServerConfig;
import cech12.bucketlib.item.UniversalBucketItem;
import cech12.bucketlib.item.crafting.BucketDyeingRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mod(BucketLibApi.MOD_ID)
@Mod.EventBusSubscriber(modid= BucketLibApi.MOD_ID, bus= Mod.EventBusSubscriber.Bus.MOD)
public class BucketLib {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final List<UniversalBucketItem> buckets = new ArrayList<>();

    public BucketLib() {
        //Config
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SERVER_CONFIG);
        ServerConfig.loadConfig(ServerConfig.SERVER_CONFIG, FMLPaths.GAMEDIR.get().resolve(FMLConfig.defaultConfigPath()).resolve(BucketLibApi.MOD_ID + "-server.toml"));

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
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
                    buckets.add(bucket);
                } else {
                    LOGGER.info("Bucket could not be registered. The item \"{}\" is not a {}.", bucketLocation, UniversalBucketItem.class.getName());
                }
            } else {
                LOGGER.warn("Bucket could not be registered. The message supplier of the IMCMessage does not contain a ResourceLocation");
            }
        });
    }

    @SubscribeEvent
    public static void registerRecipeSerializers(RegistryEvent.Register<RecipeSerializer<?>> event) {
        LOGGER.info("registerRecipeSerializers called"); //TODO
        //dye recipe serializer
        event.getRegistry().register(BucketDyeingRecipe.SERIALIZER);
    }

}
