package de.cech12.bucketlib.jei;
/*
import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.api.BucketLibTags;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.platform.Services;
import de.cech12.bucketlib.util.BucketLibUtil;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
@JeiPlugin
public class ModJEIPlugin implements IModPlugin {

    private static final ResourceLocation ID = BucketLib.id("jei_plugin");

    @NotNull
    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerItemSubtypes(@NotNull ISubtypeRegistration registration) {
        for (Item bucket : Services.REGISTRY.getRegisteredBuckets()) {
            registration.registerSubtypeInterpreter(bucket, (stack, context) -> {
                if (BucketLibUtil.containsMilk(stack)) {
                    return "milk";
                }
                EntityType<?> entityType = BucketLibUtil.getEntityType(stack);
                if (entityType != null) {
                    return entityType.getDescriptionId();
                }
                Block block = BucketLibUtil.getBlock(stack);
                if (block != null) {
                    return block.getDescriptionId();
                }
                Fluid fluid = BucketLibUtil.getFluid(stack);
                if (fluid != null) {
                    return fluid.toString();
                }
                return "empty";
            });
        }
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        if (Services.CONFIG.isInfinityEnchantmentEnabled()) {
            IVanillaRecipeFactory factory = registration.getVanillaRecipeFactory();
            EnchantmentInstance data = new EnchantmentInstance(VanillaRegistries.createLookup().lookup(Registries.ENCHANTMENT).get().getOrThrow(Enchantments.INFINITY), 1);
            List<IJeiAnvilRecipe> recipes = new ArrayList<>();
            for (UniversalBucketItem bucketItem : Services.REGISTRY.getRegisteredBuckets()) {
                ResourceLocation itemId = Services.REGISTRY.getItemLocation(bucketItem);
                for (Fluid fluid : Services.REGISTRY.getAllFluids()) {
                    if (fluid != Fluids.EMPTY && bucketItem.canHoldFluid(fluid) && fluid.defaultFluidState().is(BucketLibTags.Fluids.INFINITY_ENCHANTABLE)) {
                        ItemStack bucket = BucketLibUtil.addFluid(new ItemStack(bucketItem), fluid);
                        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
                        enchantedBook.enchant(data.enchantment(), data.level());
                        ItemStack enchantedBucket = bucket.copy();
                        enchantedBucket.enchant(data.enchantment(), data.level());
                        recipes.add(factory.createAnvilRecipe(bucket,
                                Collections.singletonList(enchantedBook),
                                Collections.singletonList(enchantedBucket),
                                BucketLib.id(itemId.getPath() + "_" + Services.REGISTRY.getFluidLocation(fluid).getPath())));
                    }
                }
            }
            if (!recipes.isEmpty()) {
                registration.addRecipes(RecipeTypes.ANVIL, recipes);
            }
        }
    }

}
 */
