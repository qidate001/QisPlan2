package com.qidate.qisplan2.client;

import com.qidate.qisplan2.network.QisNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class GhostPossessionScreen extends Screen {

    /*
     * ============================================================
     * 横条尺寸
     * ============================================================
     */

    private static final int BAR_WIDTH = 420;
    private static final int BAR_HEIGHT = 20;

    /*
     * 光标块宽度。
     */
    private static final int CURSOR_WIDTH = 34;

    /*
     * 判定点宽度。
     */
    private static final int TARGET_WIDTH = 10;

    public GhostPossessionScreen() {

        super(
                Component.literal("驾驭厉鬼")
        );
    }

    /*
     * ============================================================
     * 渲染
     * ============================================================
     */

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {

        /*
         * ========================================================
         * 半透明压暗整个屏幕
         * ========================================================
         */
        graphics.fill(
                0,
                0,
                this.width,
                this.height,
                0x66000000
        );

        /*
         * ========================================================
         * 当前状态
         * ========================================================
         */
        double success =
                GhostPossessionClientState.getSuccess();

        double cursorPosition =
                GhostPossessionClientState
                        .getCursorPosition();

        double targetPosition =
                GhostPossessionClientState
                        .getTargetPosition();

        int remainingTicks =
                GhostPossessionClientState
                        .getRemainingTicks();


        /*
         * ========================================================
         * 中心位置
         * ========================================================
         */
        int centerX =
                this.width / 2;

        int centerY =
                this.height / 2;


        /*
         * ========================================================
         * 成功率
         * ========================================================
         */
        String successText =
                String.format(
                        "成功率 %.1f%%",
                        success
                );

        int successWidth =
                this.font.width(
                        successText
                );

        graphics.drawString(
                this.font,
                successText,
                centerX - successWidth / 2,
                centerY - 45,
                0xFFFFFFFF
        );


        /*
         * ========================================================
         * 横条位置
         * ========================================================
         */
        int barLeft =
                centerX
                        - BAR_WIDTH / 2;

        int barTop =
                centerY
                        - BAR_HEIGHT / 2;


        /*
         * ========================================================
         * 横条阴影
         * ========================================================
         */
        graphics.fill(
                barLeft - 2,
                barTop - 2,
                barLeft + BAR_WIDTH + 2,
                barTop + BAR_HEIGHT + 2,
                0xAA000000
        );

        /*
         * ========================================================
         * 横条背景
         * ========================================================
         */
        graphics.fill(
                barLeft,
                barTop,
                barLeft + BAR_WIDTH,
                barTop + BAR_HEIGHT,
                0xCC202020
        );


        /*
         * ========================================================
         * 判定点
         * ========================================================
         */
        int targetX =
                barLeft
                        + (int)(
                        targetPosition
                                * (BAR_WIDTH
                                - TARGET_WIDTH)
                );

        graphics.fill(
                targetX,
                barTop,
                targetX + TARGET_WIDTH,
                barTop + BAR_HEIGHT,
                0xFFFFD54A
        );


        /*
         * ========================================================
         * 光标块
         * ========================================================
         */
        int cursorX =
                barLeft
                        + (int)(
                        cursorPosition
                                * (BAR_WIDTH
                                - CURSOR_WIDTH)
                );

        /*
         * 光标是否覆盖判定点。
         */
        boolean aligned =
                Math.abs(
                        cursorPosition
                                - targetPosition
                )
                        <= 0.08D;

        int cursorColor =
                aligned
                        ? 0xFF66FFAA
                        : 0xFFEEEEEE;

        graphics.fill(
                cursorX,
                barTop - 4,
                cursorX + CURSOR_WIDTH,
                barTop + BAR_HEIGHT + 4,
                cursorColor
        );


        /*
         * ========================================================
         * 剩余时间
         * ========================================================
         */
        double seconds =
                remainingTicks / 20.0D;

        String timeText =
                String.format(
                        "%.1fs",
                        Math.max(
                                0.0D,
                                seconds
                        )
                );

        int timeWidth =
                this.font.width(
                        timeText
                );

        graphics.drawString(
                this.font,
                timeText,
                centerX - timeWidth / 2,
                centerY + 30,
                0xFFDDDDDD
        );
    }


    /*
     * ============================================================
     * 键盘
     * ============================================================
     */

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {

        /*
         * 左。
         */
        if (keyCode == GLFW.GLFW_KEY_LEFT) {

            QisNetwork.sendPossessionInput(
                    true,
                    false,
                    false
            );

            return true;
        }

        /*
         * 右。
         */
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {

            QisNetwork.sendPossessionInput(
                    false,
                    true,
                    false
            );

            return true;
        }

        /*
         * 空格。
         */
        if (keyCode == GLFW.GLFW_KEY_SPACE) {

            QisNetwork.sendPossessionInput(
                    false,
                    false,
                    true
            );

            return true;
        }

        /*
         * 下键。
         */
        if (keyCode == GLFW.GLFW_KEY_DOWN) {

            QisNetwork.sendPossessionInput(
                    false,
                    false,
                    true
            );

            return true;
        }

        /*
         * 不允许 ESC 直接退出。
         */
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return true;
        }

        return true;
    }


    /*
     * ============================================================
     * 左右键释放
     * ============================================================
     */

    @Override
    public boolean keyReleased(
            int keyCode,
            int scanCode,
            int modifiers
    ) {

        if (keyCode == GLFW.GLFW_KEY_LEFT
                || keyCode == GLFW.GLFW_KEY_RIGHT) {

            QisNetwork.sendPossessionInput(
                    false,
                    false,
                    false
            );

            return true;
        }

        return super.keyReleased(
                keyCode,
                scanCode,
                modifiers
        );
    }


    /*
     * ============================================================
     * GUI 行为
     * ============================================================
     */

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {

        /*
         * 驾驭过程中不允许主动关闭。
         */
    }
}