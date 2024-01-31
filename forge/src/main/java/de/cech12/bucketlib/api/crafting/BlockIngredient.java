package de.cech12.bucketlib.api.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.RegistryUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.crafting.ingredients.AbstractIngredient;
import net.minecraftforge.common.crafting.ingredients.IIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class BlockIngredient extends AbstractIngredient {

    protected final Block block;
    protected final TagKey<Block> tag;
    private ItemStack[] matchingStacks;

    private BlockIngredient(Block block, TagKey<Block> tag) {
        super(Stream.of());
        this.block = block;
        this.tag = tag;
    }

    public BlockIngredient(Optional<ResourceLocation> blockOptional, Optional<TagKey<Block>> tagOptional) {
        this(blockOptional.map(ForgeRegistries.BLOCKS::getValue).orElse(null), tagOptional.orElse(null));
    }

    public BlockIngredient(Block block) {
        this(block, null);
    }

    public BlockIngredient(TagKey<Block> tag) {
        this(null, tag);
    }

    @Override
    public boolean test(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
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
                BucketLibMod.getRegisteredBuckets().forEach(bucket -> {
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
    public IIngredientSerializer<? extends Ingredient> serializer() {
        return SERIALIZER;
    }

    public static final Codec<BlockIngredient> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    ResourceLocation.CODEC.optionalFieldOf("block").forGetter(i -> Optional.ofNullable(ForgeRegistries.BLOCKS.getKey(i.block))),
                    TagKey.codec(ForgeRegistries.BLOCKS.getRegistryKey()).optionalFieldOf("tag").forGetter(i -> Optional.ofNullable(i.tag))
            ).apply(builder, BlockIngredient::new)
    );

    public static final IIngredientSerializer<BlockIngredient> SERIALIZER = new IIngredientSerializer<>() {

        @Override
        public Codec<? extends BlockIngredient> codec() {
            return CODEC;
        }

        @Override
        public BlockIngredient read(FriendlyByteBuf buffer) {
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

        @Override
        public void write(@Nonnull FriendlyByteBuf buffer, @Nonnull BlockIngredient ingredient) {
            buffer.writeUtf(ingredient.block != null ? Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(ingredient.block)).toString() : "");
            buffer.writeUtf(ingredient.tag != null ? ingredient.tag.location().toString() : "");
        }
    };
}
