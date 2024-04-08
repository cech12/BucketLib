package cech12.bucketlib.api.crafting;

import cech12.bucketlib.BucketLib;
import cech12.bucketlib.api.BucketLibApi;
import cech12.bucketlib.util.BucketLibUtil;
import cech12.bucketlib.util.RegistryUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class BlockIngredient extends Ingredient {

    protected final Block block;
    protected final TagKey<Block> tag;
    private ItemStack[] matchingStacks;

    private BlockIngredient(Block block, TagKey<Block> tag) {
        super(Stream.of());
        this.block = block;
        this.tag = tag;
    }

    public BlockIngredient(Block block) {
        this(block, null);
    }

    public BlockIngredient(TagKey<Block> tag) {
        this(null, tag);
    }

    @Override
    public boolean test(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty() || itemStack.getCount() > 1) { //TODO count cannot be limited!
            return false;
        }
        Iterable<Block> blockIterator;
        if (this.block != null) {
            blockIterator = List.of(this.block);
        } else {
            blockIterator = Objects.requireNonNull(ForgeRegistries.BLOCKS.tags()).getTag(this.tag);
        }
        for (Block block : blockIterator) {
            RegistryUtil.BucketBlock bucketBlock = RegistryUtil.getBucketBlock(block);
            if (bucketBlock != null) {
                if (itemStack.getItem() == bucketBlock.bucketItem()) {
                    return true;
                }
                return BucketLibUtil.getBlock(itemStack) == block;
            }
        }
        return false;
    }

    @Override
    @Nonnull
    public ItemStack[] getItems() {
        if (this.matchingStacks == null) {
            ArrayList<ItemStack> stacks = new ArrayList<>();
            List<Block> blocks = new ArrayList<>();
            ITagManager<Block> blockTags = ForgeRegistries.BLOCKS.tags();
            if (this.tag != null && blockTags != null) {
                blockTags.getTag(this.tag).forEach(blocks::add);
            } else if (this.block != null) {
                blocks.add(this.block);
            }
            List<RegistryUtil.BucketBlock> bucketBlocks = RegistryUtil.getBucketBlocks().stream().filter(bucketBlock -> blocks.contains(bucketBlock.block())).toList();
            //vanilla buckets
            for (RegistryUtil.BucketBlock bucketBlock : bucketBlocks) {
                stacks.add(new ItemStack(bucketBlock.bucketItem()));
            }
            //bucket lib buckets
            for (RegistryUtil.BucketBlock bucketBlock : bucketBlocks) {
                BucketLib.getRegisteredBuckets().forEach(bucket -> {
                    if (bucket.canHoldBlock(bucketBlock.block())) {
                        stacks.add(BucketLibUtil.addBlock(new ItemStack(bucket), bucketBlock.block()));
                    }
                });
            }
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
        if (this.block != null) {
            jsonObject.addProperty("entity", Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(this.block)).toString());
        }
        if (this.tag != null) {
            jsonObject.addProperty("tag", this.tag.location().toString());
        }
        return jsonObject;
    }

    public static final class Serializer implements IIngredientSerializer<BlockIngredient> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation NAME = new ResourceLocation(BucketLibApi.MOD_ID, "block");

        private Serializer() {}

        @Nonnull
        @Override
        public BlockIngredient parse(@Nonnull FriendlyByteBuf buffer) {
            String block = buffer.readUtf();
            String tagId = buffer.readUtf();
            if (!tagId.isEmpty()) {
                TagKey<Block> tag = TagKey.create(ForgeRegistries.BLOCKS.getRegistryKey(), new ResourceLocation(tagId));
                return new BlockIngredient(tag);
            }
            if (block.isEmpty()) {
                throw new IllegalArgumentException("Cannot create a block ingredient with no block or tag.");
            }
            return new BlockIngredient(ForgeRegistries.BLOCKS.getValue(new ResourceLocation(block)));
        }

        @Nonnull
        @Override
        public BlockIngredient parse(@Nonnull JsonObject json) {
            if (json.has("tag")) {
                ResourceLocation tagId = new ResourceLocation(json.get("tag").getAsString());
                TagKey<Block> tag = TagKey.create(ForgeRegistries.BLOCKS.getRegistryKey(), tagId);
                return new BlockIngredient(tag);
            } else {
                ResourceLocation block = new ResourceLocation(json.get("block").getAsString());
                if (!ForgeRegistries.BLOCKS.containsKey(block)) {
                    throw new JsonSyntaxException("Unknown block: " + block);
                }
                return new BlockIngredient(ForgeRegistries.BLOCKS.getValue(block));
            }
        }

        @Override
        public void write(@Nonnull FriendlyByteBuf buffer, @Nonnull BlockIngredient ingredient) {
            buffer.writeUtf(ingredient.block != null ? Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(ingredient.block)).toString() : "");
            buffer.writeUtf(ingredient.tag != null ? ingredient.tag.location().toString() : "");
        }
    }
}
