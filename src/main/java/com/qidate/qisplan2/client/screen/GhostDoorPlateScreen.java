package com.qidate.qisplan2.client.screen;

import com.qidate.qisplan2.network.QisNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;

public class GhostDoorPlateScreen extends Screen {

    private final BlockPos blockPos;

    private EditBox numberBox;

    public GhostDoorPlateScreen(
            BlockPos blockPos
    ) {
        super(
                Component.literal("鬼门牌")
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

        int boxWidth = 120;
        int boxHeight = 20;

        int boxX =
                this.width / 2 - boxWidth / 2;

        int boxY =
                this.height / 2 - 25;

        numberBox =
                new EditBox(
                        this.font,
                        boxX,
                        boxY,
                        boxWidth,
                        boxHeight,
                        Component.literal("门牌号")
                );

        /*
         * 只允许输入数字。
         */
        numberBox.setFilter(
                text -> text.matches("\\d*")
        );

        /*
         * 最大长度。
         */
        numberBox.setMaxLength(9);

        /*
         * 读取当前门牌号。
         */
        Minecraft.getInstance();

        if (minecraft != null
                && minecraft.level != null) {

            BlockEntity blockEntity =
                    minecraft.level.getBlockEntity(
                            blockPos
                    );

            if (blockEntity
                    instanceof com.qidate.qisplan2.block.entity.GhostDoorPlateBlockEntity ghostDoorPlate) {

                numberBox.setValue(
                        String.valueOf(
                                ghostDoorPlate.getNumber()
                        )
                );
            } else {

                numberBox.setValue("666");
            }

        } else {

            numberBox.setValue("666");
        }

        /*
         * 自动选中输入框。
         */
        numberBox.setFocused(true);

        addRenderableWidget(
                numberBox
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
     * 确认修改。
     */
    private void confirm() {

        String text =
                numberBox.getValue();

        /*
         * 空输入不处理。
         */
        if (text.isEmpty()) {
            return;
        }

        int number;

        try {

            number =
                    Integer.parseInt(text);

        } catch (NumberFormatException ignored) {

            return;
        }

        /*
         * 发送到服务器。
         */
        QisNetwork.sendSetGhostDoorPlateNumber(
                blockPos,
                number
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
         * 背景。
         */
        renderBackground(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        /*
         * 标题。
         */
        graphics.drawCenteredString(
                this.font,
                this.title,
                this.width / 2,
                this.height / 2 - 55,
                0xFFFFFFFF
        );

        /*
         * 输入框 + 按钮。
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