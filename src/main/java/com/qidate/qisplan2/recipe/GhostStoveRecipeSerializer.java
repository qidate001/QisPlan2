package com.qidate.qisplan2.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.List;

public class GhostStoveRecipeSerializer
        implements RecipeSerializer<GhostStoveRecipe> {

    /**
     * JSON 中必须正好有 5 个槽位。
     */
    private static final Codec<List<GhostStoveIngredient>>
            FIVE_INGREDIENTS_CODEC =
            GhostStoveIngredient.CODEC
                    .listOf()
                    .flatXmap(
                            list -> {

                                if (list.size() != 5) {
                                    return DataResult.error(
                                            () ->
                                                    "GhostStoveRecipe 必须有 5 个 ingredients，实际为 "
                                                            + list.size()
                                    );
                                }

                                return DataResult.success(
                                        list
                                );
                            },
                            DataResult::success
                    );

    /**
     * JSON Codec。
     *
     * 注意：
     * RecipeSerializer.codec() 在 1.21.1
     * 要求 MapCodec，而不是 Codec。
     */
    public static final MapCodec<GhostStoveRecipe>
            CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(

                                    FIVE_INGREDIENTS_CODEC
                                            .fieldOf(
                                                    "ingredients"
                                            )
                                            .forGetter(
                                                    GhostStoveRecipe::getSlotIngredients
                                            ),

                                    ItemStack.CODEC
                                            .fieldOf(
                                                    "result"
                                            )
                                            .forGetter(
                                                    GhostStoveRecipe::getResult
                                            ),

                                    Codec.intRange(
                                                    1,
                                                    100000
                                            )
                                            .optionalFieldOf(
                                                    "cooking_time",
                                                    200
                                            )
                                            .forGetter(
                                                    GhostStoveRecipe::getCookingTime
                                            )

                            ).apply(
                                    instance,
                                    GhostStoveRecipe::new
                            )
            );

    /**
     * 网络同步。
     *
     * 固定 5 个槽，所以直接使用 list codec。
     */
    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostStoveRecipe
            > STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.<RegistryFriendlyByteBuf, GhostStoveIngredient>list(5)
                            .apply(
                                    GhostStoveIngredient.STREAM_CODEC
                            ),
                    GhostStoveRecipe::getSlotIngredients,

                    ItemStack.STREAM_CODEC,
                    GhostStoveRecipe::getResult,

                    ByteBufCodecs.VAR_INT,
                    GhostStoveRecipe::getCookingTime,

                    GhostStoveRecipe::new
            );

    @Override
    public MapCodec<GhostStoveRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<
            RegistryFriendlyByteBuf,
            GhostStoveRecipe
            > streamCodec() {
        return STREAM_CODEC;
    }
}