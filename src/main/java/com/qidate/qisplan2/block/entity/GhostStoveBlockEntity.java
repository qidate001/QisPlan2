package com.qidate.qisplan2.block.entity;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.menu.GhostStoveMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
            GhostStoveBlockEntity blockEntity
    ) {
        if (level.isClientSide()) {
            return;
        }

        /*
         * 目前还没接真正配方。
         */
        if (!blockEntity.hasRecipe()) {
            blockEntity.cookTime = 0;
            blockEntity.setChanged();
            return;
        }

        blockEntity.cookTime++;

        if (blockEntity.cookTime
                >= blockEntity.cookTimeTotal) {

            blockEntity.craft();

            blockEntity.cookTime = 0;
        }

        blockEntity.setChanged();
    }

    private boolean hasRecipe() {

        /*
         * 第一行：
         *
         * 香火灰  @  香火灰
         */
        if (!isItem(
                0,
                QisPlan2.INCENSE_ASH.get()
        )) {
            return false;
        }

        if (!isItem(
                1,
                QisPlan2.INCENSE_ASH.get()
        )) {
            return false;
        }

        /*
         * 第二行：
         *
         * 小麦  小麦  小麦
         */
        if (!isItem(
                2,
                Items.WHEAT
        )) {
            return false;
        }

        if (!isItem(
                3,
                Items.WHEAT
        )) {
            return false;
        }

        if (!isItem(
                4,
                Items.WHEAT
        )) {
            return false;
        }

        /*
         * 钻石剑不能堆叠。
         *
         * 输出槽必须为空。
         */
        return getItem(OUTPUT_SLOT).isEmpty();
    }

    private boolean isItem(
            int slot,
            Item item
    ) {
        return getItem(slot).is(item);
    }

    private void craft() {

        /*
         * 安全检查。
         */
        if (!hasRecipe()) {
            return;
        }

        /*
         * ========================================
         * 消耗香火灰
         * ========================================
         */
        removeItem(
                0,
                1
        );

        removeItem(
                1,
                1
        );

        /*
         * ========================================
         * 消耗小麦
         * ========================================
         */
        removeItem(
                2,
                1
        );

        removeItem(
                3,
                1
        );

        removeItem(
                4,
                1
        );

        /*
         * ========================================
         * 输出钻石剑
         * ========================================
         */

        ItemStack output =
                getItem(OUTPUT_SLOT);

        if (output.isEmpty()) {

            setItem(
                    OUTPUT_SLOT,
                    new ItemStack(
                            Items.DIAMOND_SWORD
                    )
            );

        } else {

            output.grow(1);

            setItem(
                    OUTPUT_SLOT,
                    output
            );
        }
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