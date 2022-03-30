package cech12.bucketlib.config;

import cech12.bucketlib.api.BucketLibTags;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraftforge.common.ForgeConfigSpec;

import java.nio.file.Path;

public class ServerConfig {

    public static final ForgeConfigSpec SERVER_CONFIG;

    public static final ForgeConfigSpec.BooleanValue INFINITY_ENCHANTMENT_ENABLED;

    static {
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("Balance Options");

        INFINITY_ENCHANTMENT_ENABLED = builder
                .comment("Whether or not Infinity enchantment for modded buckets filled with fluids of the tag \"" + BucketLibTags.Fluids.INFINITY_ENCHANTABLE.location() + "\" should be enabled.")
                .define("infinityEnchantmentEnabled", false);

        builder.pop();

        SERVER_CONFIG = builder.build();
    }

    public static void loadConfig(ForgeConfigSpec spec, Path path) {
        final CommentedFileConfig configData = CommentedFileConfig.builder(path).sync().autosave().writingMode(WritingMode.REPLACE).build();
        configData.load();
        spec.setConfig(configData);
    }

}
