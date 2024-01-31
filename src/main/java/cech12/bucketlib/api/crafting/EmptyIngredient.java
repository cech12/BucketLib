package cech12.bucketlib.api.crafting;

import cech12.bucketlib.BucketLib;
import cech12.bucketlib.api.BucketLibApi;
import cech12.bucketlib.util.BucketLibUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Objects;
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
        this(null, null);
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
            BucketLib.getRegisteredBuckets().forEach(universalBucketItem -> {
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
    public IIngredientSerializer<? extends Ingredient> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Nonnull
    public JsonElement toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", Serializer.NAME.toString());
        if (this.item != null) {
            jsonObject.addProperty("item", Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(this.item)).toString());
        }
        if (this.tag != null) {
            jsonObject.addProperty("tag", this.tag.location().toString());
        }
        return jsonObject;
    }

    public static final class Serializer implements IIngredientSerializer<EmptyIngredient> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation NAME = new ResourceLocation(BucketLibApi.MOD_ID, "empty");

        private Serializer() {}

        @Nonnull
        @Override
        public EmptyIngredient parse(@Nonnull FriendlyByteBuf buffer) {
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

        @Nonnull
        @Override
        public EmptyIngredient parse(@Nonnull JsonObject json) {
            if (json.has("item")) {
                ResourceLocation item = new ResourceLocation(json.get("item").getAsString());
                if (!ForgeRegistries.ITEMS.containsKey(item)) {
                    throw new JsonSyntaxException("Unknown item: " + item);
                }
                return new EmptyIngredient(ForgeRegistries.ITEMS.getValue(item));
            } else if (json.has("tag")) {
                ResourceLocation tagId = new ResourceLocation(json.get("tag").getAsString());
                TagKey<Item> tag = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), tagId);
                return new EmptyIngredient(tag);
            }
            return new EmptyIngredient();
        }

        @Override
        public void write(@Nonnull FriendlyByteBuf buffer, @Nonnull EmptyIngredient ingredient) {
            buffer.writeUtf(ingredient.item != null ? Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(ingredient.item)).toString() : "");
            buffer.writeUtf(ingredient.tag != null ? ingredient.tag.location().toString() : "");
        }
    }
}
