package com.qidate.qisplan2.recipe;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class GhostStoveRecipe
        implements Recipe<GhostStoveInput> {

    public static final int SLOT_COUNT = 5;

    private final List<GhostStoveIngredient> slotIngredients;

    private final ItemStack result;

    private final int cookingTime;

    public GhostStoveRecipe(
            List<GhostStoveIngredient> slotIngredients,
            ItemStack result,
            int cookingTime
    ) {
        if (slotIngredients.size() != SLOT_COUNT) {
            throw new IllegalArgumentException(
                    "鬼灶台配方必须正好有 5 个 ingredients，实际："
                            + slotIngredients.size()
            );
        }

        this.slotIngredients =
                List.copyOf(
                        slotIngredients
                );

        this.result =
                result.copy();

        this.cookingTime =
                cookingTime;
    }

    /**
     * 获取鬼灶台自己的 5 格配方定义。
     *
     * 这个方法故意不叫 getIngredients，
     * 因为 Recipe 接口已经占用了 getIngredients()。
     */
    public List<GhostStoveIngredient> getSlotIngredients() {
        return slotIngredients;
    }

    public ItemStack getResult() {
        return result;
    }

    public int getCookingTime() {
        return cookingTime;
    }

    /**
     * 判断 5 个输入槽是否完全符合配方。
     */
    @Override
    public boolean matches(
            GhostStoveInput input,
            Level level
    ) {
        if (input.size() != SLOT_COUNT) {
            return false;
        }

        for (int i = 0; i < SLOT_COUNT; i++) {

            GhostStoveIngredient ingredient =
                    slotIngredients.get(i);

            if (!ingredient.matches(
                    input.getItem(i)
            )) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack assemble(
            GhostStoveInput input,
            HolderLookup.Provider registries
    ) {
        return result.copy();
    }

    @Override
    public ItemStack getResultItem(
            HolderLookup.Provider registries
    ) {
        return result;
    }

    /**
     * Recipe 接口要求的 getIngredients。
     *
     * 这里只返回 Minecraft 标准 Ingredient，
     * 数量信息仍然由 GhostStoveIngredient 保存。
     */
    @Override
    public NonNullList<Ingredient> getIngredients() {

        NonNullList<Ingredient> result =
                NonNullList.withSize(
                        SLOT_COUNT,
                        Ingredient.EMPTY
                );

        for (int i = 0; i < SLOT_COUNT; i++) {

            GhostStoveIngredient ingredient =
                    slotIngredients.get(i);

            if (!ingredient.isEmpty()) {

                result.set(
                        i,
                        ingredient.ingredient().get()
                );
            }
        }

        return result;
    }

    @Override
    public boolean canCraftInDimensions(
            int width,
            int height
    ) {
        return width >= 3
                && height >= 2;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.GHOST_STOVE_RECIPE_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.GHOST_STOVE_RECIPE_SERIALIZER.get();
    }
}