package cech12.bucketlib.jei;

import cech12.bucketlib.BucketLib;
import cech12.bucketlib.api.BucketLibApi;
import cech12.bucketlib.api.BucketLibTags;
import cech12.bucketlib.config.ServerConfig;
import cech12.bucketlib.api.item.UniversalBucketItem;
import cech12.bucketlib.util.BucketLibUtil;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("unused")
@JeiPlugin
public class ModJEIPlugin implements IModPlugin {

    private static final ResourceLocation ID = new ResourceLocation(BucketLibApi.MOD_ID, "jei_plugin");

    @Nonnull
    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerItemSubtypes(@Nonnull ISubtypeRegistration registration) {
        for (Item bucket : BucketLib.getRegisteredBuckets()) {
            registration.useNbtForSubtypes(bucket);
        }
    }

    @Override
    public void registerRecipes(@Nonnull IRecipeRegistration registration) {
        if (ServerConfig.INFINITY_ENCHANTMENT_ENABLED.get()) {
            IVanillaRecipeFactory factory = registration.getVanillaRecipeFactory();
            EnchantmentInstance data = new EnchantmentInstance(Enchantments.INFINITY_ARROWS, Enchantments.INFINITY_ARROWS.getMaxLevel());
            List<Object> recipes = new ArrayList<>();
            ITag<Fluid> infinityTag = Objects.requireNonNull(ForgeRegistries.FLUIDS.tags()).getTag(BucketLibTags.Fluids.INFINITY_ENCHANTABLE);
            for (UniversalBucketItem bucketItem : BucketLib.getRegisteredBuckets()) {
                for (Fluid fluid : ForgeRegistries.FLUIDS) {
                    if (fluid != Fluids.EMPTY && bucketItem.canHoldFluid(fluid) && infinityTag.contains(fluid)) {
                        ItemStack bucket = BucketLibUtil.addFluid(new ItemStack(bucketItem), fluid);
                        ItemStack enchantedBucket = bucket.copy();
                        enchantedBucket.enchant(data.enchantment, data.level);
                        recipes.add(factory.createAnvilRecipe(bucket,
                                Collections.singletonList(EnchantedBookItem.createForEnchantment(data)),
                                Collections.singletonList(enchantedBucket)));
                    }
                }
            }
            if (!recipes.isEmpty()) {
                registration.addRecipes(recipes, VanillaRecipeCategoryUid.ANVIL);
            }
        }
    }

}
