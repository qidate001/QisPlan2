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
     * 当前已经进行的刻字时间。
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
         * 读取当前铭文
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

        String newText =
                inscriptionBox.getValue();

        /*
         * 获取墓碑当前铭文。
         */
        String oldText = "";

        if (minecraft != null
                && minecraft.level != null) {

            BlockEntity blockEntity =
                    minecraft.level.getBlockEntity(
                            blockPos
                    );

            if (blockEntity
                    instanceof GhostTombstoneBlockEntity tombstone) {

                oldText =
                        tombstone.getInscription();
            }
        }


        /*
         * ========================================================
         * 计算真正的刻字时间。
         *
         * 必须与服务器使用完全相同的算法。
         * ========================================================
         */
        inscriptionDuration =
                calculateInscriptionDuration(
                        oldText,
                        newText
                );


        /*
         * ========================================================
         * 通知服务器开始刻字。
         * ========================================================
         *
         * 即使 duration == 0，也要发送。
         *
         * 例如：
         *
         * 原来「张三」
         * 修改成「」
         *
         * 服务器仍然需要执行最终的擦除。
         *
         * 又例如：
         *
         * 原来「」
         * 修改成「」
         *
         * 服务器会直接完成。
         */
        GhostTombstoneNetwork.sendStartInscription(
                blockPos,
                newText
        );


        /*
         * ========================================================
         * 没有耗时操作。
         * ========================================================
         *
         * 例如空 → 空。
         *
         * 服务器已经收到请求，
         * 客户端直接关闭 GUI 即可。
         */
        if (inscriptionDuration <= 0) {
            finishInscription();
            return;
        }


        /*
         * ========================================================
         * 开始显示客户端进度。
         * ========================================================
         */

        inscriptionTicks = 0;

        inscribing = true;

        inscriptionBox.setEditable(false);

        inscriptionBox.setFocused(false);
    }


    /**
     * 刻字完成。
     *
     * <p>
     * 服务器负责真正保存刻字。
     * 客户端这里只关闭 GUI。
     */
    private void finishInscription() {

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

        /*
         * Enter / 小键盘 Enter。
         */
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
     * 计算一次刻字操作需要的时间。
     *
     * <p>
     * 使用动态规划计算从旧铭文变成新铭文的最小操作时间。
     *
     * <p>
     * 相同字符可以直接保留，不需要时间。
     * 不同字符则可以选择：
     *
     * <ul>
     *     <li>擦除旧字符</li>
     *     <li>刻写新字符</li>
     * </ul>
     */
    private static int calculateInscriptionDuration(
            String oldText,
            String newText
    ) {

        int[] oldCodePoints =
                oldText.codePoints().toArray();

        int[] newCodePoints =
                newText.codePoints().toArray();

        int oldLength =
                oldCodePoints.length;

        int newLength =
                newCodePoints.length;

        int[][] dp =
                new int[
                        oldLength + 1
                        ][
                        newLength + 1
                        ];


        /*
         * ========================================================
         * 旧铭文 → 空
         *
         * 全部擦除。
         * ========================================================
         */

        for (int i = 1; i <= oldLength; i++) {

            dp[i][0] =
                    dp[i - 1][0]
                            + getEraseTicks(
                            oldCodePoints[i - 1]
                    );
        }


        /*
         * ========================================================
         * 空 → 新铭文
         *
         * 全部刻写。
         * ========================================================
         */

        for (int j = 1; j <= newLength; j++) {

            dp[0][j] =
                    dp[0][j - 1]
                            + getInscriptionTicks(
                            newCodePoints[j - 1]
                    );
        }


        /*
         * ========================================================
         * 动态规划。
         * ========================================================
         */

        for (int i = 1; i <= oldLength; i++) {

            for (int j = 1; j <= newLength; j++) {

                int oldCodePoint =
                        oldCodePoints[i - 1];

                int newCodePoint =
                        newCodePoints[j - 1];


                /*
                 * 相同字符：
                 *
                 * 直接保留。
                 */
                if (oldCodePoint == newCodePoint) {

                    dp[i][j] =
                            dp[i - 1][j - 1];

                    continue;
                }


                /*
                 * 删除旧字符。
                 */
                int eraseCost =
                        dp[i - 1][j]
                                + getEraseTicks(
                                oldCodePoint
                        );


                /*
                 * 新增新字符。
                 */
                int inscriptionCost =
                        dp[i][j - 1]
                                + getInscriptionTicks(
                                newCodePoint
                        );


                /*
                 * 选择耗时较少的方案。
                 */
                dp[i][j] =
                        Math.min(
                                eraseCost,
                                inscriptionCost
                        );
            }
        }

        return dp[oldLength][newLength];
    }


    /**
     * 获取刻写一个新字符需要的时间。
     */
    private static int getInscriptionTicks(
            int codePoint
    ) {

        /*
         * 空白字符不需要刻写时间。
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
         * 中文
         * ========================================================
         */

        if (script == Character.UnicodeScript.HAN) {
            return 120;
        }


        /*
         * ========================================================
         * 日文
         * ========================================================
         */

        if (script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA) {
            return 60;
        }


        /*
         * ========================================================
         * 英文
         * ========================================================
         */

        if (script == Character.UnicodeScript.LATIN
                && Character.isLetter(codePoint)) {
            return 30;
        }


        /*
         * ========================================================
         * 下划线、标点、数字以及其他字符
         * ========================================================
         */

        return 10;
    }


    /**
     * 获取擦除一个已有字符需要的时间。
     */
    private static int getEraseTicks(
            int codePoint
    ) {

        /*
         * 空白字符不需要擦除时间。
         */
        if (Character.isWhitespace(codePoint)) {
            return 0;
        }

        Character.UnicodeScript script =
                Character.UnicodeScript.of(
                        codePoint
                );


        /*
         * 中文。
         */
        if (script == Character.UnicodeScript.HAN) {
            return 3;
        }


        /*
         * 日文。
         */
        if (script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA) {
            return 3;
        }


        /*
         * 英文。
         */
        if (script == Character.UnicodeScript.LATIN
                && Character.isLetter(codePoint)) {
            return 3;
        }


        /*
         * 下划线、标点、数字以及其他字符。
         */
        return 1;
    }
}