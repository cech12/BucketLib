package de.cech12.bucketlib.platform;

import de.cech12.bucketlib.platform.services.IPlatformHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

/**
 * The platform service implementation for NeoForge.
 */
public class NeoforgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.getCurrent().isProduction();
    }

    @Override
    public ResourceKey<CreativeModeTab> getToolsAndUtilitiesTab() {
        return CreativeModeTabs.TOOLS_AND_UTILITIES;
    }

    @Override
    public String getMilkTranslationKey() {
        return "fluid_type.minecraft.milk";
    }

}
