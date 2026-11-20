package de.cech12.bucketlib.gametest;

import de.cech12.bucketlib.api.BucketLibApi;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Thin NeoForge adapter for the common game tests: the test mod "bucketlibtest" registers the test buckets (like a mod
 * that uses BucketLib), the test functions and the test environments. Only part of the gametest source set.
 * <p>
 * With the system property {@code bucketlibtest.milkEnabled=true} (run gameTestServerMilk) the NeoForge milk fluid is
 * enabled.
 */
@Mod(TestBuckets.NAMESPACE)
public final class NeoforgeTestMod {

    /**
     * Constructor of the test mod.
     *
     * @param modEventBus mod event bus
     */
    public NeoforgeTestMod(IEventBus modEventBus) {
        if (Boolean.getBoolean("bucketlibtest.milkEnabled")) {
            NeoForgeMod.enableMilkFluid();
        }
        modEventBus.addListener(NeoforgeTestMod::register);
        modEventBus.addListener(NeoforgeTestMod::registerCapabilities);
    }

    private static void register(RegisterEvent event) {
        event.register(Registries.ITEM, helper -> TestBuckets.ALL.forEach(bucket -> helper.register(bucket.id(), bucket.create())));
        event.register(Registries.TEST_FUNCTION, helper -> BucketLibGameTests.registerTestFunctions(helper::register));
        event.register(Registries.TEST_ENVIRONMENT_DEFINITION_TYPE, helper -> BucketLibGameTests.registerEnvironmentTypes(helper::register));
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (TestBuckets.Definition bucket : TestBuckets.ALL) {
            BucketLibApi.registerBucket(event, bucket.id());
        }
        // error handling of the API (logged and ignored): an unknown id, an item that is no UniversalBucketItem and null
        BucketLibApi.registerBucket(event, TestBuckets.UNKNOWN_BUCKET_ID);
        BucketLibApi.registerBucket(event, Identifier.withDefaultNamespace("stone"));
        BucketLibApi.registerBucket(event, null);
    }

}
