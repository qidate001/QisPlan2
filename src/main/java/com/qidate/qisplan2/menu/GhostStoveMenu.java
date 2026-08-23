package com.qidate.qisplan2.menu;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.block.entity.GhostStoveBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class GhostStoveMenu
        extends AbstractContainerMenu {

    private final Container container;

    /*
     * 客户端 Menu 使用自己的同步数据。
     *
     * 服务端则使用 BlockEntity 的 ContainerData。
     */
    private final net.minecraft.world.inventory.ContainerData data;

    /**
     * 客户端构造。
     *
     * MenuType 的 factory 需要这个签名。
     */
    public GhostStoveMenu(
            int containerId,
            Inventory inventory
    ) {
        this(
                QisPlan2.GHOST_STOVE_MENU.get(),
                containerId,
                inventory,
                new SimpleContainer(6),
                new SimpleContainerData(2)
        );
    }

    /**
     * 服务端构造。
     */
    public GhostStoveMenu(
            int containerId,
            Inventory inventory,
            GhostStoveBlockEntity stove
    ) {
        this(
                QisPlan2.GHOST_STOVE_MENU.get(),
                containerId,
                inventory,
                stove,
                stove.getData()
        );
    }

    private GhostStoveMenu(
            MenuType<?> menuType,
            int containerId,
            Inventory inventory,
            Container container,
            net.minecraft.world.inventory.ContainerData data
    ) {
        super(
                menuType,
                containerId
        );

        this.container = container;
        this.data = data;

        /*
         * ========================================
         * 鬼灶台输入
         *
         * # @ @ @ #
         * @ # # # @
         * ========================================
         */

        addSlot(
                new Slot(
                        container,
                        0,
                        44,
                        26
                )
        );

        addSlot(
                new Slot(
                        container,
                        1,
                        116,
                        26
                )
        );

        addSlot(
                new Slot(
                        container,
                        2,
                        59,
                        50
                )
        );

        addSlot(
                new Slot(
                        container,
                        3,
                        83,
                        50
                )
        );

        addSlot(
                new Slot(
                        container,
                        4,
                        107,
                        50
                )
        );

        /*
         * ========================================
         * 输出槽
         * ========================================
         */

        addSlot(
                new Slot(
                        container,
                        5,
                        152,
                        38
                ) {
                    @Override
                    public boolean mayPlace(
                            ItemStack stack
                    ) {
                        return false;
                    }
                }
        );

        /*
         * ========================================
         * 玩家背包
         * ========================================
         */

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {

                addSlot(
                        new Slot(
                                inventory,
                                column
                                        + row * 9
                                        + 9,
                                8 + column * 18,
                                84 + row * 18
                        )
                );
            }
        }

        /*
         * 快捷栏
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

        /*
         * 同步炼制进度。
         */
        addDataSlots(
                data
        );
    }

    /**
     * 当前炼制进度百分比。
     *
     * 0 ~ 100
     */
    public int getCookProgress() {

        int cookTime =
                data.get(0);

        int cookTimeTotal =
                data.get(1);

        if (cookTimeTotal <= 0) {
            return 0;
        }

        return Math.clamp(
                cookTime * 100
                        / cookTimeTotal,
                0,
                100
        );
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
         * 0~4：鬼灶台输入
         * 5：输出
         */
        if (index < 5) {

            if (!moveItemStackTo(
                    stack,
                    6,
                    slots.size(),
                    true
            )) {
                return ItemStack.EMPTY;
            }

        } else if (index == 5) {

            /*
             * 输出槽 → 玩家背包
             */
            if (!moveItemStackTo(
                    stack,
                    6,
                    slots.size(),
                    true
            )) {
                return ItemStack.EMPTY;
            }

            slot.onQuickCraft(
                    stack,
                    result
            );

        } else {

            /*
             * 玩家背包 → 5 个输入槽
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
        return container.stillValid(
                player
        );
    }
}