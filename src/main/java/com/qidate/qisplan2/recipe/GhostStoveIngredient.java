package com.qidate.qisplan2.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Optional;

public record GhostStoveIngredient(
        Optional<Ingredient> ingredient,
        int count
) {

    /**
     * JSON：
     *
     * {
     *   "ingredient": {...},
     *   "count": 2
     * }
     *
     * 空槽：
     *
     * {}
     */
    public static final Codec<GhostStoveIngredient> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(

                            Ingredient.CODEC
                                    .optionalFieldOf(
                                            "ingredient"
                                    )
                                    .forGetter(
                                            GhostStoveIngredient::ingredient
                                    ),

                            Codec.intRange(
                                            0,
                                            64
                                    )
                                    .optionalFieldOf(
                                            "count",
                                            0
                                    )
                                    .forGetter(
                                            GhostStoveIngredient::count
                                    )

                    ).apply(
                            instance,
                            GhostStoveIngredient::new
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostStoveIngredient
            > STREAM_CODEC =
            StreamCodec.composite(

                    ByteBufCodecs.optional(
                            Ingredient.CONTENTS_STREAM_CODEC
                    ),
                    GhostStoveIngredient::ingredient,

                    ByteBufCodecs.VAR_INT,
                    GhostStoveIngredient::count,

                    GhostStoveIngredient::new
            );

    /**
     * 是否为空槽。
     */
    public boolean isEmpty() {
        return ingredient.isEmpty()
                || count <= 0;
    }

    /**
     * 判断当前槽是否满足要求。
     */
    public boolean matches(
            ItemStack stack
    ) {
        if (isEmpty()) {

            return stack.isEmpty();
        }

        return stack.getCount() >= count
                && ingredient
                .get()
                .test(stack);
    }
}