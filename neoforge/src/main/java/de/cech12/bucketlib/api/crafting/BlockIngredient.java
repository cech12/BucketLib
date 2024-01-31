package de.cech12.bucketlib.api.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.cech12.bucketlib.BucketLibMod;
import de.cech12.bucketlib.util.BucketLibUtil;
import de.cech12.bucketlib.util.RegistryUtil;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.crafting.IngredientType;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    @Nonnull
    public ItemStack[] getItems() {
        if (this.matchingStacks == null) {
            ArrayList<ItemStack> stacks = new ArrayList<>();
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

    public static final Codec<BlockIngredient> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    ResourceLocation.CODEC.optionalFieldOf("block").forGetter(i -> Optional.of(BuiltInRegistries.BLOCK.getKey(i.block))),
                    TagKey.codec(BuiltInRegistries.BLOCK.key()).optionalFieldOf("tag").forGetter(i -> Optional.ofNullable(i.tag))
            ).apply(builder, BlockIngredient::new)
    );

    public static final IngredientType<BlockIngredient> TYPE = new IngredientType<>(CODEC);

}
