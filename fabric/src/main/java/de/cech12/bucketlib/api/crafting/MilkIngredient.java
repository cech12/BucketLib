package de.cech12.bucketlib.api.crafting;

import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class MilkIngredient implements CustomIngredient {

    private List<Holder<Item>> matchingStacks;

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
        Identifier location = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        //Mekansim tanks are not compatible: https://github.com/cech12/BucketLib/issues/55 | https://github.com/mekanism/Mekanism/issues/8335
        if ("mekanism".equals(location.getNamespace()) && itemStack.getRecipeRemainder().isEmpty()) {
            return false;
        }
        return BucketLibUtil.containsMilk(itemStack.copy());
    }

    @Override
    public Stream<Holder<Item>> getMatchingItems() {
        if (this.matchingStacks == null) {
            this.matchingStacks = new ArrayList<>();
            this.matchingStacks.add(Holder.direct(Items.MILK_BUCKET));
            BucketLibMod.getRegisteredBuckets().forEach(universalBucketItem -> {
                if (universalBucketItem.canMilkEntities()) {
                    //this.matchingStacks.add(BucketLibUtil.addMilk(new ItemStack(universalBucketItem)));
                    this.matchingStacks.add(Holder.direct(universalBucketItem));
                }
            });
        }
        return this.matchingStacks.stream();
    }

    @Override
    public boolean requiresTesting() {
        return true;
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static final class Serializer implements CustomIngredientSerializer<MilkIngredient> {

        public static final Serializer INSTANCE = new Serializer();
        public static final Identifier NAME = BucketLib.id("milk");

        public static final MapCodec<MilkIngredient> CODEC = MapCodec.of(Encoder.empty(), Decoder.unit(new MilkIngredient()));

        private static final StreamCodec<RegistryFriendlyByteBuf, MilkIngredient> PACKET_CODEC = StreamCodec.of(
                MilkIngredient.Serializer::write,
                MilkIngredient.Serializer::read);

        private Serializer() {}

        @Override
        public Identifier getIdentifier() {
            return NAME;
        }

        @Override
        public MapCodec<MilkIngredient> getCodec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MilkIngredient> getPacketCodec() {
            return PACKET_CODEC;
        }

        private static MilkIngredient read(RegistryFriendlyByteBuf buffer) {
            return new MilkIngredient();
        }

        private static void write(@NotNull RegistryFriendlyByteBuf buffer, @NotNull MilkIngredient ingredient) {
            //nothing to write here
        }
    }

}
