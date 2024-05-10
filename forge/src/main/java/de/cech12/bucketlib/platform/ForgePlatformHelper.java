package de.cech12.bucketlib.platform;

import de.cech12.bucketlib.platform.services.IPlatformHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * The platform service implementation for Forge.
 */
public class ForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Forge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public Level getCurrentLevel() {
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            return ServerLifecycleHooks.getCurrentServer().overworld();
        }
        return LogicalSidedProvider.CLIENTWORLD.get(LogicalSide.CLIENT).orElse(null);
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
