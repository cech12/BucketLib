package cech12.bucketlib;

import cech12.bucketlib.api.BucketLibApi;
import cech12.bucketlib.item.UniversalBucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.DispenseFluidContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.Collectors;

@Mod(BucketLibApi.MOD_ID)
@Mod.EventBusSubscriber(modid= BucketLibApi.MOD_ID, bus= Mod.EventBusSubscriber.Bus.MOD)
public class BucketLib {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BucketLibApi.MOD_ID);
    public static final RegistryObject<Item> TEST_BUCKET = ITEMS.register("test_bucket", UniversalBucketItem::new);

    public BucketLib() {
        //TODO item only for testing
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modEventBus);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        //TODO For each item, a Dispense behaviour must be registered
        DispenserBlock.registerBehavior(TEST_BUCKET.get(), DispenseFluidContainer.getInstance());
    }

    private void processIMC(final InterModProcessEvent event) {
        //TODO
        LOGGER.info("Got IMC {}", event.getIMCStream().
                map(m->m.messageSupplier().get()).
                collect(Collectors.toList()));
    }

}
