package de.cech12.bucketlib.api.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class MilkIngredient implements CustomIngredient {

    private List<ItemStack> matchingStacks;

    public MilkIngredient() {
    }

    @Override
    public boolean test(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        if (itemStack.getItem() == Items.MILK_BUCKET) {
            return true;
        }
        return BucketLibUtil.containsMilk(itemStack.copy());
    }

    @Override
    public List<ItemStack> getMatchingStacks() {
        if (this.matchingStacks == null) {
            this.matchingStacks = new ArrayList<>();
            this.matchingStacks.add(new ItemStack(Items.MILK_BUCKET));
            BucketLibMod.getRegisteredBuckets().forEach(universalBucketItem -> {
                if (universalBucketItem.canMilkEntities()) {
                    this.matchingStacks.add(BucketLibUtil.addMilk(new ItemStack(universalBucketItem)));
                }
            });
        }
        return this.matchingStacks;
    }

    @Override
    public boolean requiresTesting() {
        return true;
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static final Codec<MilkIngredient> CODEC = MapCodec.of(Encoder.empty(), Decoder.unit(new MilkIngredient())).codec();

    public static final class Serializer implements CustomIngredientSerializer<MilkIngredient> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation NAME = new ResourceLocation(BucketLib.MOD_ID, "milk");

        private Serializer() {}

        @Override
        public ResourceLocation getIdentifier() {
            return NAME;
        }

        @Override
        public MilkIngredient read(FriendlyByteBuf buffer) {
            return new MilkIngredient();
        }

        @Override
        public void write(@Nonnull FriendlyByteBuf buffer, @Nonnull MilkIngredient ingredient) {
        }

        @Override
        public Codec<MilkIngredient> getCodec(boolean allowEmpty) {
            return CODEC;
        }
    }

}
