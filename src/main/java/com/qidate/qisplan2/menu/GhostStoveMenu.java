package com.qidate.qisplan2.menu;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;

public class GhostStoveMenu extends AbstractContainerMenu {

    /*
     * 5 个鬼灶台输入槽。
     */
    private final SimpleContainer inputContainer =
            new SimpleContainer(5);

    public GhostStoveMenu(
            int containerId,
            Inventory inventory
    ) {
        this(
                QisPlan2.GHOST_STOVE_MENU.get(),
                containerId,
                inventory
        );
    }

    public GhostStoveMenu(
            MenuType<?> menuType,
            int containerId,
            Inventory inventory
    ) {
        super(
                menuType,
                containerId
        );

        /*
         * ========================================
         * 鬼灶台 5 格
         *
         * 布局：
         *
         * # @ @ @ #
         * @ # # # @
         * ========================================
         */

        // 第一行
        addSlot(
                new Slot(
                        inputContainer,
                        0,
                        44,
                        26
                )
        );

        addSlot(
                new Slot(
                        inputContainer,
                        1,
                        116,
                        26
                )
        );

        // 第二行
        addSlot(
                new Slot(
                        inputContainer,
                        2,
                        59,
                        50
                )
        );

        addSlot(
                new Slot(
                        inputContainer,
                        3,
                        83,
                        50
                )
        );

        addSlot(
                new Slot(
                        inputContainer,
                        4,
                        107,
                        50
                )
        );


        /*
         * ========================================
         * 玩家背包
         * ========================================
         *
         * 27 个主背包槽
         */
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {

                addSlot(
                        new Slot(
                                inventory,
                                column + row * 9 + 9,
                                8 + column * 18,
                                84 + row * 18
                        )
                );
            }
        }

        /*
         * 玩家快捷栏
         */
        for (int column = 0; column < 9; column++) {

            addSlot(
                    new Slot(
                            inventory,
                            column,
                            8 + column * 18,
                            142
                    )
            );
        }
    }

    @Override
    public ItemStack quickMoveStack(
            Player player,
            int index
    ) {
        ItemStack result =
                ItemStack.EMPTY;

        Slot slot =
                slots.get(index);

        if (!slot.hasItem()) {
            return result;
        }

        ItemStack stack =
                slot.getItem();

        result =
                stack.copy();

        /*
         * 鬼灶台输入槽：
         * 0 ~ 4
         */
        if (index < 5) {

            /*
             * 输入槽 → 玩家背包
             */
            if (!moveItemStackTo(
                    stack,
                    5,
                    slots.size(),
                    true
            )) {
                return ItemStack.EMPTY;
            }

        } else {

            /*
             * 玩家背包 → 鬼灶台
             *
             * 暂时把物品依次塞入
             * 5 个输入槽。
             */
            if (!moveItemStackTo(
                    stack,
                    0,
                    5,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(
                    ItemStack.EMPTY
            );
        } else {
            slot.setChanged();
        }

        return result;
    }

    @Override
    public boolean stillValid(
            Player player
    ) {
        /*
         * 第一版使用 SimpleMenuProvider，
         * 所以暂时直接允许保持打开。
         *
         * 后面如果给鬼灶台增加 BlockEntity，
         * 再改成检查距离和方块是否存在。
         */
        return true;
    }
}