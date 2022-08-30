package cech12.bucketlib;

import cech12.bucketlib.api.BucketLibApi;
import cech12.bucketlib.api.item.UniversalBucketItem;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;

@Mod(BucketLibTestMod.MOD_ID)
@Mod.EventBusSubscriber(modid= BucketLibTestMod.MOD_ID, bus= Mod.EventBusSubscriber.Bus.MOD)
public class BucketLibTestMod {

    public static final String MOD_ID = "bucketlibtest";
    public static final boolean MILK_ENABLED = Boolean.parseBoolean(System.getProperty("bucketlibtest.milkEnabled", "false"));

    public static final TagKey<Fluid> WATER_TAG = TagKey.create(Registry.FLUID_REGISTRY, new ResourceLocation(MOD_ID, "water"));

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BucketLibTestMod.MOD_ID);

    public static final RegistryObject<Item> TEST_BUCKET = ITEMS.register("test_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties()));
    public static final RegistryObject<Item> TEMPERATURE_BUCKET = ITEMS.register("temperature_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().maxTemperature(1000)));
    public static final RegistryObject<Item> WATER_ALLOWING_BUCKET = ITEMS.register("water_allowing_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().allowedFluids(Collections.singletonList(Fluids.WATER))));
    public static final RegistryObject<Item> WATER_ALLOWING_TAG_BUCKET = ITEMS.register("water_allowing_tag_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().allowedFluids(WATER_TAG)));
    public static final RegistryObject<Item> WATER_DENYING_BUCKET = ITEMS.register("water_denying_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().deniedFluids(Collections.singletonList(Fluids.WATER))));
    public static final RegistryObject<Item> WATER_DENYING_TAG_BUCKET = ITEMS.register("water_denying_tag_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().deniedFluids(WATER_TAG)));
    public static final RegistryObject<Item> CRACKING_BUCKET = ITEMS.register("cracking_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().upperCrackingTemperature(1000)));
    public static final RegistryObject<Item> COLORED_BUCKET = ITEMS.register("colored_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().dyeable(255, 0 ,0)));
    public static final RegistryObject<Item> ANTI_MILK_BUCKET = ITEMS.register("anti_milk_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().disableMilking()));
    public static final RegistryObject<Item> NO_ENTITIES_BUCKET = ITEMS.register("no_entities_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().disableEntityObtaining()));
    public static final RegistryObject<Item> ANTI_SALMON_BUCKET = ITEMS.register("anti_salmon_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().deniedEntities(Collections.singletonList(EntityType.SALMON))));
    public static final RegistryObject<Item> ONLY_PUFFER_BUCKET = ITEMS.register("only_puffer_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().allowedEntities(Collections.singletonList(EntityType.PUFFERFISH))));
    public static final RegistryObject<Item> NO_BLOCKS_BUCKET = ITEMS.register("no_blocks_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().disableBlockObtaining()));
    public static final RegistryObject<Item> DURABILITY_BUCKET = ITEMS.register("durability_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().durability(5)));
    public static final RegistryObject<Item> BURNING_BUCKET = ITEMS.register("burning_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().durability(20).burningTemperature(1000).burningBlocks(Collections.singletonList(Blocks.POWDER_SNOW))));
    public static final RegistryObject<Item> FREEZING_BUCKET = ITEMS.register("freezing_bucket", () -> new UniversalBucketItem(new UniversalBucketItem.Properties().durability(20).freezingTemperature(500).freezingBlocks(Collections.singletonList(Blocks.POWDER_SNOW))));

    public BucketLibTestMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modEventBus);
        if (MILK_ENABLED) {
            ForgeMod.enableMilkFluid();
        }
    }

    @SubscribeEvent
    public static void sendImc(InterModEnqueueEvent evt) {
        for (RegistryObject<Item> item : ITEMS.getEntries()) {
            BucketLibApi.registerBucket(item.getId());
        }
    }

}
