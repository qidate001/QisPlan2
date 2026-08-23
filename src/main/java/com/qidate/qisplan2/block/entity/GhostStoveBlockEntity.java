package com.qidate.qisplan2.block.entity;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.menu.GhostStoveMenu;
import com.qidate.qisplan2.recipe.GhostStoveIngredient;
import com.qidate.qisplan2.recipe.GhostStoveInput;
import com.qidate.qisplan2.recipe.GhostStoveRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class GhostStoveBlockEntity
        extends BlockEntity
        implements Container, MenuProvider {

    /**
     * 0 ~ 4：输入
     * 5：输出
     */
    private static final int INPUT_SLOTS = 5;
    private static final int OUTPUT_SLOT = 5;
    private static final int TOTAL_SLOTS = 6;

    private final NonNullList<ItemStack> items =
            NonNullList.withSize(
                    TOTAL_SLOTS,
                    ItemStack.EMPTY
            );

    private int cookTime = 0;

    private int cookTimeTotal = 200;

    private ResourceLocation activeRecipeId;

    public List<ItemStack> getInputStacks() {
        return List.of(
                items.get(0),
                items.get(1),
                items.get(2),
                items.get(3),
                items.get(4)
        );
    }

    private final ContainerData data =
            new ContainerData() {

                @Override
                public int get(int index) {
                    return switch (index) {
                        case 0 -> cookTime;
                        case 1 -> cookTimeTotal;
                        default -> 0;
                    };
                }

                @Override
                public void set(
                        int index,
                        int value
                ) {
                    switch (index) {
                        case 0 -> cookTime = value;
                        case 1 -> cookTimeTotal = value;
                    }
                }

                @Override
                public int getCount() {
                    return 2;
                }
            };

    public GhostStoveBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                QisPlan2.GHOST_STOVE_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            GhostStoveBlockEntity stove
    ) {
        if (level.isClientSide()) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        GhostStoveInput input =
                new GhostStoveInput(
                        stove.getInputStacks()
                );

        var optional =
                serverLevel.getRecipeManager()
                        .getRecipeFor(
                                QisPlan2.GHOST_STOVE_RECIPE_TYPE.get(),
                                input,
                                serverLevel
                        );

        if (optional.isEmpty()) {

            stove.cookTime = 0;
            stove.setChanged();

            return;
        }

        var holder =
                optional.get();

        GhostStoveRecipe recipe =
                holder.value();

        /*
         * 输出槽装不下：
         * 不开始炼制。
         */
        ItemStack result =
                recipe.assemble(
                        input,
                        serverLevel.registryAccess()
                );

        if (!stove.canAcceptResult(
                result
        )) {
            stove.cookTime = 0;
            stove.setChanged();

            return;
        }

        /*
         * 新配方开始炼制。
         *
         * 目前简单采用：
         * 输入发生变化时重新从 0 开始。
         */
        if (stove.activeRecipeId == null
                || !stove.activeRecipeId.equals(
                holder.id()
        )) {

            stove.activeRecipeId =
                    holder.id();

            stove.cookTime = 0;
        }

        stove.cookTime++;

        stove.cookTimeTotal =
                recipe.getCookingTime();

        if (stove.cookTime
                >= stove.cookTimeTotal) {

            stove.craftRecipe(
                    recipe,
                    serverLevel.registryAccess()
            );

            stove.cookTime = 0;
        }

        stove.setChanged();
    }

    public boolean canAcceptResult(
            ItemStack result
    ) {
        if (result.isEmpty()) {
            return false;
        }

        ItemStack output =
                items.get(
                        OUTPUT_SLOT
                );

        if (output.isEmpty()) {
            return true;
        }

        if (!ItemStack.isSameItemSameComponents(
                output,
                result
        )) {
            return false;
        }

        return output.getCount()
                + result.getCount()
                <= output.getMaxStackSize();
    }

    public void craftRecipe(
            GhostStoveRecipe recipe,
            HolderLookup.Provider registries
    ) {
        GhostStoveInput input =
                new GhostStoveInput(
                        getInputStacks()
                );

        ItemStack result =
                recipe.assemble(
                        input,
                        registries
                );

        if (!canAcceptResult(result)) {
            return;
        }

        /*
         * 消耗每个槽位要求的数量。
         */
        for (int i = 0; i < 5; i++) {

            GhostStoveIngredient ingredient =
                    recipe.getSlotIngredients().get(i);

            if (ingredient.isEmpty()) {
                continue;
            }

            ItemStack stack =
                    items.get(i);

            stack.shrink(
                    ingredient.count()
            );
        }

        /*
         * 输出。
         */
        ItemStack output =
                items.get(
                        OUTPUT_SLOT
                );

        if (output.isEmpty()) {

            items.set(
                    OUTPUT_SLOT,
                    result.copy()
            );

        } else {

            output.grow(
                    result.getCount()
            );
        }

        setChanged();
    }

    public ContainerData getData() {
        return data;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "container.qisplan2.ghost_stove"
        );
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId,
            Inventory inventory,
            Player player
    ) {
        return new GhostStoveMenu(
                containerId,
                inventory,
                this
        );
    }

    // ========================================
    // Container
    // ========================================

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(
            int index
    ) {
        return items.get(index);
    }

    @Override
    public ItemStack removeItem(
            int index,
            int count
    ) {
        ItemStack result =
                ContainerHelper.removeItem(
                        items,
                        index,
                        count
                );

        if (!result.isEmpty()) {
            setChanged();
        }

        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(
            int index
    ) {
        return ContainerHelper.takeItem(
                items,
                index
        );
    }

    @Override
    public void setItem(
            int index,
            ItemStack stack
    ) {
        items.set(
                index,
                stack
        );

        if (stack.getCount()
                > getMaxStackSize()) {

            stack.setCount(
                    getMaxStackSize()
            );
        }

        setChanged();
    }

    @Override
    public boolean stillValid(
            Player player
    ) {
        if (level == null) {
            return false;
        }

        if (level.getBlockEntity(
                worldPosition
        ) != this) {
            return false;
        }

        return player.distanceToSqr(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D
        ) <= 64.0D;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    // ========================================
    // NBT
    // ========================================

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(
                tag,
                registries
        );

        ContainerHelper.loadAllItems(
                tag,
                items,
                registries
        );

        cookTime =
                tag.getInt(
                        "CookTime"
                );

        cookTimeTotal =
                tag.getInt(
                        "CookTimeTotal"
                );

        if (cookTimeTotal <= 0) {
            cookTimeTotal = 200;
        }
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(
                tag,
                registries
        );

        ContainerHelper.saveAllItems(
                tag,
                items,
                registries
        );

        tag.putInt(
                "CookTime",
                cookTime
        );

        tag.putInt(
                "CookTimeTotal",
                cookTimeTotal
        );
    }
}