package de.cech12.bucketlib;

import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.Container;
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

    public static LootContext createLootContext(Object reference) {
        ContextMap.Builder contextMapBuilder = ContextMap.builder();
        if (reference instanceof Container container) {
            contextMapBuilder.set(LootContextParams.CONTAINER, container);
        }
        if (reference instanceof BlockEntity blockEntity) {
            contextMapBuilder.set(LootContextParams.BLOCK_STATE, blockEntity.getBlockState())
                    .set(LootContextParams.BLOCK_ENTITY, blockEntity)
                    .set(LootContextParams.ORIGIN, Vec3.atCenterOf(blockEntity.getBlockPos()));
        }
        LootParams params = new LootParams(null, contextMapBuilder.build(), Map.of(), 0);
        assert Minecraft.getInstance().level != null;
        return new LootContext(params, RandomSource.create(), getRegistryProvider());
    }

    private static HolderLookup.Provider getRegistryProvider() {
        if (REGISTRY_PROVIDER == null) {
            REGISTRY_PROVIDER = createRegistryProvider(getRegistryAccess());
        }
        return REGISTRY_PROVIDER;
    }

    private static RegistryAccess getRegistryAccess() {
        if (REGISTRY_ACCESS == null) {
            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel level = minecraft.level;
            if (level == null) {
                throw new IllegalStateException("Could not get registry, registry access is unavailable because the level is currently null");
            }
            REGISTRY_ACCESS = level.registryAccess();
        }
        return REGISTRY_ACCESS;
    }

    private static HolderLookup.Provider createRegistryProvider(HolderLookup.Provider registryProvider) {
        Map<ResourceKey<? extends Registry<?>>, HolderLookup.RegistryLookup<?>> lookups = new HashMap<>();
        registryProvider.listRegistries().forEach(lookup -> lookups.put(lookup.key(), lookup));
        VanillaRegistries.createWorldLookup().listRegistries().forEach(lookup -> lookups.put(lookup.key(), lookup));
        HolderLookup.Provider context = HolderLookup.Provider.create(lookups.values().stream());
        return VanillaRegistries.createReloadableLookup(context);
    }

}
