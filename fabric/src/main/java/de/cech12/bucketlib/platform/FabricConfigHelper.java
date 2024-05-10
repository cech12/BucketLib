package de.cech12.bucketlib.platform;

import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.platform.services.IConfigHelper;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;

/**
 * The config service implementation for Fabric.
 */
@Config(name = BucketLib.MOD_ID)
public class FabricConfigHelper implements ConfigData, IConfigHelper {

    @ConfigEntry.Gui.Tooltip(count = 5)
    public boolean INFINITY_ENCHANTMENT_ENABLED = INFINITY_ENCHANTMENT_ENABLED_DEFAULT;

    @Override
    public void init() {
        AutoConfig.register(FabricConfigHelper.class, Toml4jConfigSerializer::new);
    }

    @Override
    public boolean isInfinityEnchantmentEnabled() {
        return AutoConfig.getConfigHolder(FabricConfigHelper.class).getConfig().INFINITY_ENCHANTMENT_ENABLED;
    }

}
