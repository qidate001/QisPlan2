package com.qidate.qisplan2.client.screen;

import com.qidate.qisplan2.block.entity.GhostTombstoneBlockEntity;
import com.qidate.qisplan2.network.ghosttombstone.GhostTombstoneNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

public class GhostTombstoneScreen
        extends Screen {

    private final BlockPos blockPos;

    private EditBox inscriptionBox;


    public GhostTombstoneScreen(
            BlockPos blockPos
    ) {

        super(
                Component.literal("鬼墓碑")
        );

        this.blockPos = blockPos;
    }


    @Override
    protected void init() {

        super.init();

        /*
         * ========================================================
         * 输入框
         * ========================================================
         */

        int boxWidth = 240;
        int boxHeight = 20;

        int boxX =
                this.width / 2 - boxWidth / 2;

        int boxY =
                this.height / 2 - 25;

        inscriptionBox =
                new EditBox(
                        this.font,
                        boxX,
                        boxY,
                        boxWidth,
                        boxHeight,
                        Component.literal("刻字")
                );

        /*
         * 最大长度。
         */
        inscriptionBox.setMaxLength(64);


        /*
         * ========================================================
         * 读取当前刻字
         * ========================================================
         */

        if (minecraft != null
                && minecraft.level != null) {

            BlockEntity blockEntity =
                    minecraft.level.getBlockEntity(
                            blockPos
                    );

            if (blockEntity
                    instanceof GhostTombstoneBlockEntity tombstone) {

                inscriptionBox.setValue(
                        tombstone.getInscription()
                );
            }
        }


        /*
         * ========================================================
         * 加入 GUI
         * ========================================================
         */

        addRenderableWidget(
                inscriptionBox
        );


        /*
         * 打开 GUI 后自动获得键盘焦点。
         */
        setInitialFocus(
                inscriptionBox
        );


        /*
         * ========================================================
         * 确定按钮
         * ========================================================
         */

        int buttonWidth = 80;
        int buttonHeight = 20;

        int buttonX =
                this.width / 2 - buttonWidth / 2;

        int buttonY =
                this.height / 2 + 10;

        addRenderableWidget(
                Button.builder(
                        Component.literal("确定"),
                        button -> confirm()
                ).bounds(
                        buttonX,
                        buttonY,
                        buttonWidth,
                        buttonHeight
                ).build()
        );
    }


    /**
     * 确认刻字。
     */
    private void confirm() {

        String text =
                inscriptionBox.getValue();

        /*
         * 发送到服务器保存。
         */
        GhostTombstoneNetwork.sendSetInscription(
                blockPos,
                text
        );

        /*
         * 关闭 GUI。
         */
        onClose();
    }


    /**
     * 按 Enter 也可以确定。
     */
    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {

        if (keyCode == 257
                || keyCode == 335) {

            confirm();

            return true;
        }

        return super.keyPressed(
                keyCode,
                scanCode,
                modifiers
        );
    }


    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {

        /*
         * ========================================================
         * 背景
         * ========================================================
         */

        renderBackground(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );


        /*
         * ========================================================
         * 标题
         * ========================================================
         */

        graphics.drawCenteredString(
                this.font,
                this.title,
                this.width / 2,
                this.height / 2 - 55,
                0xFFFFFFFF
        );


        /*
         * ========================================================
         * 输入框 + 按钮
         * ========================================================
         */

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }


    @Override
    public boolean isPauseScreen() {
        return false;
    }
}