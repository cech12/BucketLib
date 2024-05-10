package de.cech12.bucketlib.platform;

import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.platform.services.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.Level;

/**
 * The platform service implementation for Fabric.
 */
public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public Level getCurrentLevel() {
        return BucketLibMod.SERVER_LEVEL;
    }

    @Override
    public ResourceKey<CreativeModeTab> getToolsAndUtilitiesTab() {
        return CreativeModeTabs.TOOLS_AND_UTILITIES;
    }

    @Override
    public String getMilkTranslationKey() {
        return "tag.fluid.c.milk";
    }

}
