package de.cech12.bucketlib.api.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.api.BucketLib;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.RegistryUtil;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BlockIngredient implements CustomIngredient {

    protected final Block block;
    protected final TagKey<Block> tag;
    private List<ItemStack> matchingStacks;

    private BlockIngredient(Block block, TagKey<Block> tag) {
        this.block = block;
        this.tag = tag;
    }

    public BlockIngredient(Optional<ResourceLocation> blockOptional, Optional<TagKey<Block>> tagOptional) {
        this(blockOptional.map(BuiltInRegistries.BLOCK::get).orElse(null), tagOptional.orElse(null));
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
        List<RegistryUtil.BucketBlock> bucketBlocks;
        if (this.block != null) {
            RegistryUtil.BucketBlock bucketBlock = RegistryUtil.getBucketBlock(this.block);
            if (bucketBlock == null) {
                return false;
            }
            bucketBlocks = List.of(bucketBlock);
        } else {
            bucketBlocks = RegistryUtil.getBucketBlocks().stream().filter(bucketBlock -> bucketBlock.block().defaultBlockState().is(this.tag)).toList();
        }
        for (RegistryUtil.BucketBlock bucketBlock : bucketBlocks) {
            if (itemStack.getItem() == bucketBlock.bucketItem()) {
                return true;
            }
            return BucketLibUtil.getBlock(itemStack) == bucketBlock.block();
        }
        return false;
    }

    @Override
    public List<ItemStack> getMatchingStacks() {
        if (this.matchingStacks == null) {
            this.matchingStacks = new ArrayList<>();
            List<Block> blocks = new ArrayList<>();
            Optional<HolderSet.Named<Block>> blockTag = Optional.empty();
            if (this.tag != null) {
                blockTag = BuiltInRegistries.BLOCK.getTag(this.tag);
            }
            if (blockTag.isPresent()) {
                blockTag.get().forEach(block -> blocks.add(block.value()));
            } else if (this.block != null) {
                blocks.add(this.block);
            }
            List<RegistryUtil.BucketBlock> bucketBlocks = RegistryUtil.getBucketBlocks().stream().filter(bucketBlock -> blocks.contains(bucketBlock.block())).toList();
            //vanilla buckets
            for (RegistryUtil.BucketBlock bucketBlock : bucketBlocks) {
                this.matchingStacks.add(new ItemStack(bucketBlock.bucketItem()));
            }
            //bucket lib buckets
            for (RegistryUtil.BucketBlock bucketBlock : bucketBlocks) {
                BucketLibMod.getRegisteredBuckets().forEach(bucket -> {
                    if (bucket.canHoldBlock(bucketBlock.block())) {
                        this.matchingStacks.add(BucketLibUtil.addBlock(new ItemStack(bucket), bucketBlock.block()));
                    }
                });
            }
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

    public static final class Serializer implements CustomIngredientSerializer<BlockIngredient> {

        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation NAME = BucketLib.id("block");

        private static final MapCodec<BlockIngredient> CODEC = RecordCodecBuilder.mapCodec(builder ->
                builder.group(
                        ResourceLocation.CODEC.optionalFieldOf("block").forGetter(i -> Optional.of(BuiltInRegistries.BLOCK.getKey(i.block))),
                        TagKey.codec(BuiltInRegistries.BLOCK.key()).optionalFieldOf("tag").forGetter(i -> Optional.ofNullable(i.tag))
                ).apply(builder, BlockIngredient::new)
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, BlockIngredient> PACKET_CODEC = StreamCodec.of(
                de.cech12.bucketlib.api.crafting.BlockIngredient.Serializer::write,
                de.cech12.bucketlib.api.crafting.BlockIngredient.Serializer::read);

        private Serializer() {}

        @Override
        public ResourceLocation getIdentifier() {
            return NAME;
        }

        @Override
        public MapCodec<BlockIngredient> getCodec(boolean allowEmpty) {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BlockIngredient> getPacketCodec() {
            return PACKET_CODEC;
        }

        private static BlockIngredient read(RegistryFriendlyByteBuf buffer) {
            String block = buffer.readUtf();
            String tagId = buffer.readUtf();
            if (!tagId.isEmpty()) {
                TagKey<Block> tag = TagKey.create(Registries.BLOCK, ResourceLocation.parse(tagId));
                return new BlockIngredient(tag);
            }
            if (block.isEmpty()) {
                throw new IllegalArgumentException("Cannot create a block ingredient with no block or tag.");
            }
            return new BlockIngredient(BuiltInRegistries.BLOCK.get(ResourceLocation.parse(block)));
        }

        private static void write(@Nonnull RegistryFriendlyByteBuf buffer, @Nonnull BlockIngredient ingredient) {
            buffer.writeUtf(ingredient.block != null ? Objects.requireNonNull(BuiltInRegistries.BLOCK.getKey(ingredient.block)).toString() : "");
            buffer.writeUtf(ingredient.tag != null ? ingredient.tag.location().toString() : "");
        }
    }

}
