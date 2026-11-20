package de.cech12.bucketlib.gametest;

import de.cech12.bucketlib.api.BucketLibApi;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
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

    /** test fluids of other mods (fluids can only be created while their registry is open) */
    private static final java.util.Map<TestFluids.Definition, TestFluids.TestFluid> FLUIDS = new java.util.LinkedHashMap<>();
    /** fluid types of the test fluids */
    private static final java.util.Map<TestFluids.Definition, FluidType> FLUID_TYPES = new java.util.LinkedHashMap<>();

    private static FluidType fluidType(TestFluids.Definition definition) {
        return FLUID_TYPES.computeIfAbsent(definition, d -> new FluidType(FluidType.Properties.create()
                .descriptionId("fluid." + TestBuckets.NAMESPACE + "." + d.name())
                .temperature(d.temperature())
                .lightLevel(d.lightLevel())
                .density(d.lighterThanAir() ? -1000 : 1000)));
    }

    private static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> helper.register(TestBlocks.INCOMPATIBLE_FLUID_SOURCE, TestBlocks.create()));
        event.register(NeoForgeRegistries.Keys.FLUID_TYPES, helper -> TestFluids.REGISTERED.forEach(definition -> helper.register(definition.id(), fluidType(definition))));
        event.register(Registries.FLUID, helper -> TestFluids.REGISTERED.forEach(definition -> {
            FluidType type = fluidType(definition);
            TestFluids.TestFluid fluid = new TestFluids.TestFluid(definition) {
                @Override
                public FluidType getFluidType() {
                    return type;
                }
            };
            FLUIDS.put(definition, fluid);
            helper.register(definition.id(), fluid);
        }));
        event.register(Registries.ITEM, helper -> {
            if (FLUIDS.size() != TestFluids.REGISTERED.size()) {
                throw new IllegalStateException("test fluids must be registered before the items");
            }
            FLUIDS.forEach((definition, fluid) -> {
                if (definition.bucketKind() == TestFluids.BucketKind.OWN_BUCKET) {
                    helper.register(definition.bucketId(), definition.createBucket(fluid));
                }
            });
            TestBuckets.ALL.forEach(bucket -> helper.register(bucket.id(), bucket.create()));
        });
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
