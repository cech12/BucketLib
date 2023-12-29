package de.cech12.bucketlib.platform;

import de.cech12.bucketlib.platform.services.IPlatformHelper;
import net.minecraft.world.level.Level;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.util.LogicalSidedProvider;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

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
        return !FMLLoader.isProduction();
    }

    @Override
    public Level getCurrentLevel() {
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            return ServerLifecycleHooks.getCurrentServer().overworld();
        }
        return LogicalSidedProvider.CLIENTWORLD.get(LogicalSide.CLIENT).orElse(null);
    }

}
