package com.qidate.qisplan2.client.screen;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.block.entity.GhostTombstoneBlockEntity;
import com.qidate.qisplan2.network.ghosttombstone.GhostTombstoneNetwork;
import com.qidate.qisplan2.core.ModSounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;

public class GhostTombstoneScreen
        extends Screen {

    private final BlockPos blockPos;

    private EditBox inscriptionBox;


    /*
     * ========================================================
     * 刻字进度
     * ========================================================
     */

    /**
     * 当前这次刻字所需的总时间。
     */
    private int inscriptionDuration = 0;

    /**
     * 每 0.6 秒播放一次音效。
     */
    private static final int INSCRIPTION_SOUND_INTERVAL =
            12;

    /**
     * 当前已经刻字了多少 tick。
     */
    private int inscriptionTicks = 0;

    /**
     * 是否正在刻字。
     */
    private boolean inscribing = false;


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
                        button -> startInscription()
                ).bounds(
                        buttonX,
                        buttonY,
                        buttonWidth,
                        buttonHeight
                ).build()
        );
    }


    /**
     * 开始刻字。
     */
    private void startInscription() {

        if (inscribing) {
            return;
        }

        String text =
                inscriptionBox.getValue();

        inscriptionDuration =
                calculateInscriptionDuration(
                        text
                );

        if (inscriptionDuration <= 0) {
            return;
        }

        /*
         * ========================================================
         * 通知服务器开始真正的刻字过程。
         * ========================================================
         */
        GhostTombstoneNetwork.sendStartInscription(
                blockPos,
                text
        );

        /*
         * ========================================================
         * 客户端开始显示进度。
         * ========================================================
         */
        inscriptionTicks = 0;

        inscribing = true;

        inscriptionBox.setEditable(false);

        inscriptionBox.setFocused(false);
    }


    /**
     * 刻字完成。
     */
    private void finishInscription() {

        /*
         * 服务器负责真正保存刻字。
         *
         * 客户端这里只关闭 GUI。
         */
        onClose();
    }


    /**
     * 每一个客户端 Tick。
     */
    @Override
    public void tick() {

        super.tick();

        if (!inscribing) {
            return;
        }

        inscriptionTicks++;

        /*
         * ========================================================
         * 完成
         * ========================================================
         */

        if (inscriptionTicks >= inscriptionDuration) {

            finishInscription();
        }
    }


    /**
     * 按 Enter 确定。
     */
    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {

        /*
         * 刻字过程中禁止再次确定。
         */
        if (inscribing) {
            return true;
        }

        if (keyCode == 257
                || keyCode == 335) {

            startInscription();

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
         * 输入框 + 按钮
         * ========================================================
         */

        super.render(
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
         * 刻字进度
         * ========================================================
         */

        if (inscribing) {

            int barWidth = 240;
            int barHeight = 8;

            int barX =
                    this.width / 2
                            - barWidth / 2;

            int barY =
                    this.height / 2 + 40;


            /*
             * 外框。
             */
            graphics.fill(
                    barX - 1,
                    barY - 1,
                    barX + barWidth + 1,
                    barY + barHeight + 1,
                    0xFF555555
            );


            /*
             * 背景。
             */
            graphics.fill(
                    barX,
                    barY,
                    barX + barWidth,
                    barY + barHeight,
                    0xFF202020
            );


            /*
             * 当前进度。
             */
            int progressWidth =
                    Math.round(
                            barWidth
                                    * (
                                    (float) inscriptionTicks
                                            / inscriptionDuration
                            )
                    );

            graphics.fill(
                    barX,
                    barY,
                    barX + progressWidth,
                    barY + barHeight,
                    0xFFAAAAAA
            );


            /*
             * 剩余时间。
             */
            int remainingTicks =
                    Math.max(
                            0,
                            inscriptionDuration
                                    - inscriptionTicks
                    );

            int remainingSeconds =
                    (remainingTicks + 19) / 20;


            graphics.drawCenteredString(
                    this.font,
                    "刻字中…… "
                            + remainingSeconds
                            + "s",
                    this.width / 2,
                    barY + 14,
                    0xFFFFFFFF
            );
        }
    }


    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * 获取单个 Unicode 字符的刻字时间。
     *
     * @param codePoint Unicode Code Point
     * @return 刻字所需 tick
     */
    private static int getInscriptionTicks(
            int codePoint
    ) {

        /*
         * 空格、制表符、换行等空白字符
         * 不计入刻字时间。
         */
        if (Character.isWhitespace(codePoint)) {
            return 0;
        }


        Character.UnicodeScript script =
                Character.UnicodeScript.of(
                        codePoint
                );


        /*
         * ========================================================
         * 英文字母
         * ========================================================
         *
         * 暂时把 Latin 字母全部按照英文字母处理。
         */
        if (
                script == Character.UnicodeScript.LATIN
                        && Character.isLetter(codePoint)
        ) {
            return 30;
        }


        /*
         * ========================================================
         * 中文汉字
         * ========================================================
         */
        if (
                script == Character.UnicodeScript.HAN
        ) {
            return 120;
        }


        /*
         * ========================================================
         * 日文平假名
         * ========================================================
         */
        if (
                script == Character.UnicodeScript.HIRAGANA
        ) {
            return 60;
        }


        /*
         * ========================================================
         * 日文片假名
         * ========================================================
         */
        if (
                script == Character.UnicodeScript.KATAKANA
        ) {
            return 60;
        }


        /*
         * ========================================================
         * 下划线、标点符号以及其他字符
         * ========================================================
         */
        return 10;
    }

    /**
     * 计算整段文字的刻字时间。
     */
    private static int calculateInscriptionDuration(
            String text
    ) {

        int ticks = 0;

        for (int i = 0; i < text.length();) {

            int codePoint =
                    text.codePointAt(i);

            ticks +=
                    getInscriptionTicks(
                            codePoint
                    );

            i +=
                    Character.charCount(
                            codePoint
                    );
        }

        return ticks;
    }
}