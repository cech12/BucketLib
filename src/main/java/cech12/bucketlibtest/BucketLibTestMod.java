package cech12.bucketlibtest;

import cech12.bucketlib.item.UniversalBucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.DispenseFluidContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;

@Mod(BucketLibTestMod.MOD_ID)
@Mod.EventBusSubscriber(modid= BucketLibTestMod.MOD_ID, bus= Mod.EventBusSubscriber.Bus.MOD)
public class BucketLibTestMod {

    public static final String MOD_ID = "bucketlibtest";

    public static final boolean ENABLED = true;

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BucketLibTestMod.MOD_ID);

    public static final RegistryObject<Item> TEST_BUCKET = ITEMS.register("test_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties()));
    public static final RegistryObject<Item> TEMPERATURE_BUCKET = ITEMS.register("temperature_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().maxTemperature(1000)));
    public static final RegistryObject<Item> WATER_BLOCKING_BUCKET = ITEMS.register("water_blocking_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().blockedFluids(Collections.singletonList(Fluids.WATER))));
    public static final RegistryObject<Item> CRACKING_BUCKET = ITEMS.register("cracking_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().upperCrackingTemperature(1000)));

    public BucketLibTestMod() {
        if (!ENABLED) return;
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modEventBus);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!ENABLED) return;
        //TODO For each item, a Dispense behaviour must be registered
        for (RegistryObject<Item> item : ITEMS.getEntries()) {
            DispenserBlock.registerBehavior(item.get(), DispenseFluidContainer.getInstance());
        }
    }

}
