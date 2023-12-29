package de.cech12.bucketlib.platform;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.platform.services.IConfigHelper;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.nio.file.Path;

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
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_CONFIG);
        Path path = FMLPaths.GAMEDIR.get().resolve(FMLConfig.defaultConfigPath()).resolve(BucketLib.MOD_ID + "-server.toml");
        final CommentedFileConfig configData = CommentedFileConfig.builder(path).sync().autosave().writingMode(WritingMode.REPLACE).build();
        configData.load();
        SERVER_CONFIG.setConfig(configData);
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
