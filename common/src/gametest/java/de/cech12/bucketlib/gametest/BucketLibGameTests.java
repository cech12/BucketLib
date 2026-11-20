package de.cech12.bucketlib.gametest;

import com.mojang.serialization.MapCodec;
import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.gametest.tests.CauldronTests;
import de.cech12.bucketlib.gametest.tests.CodecTests;
import de.cech12.bucketlib.gametest.tests.ContentTests;
import de.cech12.bucketlib.gametest.tests.DispenserTests;
import de.cech12.bucketlib.gametest.tests.DurabilityTests;
import de.cech12.bucketlib.gametest.tests.EntityTests;
import de.cech12.bucketlib.gametest.tests.FluidApiTests;
import de.cech12.bucketlib.gametest.tests.FluidTests;
import de.cech12.bucketlib.gametest.tests.FurnaceTests;
import de.cech12.bucketlib.gametest.tests.IngredientTests;
import de.cech12.bucketlib.gametest.tests.InfinityTests;
import de.cech12.bucketlib.gametest.tests.MetaTests;
import de.cech12.bucketlib.gametest.tests.MilkTests;
import de.cech12.bucketlib.gametest.tests.PropertiesTests;
import de.cech12.bucketlib.gametest.tests.RecipeTests;
import de.cech12.bucketlib.gametest.tests.RegistrationTests;
import de.cech12.bucketlib.gametest.tests.ResourceTests;
import de.cech12.bucketlib.gametest.tests.BlockTests;
import de.cech12.bucketlib.gametest.tests.TemperatureTests;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Central entry point of all game tests. The loader adapters only call the register methods of this class.
 * <p>
 * Every test function registered here needs a test instance json in
 * {@code common/src/gametest/resources/data/bucketlib/test_instance/<name>.json} (checked by
 * {@link MetaTests#everyTestFunctionHasAnInstance}).
 */
public final class BucketLibGameTests {

    private static Map<Identifier, Consumer<GameTestHelper>> tests;

    private BucketLibGameTests() {}

    /**
     * @return all test functions of this mod, keyed by id
     */
    public static synchronized Map<Identifier, Consumer<GameTestHelper>> getTests() {
        if (tests == null) {
            Map<Identifier, Consumer<GameTestHelper>> map = new LinkedHashMap<>();
            TestCollector collector = (name, test) -> {
                Identifier id = BucketLib.id(name);
                if (map.put(id, test) != null) {
                    throw new IllegalStateException("Duplicate game test " + id);
                }
            };
            MetaTests.register(collector);
            RegistrationTests.register(collector);
            PropertiesTests.register(collector);
            ContentTests.register(collector);
            FluidTests.register(collector);
            CauldronTests.register(collector);
            BlockTests.register(collector);
            EntityTests.register(collector);
            MilkTests.register(collector);
            DispenserTests.register(collector);
            DurabilityTests.register(collector);
            InfinityTests.register(collector);
            TemperatureTests.register(collector);
            FurnaceTests.register(collector);
            FluidApiTests.register(collector);
            IngredientTests.register(collector);
            RecipeTests.register(collector);
            CodecTests.register(collector);
            ResourceTests.register(collector);
            tests = Collections.unmodifiableMap(map);
        }
        return tests;
    }

    /**
     * Registers all test functions. Must be called while the test function registry is writable.
     *
     * @param registrar loader specific registration callback
     */
    public static void registerTestFunctions(BiConsumer<Identifier, Consumer<GameTestHelper>> registrar) {
        getTests().forEach(registrar);
    }

    /**
     * Registers all test environment types. Must be called while the test environment definition type registry is
     * writable.
     *
     * @param registrar loader specific registration callback
     */
    public static void registerEnvironmentTypes(BiConsumer<Identifier, MapCodec<? extends TestEnvironmentDefinition<?>>> registrar) {
        registrar.accept(BucketLib.id("infinity_config"), InfinityConfigEnvironment.CODEC);
    }

    /**
     * Collects test functions of a test class.
     */
    @FunctionalInterface
    public interface TestCollector {
        /**
         * @param name test name (path of the test id, must match the test_instance json file name)
         * @param test test function
         */
        void add(String name, Consumer<GameTestHelper> test);
    }

}
