package de.cech12.bucketlib.api.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.api.item.UniversalBucketItem;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Stream;

public class EmptyIngredient extends Ingredient {

    protected Item item;
    protected TagKey<Item> tag;
    private ItemStack[] matchingStacks;

    public EmptyIngredient(Item item, TagKey<Item> tag) {
        super(Stream.of());
        this.item = item;
        this.tag = tag;
    }

    public EmptyIngredient(Item item) {
        this(item, null);
    }

    public EmptyIngredient(TagKey<Item> tag) {
        this(null, tag);
    }

    public EmptyIngredient() {
        this((Item) null, null);
    }

    public EmptyIngredient(Optional<ResourceLocation> itemOptional, Optional<TagKey<Item>> tagOptional) {
        this(itemOptional.map(BuiltInRegistries.ITEM::get).orElse(null), tagOptional.orElse(null));
    }

    @Override
    public boolean test(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        if (this.item == null && this.tag == null && itemStack.getItem() == Items.BUCKET) {
            return true;
        }
        if (this.item != null && itemStack.getItem() == this.item
                || this.tag != null && itemStack.is(this.tag)
                || this.item == null && this.tag == null && itemStack.getItem() instanceof UniversalBucketItem) {
            return BucketLibUtil.isEmpty(itemStack);
        }
        return false;
    }

    @Override
    @Nonnull
    public ItemStack[] getItems() {
        if (this.matchingStacks == null) {
            ArrayList<ItemStack> stacks = new ArrayList<>();
            if (this.item == null && this.tag == null) {
                stacks.add(new ItemStack(Items.BUCKET));
            }
            BucketLibMod.getRegisteredBuckets().forEach(universalBucketItem -> {
                ItemStack universalBucketItemStack = new ItemStack(universalBucketItem);
                if (this.item != null && universalBucketItem == this.item
                        || this.tag != null && universalBucketItemStack.is(this.tag)
                        || this.item == null && this.tag == null) {
                    stacks.add(universalBucketItemStack);
                }
            });
            this.matchingStacks = stacks.toArray(new ItemStack[0]);
        }
        return this.matchingStacks;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    public static final Codec<EmptyIngredient> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    ResourceLocation.CODEC.optionalFieldOf("item").forGetter(i -> Optional.of(BuiltInRegistries.ITEM.getKey(i.item))),
                    TagKey.codec(BuiltInRegistries.ITEM.key()).optionalFieldOf("tag").forGetter(i -> Optional.ofNullable(i.tag))
            ).apply(builder, EmptyIngredient::new)
    );

    public static final IngredientType<EmptyIngredient> TYPE = new IngredientType<>(CODEC);

}
