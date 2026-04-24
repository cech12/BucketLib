package de.cech12.bucketlib;
/*
import de.cech12.bucketlib.api.BucketLibApi;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;

@Mod(BucketLibTestMod.MOD_ID)
@EventBusSubscriber(modid= BucketLibTestMod.MOD_ID)
public class BucketLibTestMod {

    public static final String MOD_ID = "bucketlibtest";
    public static final boolean MILK_ENABLED = Boolean.parseBoolean(System.getProperty("bucketlibtest.milkEnabled", "false"));

    public static final TagKey<Fluid> WATER_TAG = TagKey.create(BuiltInRegistries.FLUID.key(), ResourceLocation.fromNamespaceAndPath(MOD_ID, "water"));

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, MOD_ID);

    public static final DeferredHolder<Item, ?> TEST_BUCKET = registerBucket("test_bucket", new UniversalBucketItem.Properties());
    public static final DeferredHolder<Item, ?> TEMPERATURE_BUCKET = registerBucket("temperature_bucket", new UniversalBucketItem.Properties().maxTemperature(1000));
    public static final DeferredHolder<Item, ?> WATER_ALLOWING_BUCKET = registerBucket("water_allowing_bucket", new UniversalBucketItem.Properties().allowedFluids(Collections.singletonList(Fluids.WATER)));
    public static final DeferredHolder<Item, ?> WATER_ALLOWING_TAG_BUCKET = registerBucket("water_allowing_tag_bucket", new UniversalBucketItem.Properties().allowedFluids(WATER_TAG));
    public static final DeferredHolder<Item, ?> WATER_DENYING_BUCKET = registerBucket("water_denying_bucket", new UniversalBucketItem.Properties().deniedFluids(Collections.singletonList(Fluids.WATER)));
    public static final DeferredHolder<Item, ?> WATER_DENYING_TAG_BUCKET = registerBucket("water_denying_tag_bucket", new UniversalBucketItem.Properties().deniedFluids(WATER_TAG));
    public static final DeferredHolder<Item, ?> CRACKING_BUCKET = registerBucket("cracking_bucket", new UniversalBucketItem.Properties().upperCrackingTemperature(1000));
    public static final DeferredHolder<Item, ?> COLORED_BUCKET = registerBucket("colored_bucket", new UniversalBucketItem.Properties());
    public static final DeferredHolder<Item, ?> ANTI_MILK_BUCKET = registerBucket("anti_milk_bucket", new UniversalBucketItem.Properties().disableMilking());
    public static final DeferredHolder<Item, ?> NO_ENTITIES_BUCKET = registerBucket("no_entities_bucket", new UniversalBucketItem.Properties().disableEntityObtaining());
    public static final DeferredHolder<Item, ?> ANTI_SALMON_BUCKET = registerBucket("anti_salmon_bucket", new UniversalBucketItem.Properties().deniedEntities(Collections.singletonList(EntityType.SALMON)));
    public static final DeferredHolder<Item, ?> ONLY_PUFFER_BUCKET = registerBucket("only_puffer_bucket", new UniversalBucketItem.Properties().allowedEntities(Collections.singletonList(EntityType.PUFFERFISH)));
    public static final DeferredHolder<Item, ?> NO_BLOCKS_BUCKET = registerBucket("no_blocks_bucket", new UniversalBucketItem.Properties().disableBlockObtaining());
    public static final DeferredHolder<Item, ?> DURABILITY_BUCKET = registerBucket("durability_bucket", new UniversalBucketItem.Properties().durability(5));
    public static final DeferredHolder<Item, ?> BURNING_BUCKET = registerBucket("burning_bucket", new UniversalBucketItem.Properties().durability(20).burningTemperature(1000).burningBlocks(Collections.singletonList(Blocks.POWDER_SNOW)));
    public static final DeferredHolder<Item, ?> FREEZING_BUCKET = registerBucket("freezing_bucket", new UniversalBucketItem.Properties().durability(20).freezingTemperature(500).freezingBlocks(Collections.singletonList(Blocks.POWDER_SNOW)));

    private static DeferredHolder<Item, ?> registerBucket(String name, UniversalBucketItem.Properties properties) {
        return ITEMS.register(name, () -> new UniversalBucketItem(ResourceKey.create(BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath(MOD_ID, name)), properties));
    }

    public BucketLibTestMod(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        if (MILK_ENABLED) {
            NeoForgeMod.enableMilkFluid();
        }
    }

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent evt) {
        for (DeferredHolder<Item, ?> item : ITEMS.getEntries()) {
            BucketLibApi.registerBucket(evt, item.getId());
        }
    }

}
 */
