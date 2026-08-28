package com.qidate.qisplan2.core;

import com.qidate.qisplan2.recipe.GhostStoveRecipe;
import com.qidate.qisplan2.recipe.GhostStoveRecipeSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.function.Supplier;

import static com.qidate.qisplan2.QisPlan2.MODID;
import static com.qidate.qisplan2.core.ModRegistries.RECIPE_SERIALIZERS;
import static com.qidate.qisplan2.core.ModRegistries.RECIPE_TYPES;

public class ModRecipes {

    private ModRecipes() {}

    public static void init() {
        /*
         * 故意留空。
         *
         * 调用这个方法本身，就会强制 JVM
         * 在正确的时机完成 ModRecipes 的静态初始化。
         */
    }


    // 鬼灶台 Recipe 注册
    public static final Supplier<RecipeType<GhostStoveRecipe>>
            GHOST_STOVE_RECIPE_TYPE =
            RECIPE_TYPES.register(
                    "ghost_stove",
                    () -> RecipeType.simple(
                            ResourceLocation.fromNamespaceAndPath(
                                    MODID,
                                    "ghost_stove"
                            )
                    )
            );

    public static final Supplier<RecipeSerializer<GhostStoveRecipe>>
            GHOST_STOVE_RECIPE_SERIALIZER =
            RECIPE_SERIALIZERS.register(
                    "ghost_stove",
                    GhostStoveRecipeSerializer::new
            );
}
