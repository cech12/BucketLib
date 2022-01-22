package cech12.bucketlibtest;

import cech12.bucketlib.api.BucketLibApi;
import cech12.bucketlib.item.UniversalBucketItem;
import cech12.bucketlib.util.ColorUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.DispenseFluidContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;

@Mod(BucketLibTestMod.MOD_ID)
@Mod.EventBusSubscriber(modid= BucketLibTestMod.MOD_ID, bus= Mod.EventBusSubscriber.Bus.MOD)
public class BucketLibTestMod {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String MOD_ID = "bucketlibtest";

    public static final boolean ENABLED = true;

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BucketLibTestMod.MOD_ID);

    public static final RegistryObject<Item> TEST_BUCKET = ITEMS.register("test_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties()));
    public static final RegistryObject<Item> TEMPERATURE_BUCKET = ITEMS.register("temperature_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().maxTemperature(1000)));
    public static final RegistryObject<Item> WATER_BLOCKING_BUCKET = ITEMS.register("water_blocking_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().blockedFluids(Collections.singletonList(Fluids.WATER))));
    public static final RegistryObject<Item> CRACKING_BUCKET = ITEMS.register("cracking_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().upperCrackingTemperature(1000)));
    public static final RegistryObject<Item> COLORED_BUCKET = ITEMS.register("colored_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().dyeable(255, 0 ,0)));
    public static final RegistryObject<Item> ANTI_MILK_BUCKET = ITEMS.register("anti_milk_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().disableMilking()));
    public static final RegistryObject<Item> NO_ENTITIES_BUCKET = ITEMS.register("no_entities_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().disableEntityObtaining()));
    public static final RegistryObject<Item> ANTI_SALMON_BUCKET = ITEMS.register("anti_salmon_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().blockedEntities(Collections.singletonList(EntityType.SALMON))));
    public static final RegistryObject<Item> ONLY_PUFFER_BUCKET = ITEMS.register("only_puffer_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().allowedEntities(Collections.singletonList(EntityType.PUFFERFISH))));
    public static final RegistryObject<Item> NO_BLOCKS_BUCKET = ITEMS.register("no_blocks_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().disableBlockObtaining()));

    public BucketLibTestMod() {
        if (!ENABLED) return;
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modEventBus);
    }

    @SubscribeEvent
    public static void sendImc(InterModEnqueueEvent evt) {
        for (RegistryObject<Item> item : ITEMS.getEntries()) {
            BucketLibApi.registerBucket(item.getId());
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!ENABLED) return;
        //TODO For each item, a Dispense behaviour must be registered
        for (RegistryObject<Item> item : ITEMS.getEntries()) {
            DispenserBlock.registerBehavior(item.get(), DispenseFluidContainer.getInstance());
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void registerItemColors(ColorHandlerEvent.Item event) {
        LOGGER.info("registerItemColors called"); //TODO
        if (!ENABLED) return;
        for (RegistryObject<Item> item : ITEMS.getEntries()) {
            UniversalBucketItem bucketItem = (UniversalBucketItem) item.get();
            if (bucketItem.isDyeable()) {
                event.getItemColors().register((stack, color) -> (color > 0) ? -1 : ColorUtil.getColor(stack, bucketItem.getDefaultColor()), bucketItem);
            }
        }
    }

}
