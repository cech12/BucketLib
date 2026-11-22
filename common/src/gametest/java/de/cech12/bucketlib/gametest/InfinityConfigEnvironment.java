package de.cech12.bucketlib.gametest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.server.level.ServerLevel;

/**
 * Test environment that sets the "infinity enchantment enabled" config option of BucketLib for all tests of a batch
 * and restores it afterward. Tests are grouped into batches by environment and batches run one after another, so tests
 * with different config values never run at the same time.
 * <p>
 * Usage in a test_environment json: {@code {"type": "bucketlib:infinity_config", "infinity_enchantment_enabled": true}}
 *
 * @param infinityEnchantmentEnabled config value that is set during the batch
 */
public record InfinityConfigEnvironment(boolean infinityEnchantmentEnabled) implements TestEnvironmentDefinition<Boolean> {

    /** codec of this environment type */
    public static final MapCodec<InfinityConfigEnvironment> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(Codec.BOOL.fieldOf("infinity_enchantment_enabled").forGetter(InfinityConfigEnvironment::infinityEnchantmentEnabled))
                    .apply(i, InfinityConfigEnvironment::new)
    );

    @Override
    public Boolean setup(ServerLevel level) {
        return GameTestPlatform.INSTANCE.setInfinityEnchantmentEnabled(this.infinityEnchantmentEnabled);
    }

    @Override
    public void teardown(ServerLevel level, Boolean previous) {
        GameTestPlatform.INSTANCE.setInfinityEnchantmentEnabled(previous);
    }

    @Override
    public MapCodec<InfinityConfigEnvironment> codec() {
        return CODEC;
    }

}
