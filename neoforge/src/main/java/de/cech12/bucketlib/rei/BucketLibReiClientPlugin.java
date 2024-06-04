package de.cech12.bucketlib.rei;

import de.cech12.bucketlib.api.BucketLibTags;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.platform.Services;
import de.cech12.bucketlib.util.BucketLibUtil;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.forge.REIPluginClient;
import me.shedaniel.rei.plugin.common.displays.DefaultFuelDisplay;
import me.shedaniel.rei.plugin.common.displays.anvil.DefaultAnvilDisplay;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@REIPluginClient
public class BucketLibReiClientPlugin implements REIClientPlugin {

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        //register fuel
        for (UniversalBucketItem bucketItem : Services.REGISTRY.getRegisteredBuckets()) {
            for (Fluid fluid : Services.REGISTRY.getAllFluids()) {
                if (fluid != Fluids.EMPTY && bucketItem.canHoldFluid(fluid)) {
                    ItemStack bucket = BucketLibUtil.addFluid(new ItemStack(bucketItem), fluid);
                    int burnTime = bucketItem.getBurnTime(bucket, null);
                    if (burnTime > 0) {
                        registry.add(new DefaultFuelDisplay(
                                List.of(EntryIngredients.of(bucket)),
                                List.of(),
                                burnTime));
                    }
                }
            }
        }
        //register infinity enchanting
        if (Services.CONFIG.isInfinityEnchantmentEnabled()) {
            EnchantmentInstance data = new EnchantmentInstance(Enchantments.INFINITY_ARROWS, Enchantments.INFINITY_ARROWS.getMaxLevel());
            for (UniversalBucketItem bucketItem : Services.REGISTRY.getRegisteredBuckets()) {
                for (Fluid fluid : Services.REGISTRY.getAllFluids()) {
                    if (fluid != Fluids.EMPTY && bucketItem.canHoldFluid(fluid) && fluid.defaultFluidState().is(BucketLibTags.Fluids.INFINITY_ENCHANTABLE)) {
                        ItemStack bucket = BucketLibUtil.addFluid(new ItemStack(bucketItem), fluid);
                        ItemStack enchantedBucket = bucket.copy();
                        enchantedBucket.enchant(data.enchantment, data.level);
                        registry.add(new DefaultAnvilDisplay(
                                List.of(EntryIngredients.of(bucket), EntryIngredients.of(EnchantedBookItem.createForEnchantment(data))),
                                List.of(EntryIngredients.of(enchantedBucket)),
                                Optional.empty()));
                    }
                }
            }
        }
    }

}
