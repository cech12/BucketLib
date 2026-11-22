package de.cech12.bucketlib;

import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.platform.Services;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * A static class for all loaders which initializes everything which is used by all loaders.
 */
public class CommonLoader {

    /** Logger instance */
    public static final Logger LOG = LogManager.getLogger(BucketLib.MOD_NAME);

    private static RegistryAccess REGISTRY_ACCESS = null;
    private static HolderLookup.Provider REGISTRY_PROVIDER = null;

    /**
     * Initialize method that should be called by every loader mod in the constructor.
     */
    public static void init() {
        Services.CONFIG.init();
    }

    private CommonLoader() {}

    /**
     * Creates a loot context to resolve data components like the cooking fuel of a bucket. Works on the server and the
     * client, because the registries are taken from the given level (no client classes).
     *
     * @param reference container or block entity which is added to the context parameters
     * @param level level of the reference (server or client level)
     * @return loot context
     */
    public static LootContext createLootContext(Object reference, Level level) {
        ContextMap.Builder contextMapBuilder = ContextMap.builder();
        if (reference instanceof Container container) {
            contextMapBuilder.set(LootContextParams.CONTAINER, container);
        }
        if (reference instanceof BlockEntity blockEntity) {
            contextMapBuilder.set(LootContextParams.BLOCK_STATE, blockEntity.getBlockState())
                    .set(LootContextParams.BLOCK_ENTITY, blockEntity)
                    .set(LootContextParams.ORIGIN, Vec3.atCenterOf(blockEntity.getBlockPos()));
        }
        ServerLevel serverLevel = level instanceof ServerLevel server ? server : null;
        LootParams params = new LootParams(serverLevel, contextMapBuilder.build(), Map.of(), 0);
        return new LootContext(params, RandomSource.create(), getRegistryProvider(level));
    }

    private static synchronized HolderLookup.Provider getRegistryProvider(Level level) {
        if (level == null) {
            throw new IllegalStateException("Could not get registry, registry access is unavailable because the level is null");
        }
        if (level instanceof ServerLevel serverLevel) {
            // like vanilla: the reloadable registries of the server contain the context providers of the data packs
            return serverLevel.getServer().reloadableRegistries().lookup();
        }
        RegistryAccess registryAccess = level.registryAccess();
        if (REGISTRY_ACCESS != registryAccess) {
            REGISTRY_ACCESS = registryAccess;
            REGISTRY_PROVIDER = createRegistryProvider(registryAccess);
        }
        return REGISTRY_PROVIDER;
    }

    private static HolderLookup.Provider createRegistryProvider(HolderLookup.Provider registryProvider) {
        Map<ResourceKey<? extends Registry<?>>, HolderLookup.RegistryLookup<?>> lookups = new HashMap<>();
        registryProvider.listRegistries().forEach(lookup -> lookups.put(lookup.key(), lookup));
        VanillaRegistries.createWorldLookup().listRegistries().forEach(lookup -> lookups.put(lookup.key(), lookup));
        HolderLookup.Provider context = HolderLookup.Provider.create(lookups.values().stream());
        return VanillaRegistries.createReloadableLookup(context);
    }

}
