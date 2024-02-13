package cech12.bucketlib.item.crafting;

import net.minecraft.util.StringRepresentable;

import javax.annotation.Nonnull;

public enum BucketFillingType implements StringRepresentable {

    BLOCK("block"),
    ENTITY("entity"),
    FLUID("fluid"),
    MILK("milk");

    public static final StringRepresentable.EnumCodec<BucketFillingType> CODEC = StringRepresentable.fromEnum(BucketFillingType::values);

    private final String name;

    BucketFillingType(String name) {
        this.name = name;
    }

    @Override
    @Nonnull
    public String getSerializedName() {
        return this.name;
    }

}
