package de.cech12.bucketlib.api.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.util.BucketLibUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.ingredients.AbstractIngredient;
import net.minecraftforge.common.crafting.ingredients.IIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class EmptyIngredient extends AbstractIngredient {

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
        this(itemOptional.map(ForgeRegistries.ITEMS::getValue).orElse(null), tagOptional.orElse(null));
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
                || this.item == null && this.tag == null) {
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

    @Override
    protected void invalidate() {
        this.matchingStacks = null;
    }

    @Override
    @Nonnull
    public IIngredientSerializer<? extends Ingredient> serializer() {
        return SERIALIZER;
    }

    public static final Codec<EmptyIngredient> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    ResourceLocation.CODEC.optionalFieldOf("item").forGetter(i -> Optional.ofNullable(ForgeRegistries.ITEMS.getKey(i.item))),
                    TagKey.codec(ForgeRegistries.ITEMS.getRegistryKey()).optionalFieldOf("tag").forGetter(i -> Optional.ofNullable(i.tag))
            ).apply(builder, EmptyIngredient::new)
    );

    public static final IIngredientSerializer<EmptyIngredient> SERIALIZER = new IIngredientSerializer<>() {

        @Override
        public Codec<? extends EmptyIngredient> codec() {
            return CODEC;
        }

        @Override
        public EmptyIngredient read(FriendlyByteBuf buffer) {
            String item = buffer.readUtf();
            String tagId = buffer.readUtf();
            if (!item.isEmpty()) {
                return new EmptyIngredient(ForgeRegistries.ITEMS.getValue(new ResourceLocation(item)));
            }
            if (!tagId.isEmpty()) {
                TagKey<Item> tag = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), new ResourceLocation(tagId));
                return new EmptyIngredient(tag);
            }
            return new EmptyIngredient();
        }

        @Override
        public void write(@Nonnull FriendlyByteBuf buffer, @Nonnull EmptyIngredient ingredient) {
            buffer.writeUtf(ingredient.item != null ? Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(ingredient.item)).toString() : "");
            buffer.writeUtf(ingredient.tag != null ? ingredient.tag.location().toString() : "");
        }
    };
}
