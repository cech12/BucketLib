package de.cech12.bucketlib.platform;

import de.cech12.bucketlib.platform.services.IConfigHelper;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The config service implementation for NeoForge.
 */
public class NeoforgeConfigHelper implements IConfigHelper {

    private static final ModConfigSpec SERVER_CONFIG;

    public static final ModConfigSpec.BooleanValue INFINITY_ENCHANTMENT_ENABLED;

    static {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("Balance Options");

        INFINITY_ENCHANTMENT_ENABLED = builder
                .comment(INFINITY_ENCHANTMENT_ENABLED_DESCRIPTION)
                .define("infinityEnchantmentEnabled", INFINITY_ENCHANTMENT_ENABLED_DEFAULT);

        builder.pop();

        SERVER_CONFIG = builder.build();
    }

    @Override
    public void init() {
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.SERVER, SERVER_CONFIG);
    }

    @Override
    public boolean isInfinityEnchantmentEnabled() {
        try {
            return INFINITY_ENCHANTMENT_ENABLED.get();
        } catch (IllegalStateException ex) {
            return INFINITY_ENCHANTMENT_ENABLED_DEFAULT;
        }
    }

}
