package com.qidate.qisplan2.client.screen.PossessionScreen;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 驭鬼者面板通用滚动条。
 *
 * <p>
 * 负责处理滚动区域所需要的通用功能：
 *
 * <ul>
 *     <li>记录当前滚动位置</li>
 *     <li>根据内容高度计算最大滚动距离</li>
 *     <li>处理鼠标滚轮</li>
 *     <li>处理滚动条滑块拖动</li>
 *     <li>绘制滚动槽和滚动滑块</li>
 * </ul>
 *
 * <p>
 * 这个类不关心具体滚动的是什么内容。
 * 诅咒卡片、驭鬼卡片等都可以共用这个类。
 */
public final class PossessionScrollbar {

    /**
     * 滚动条宽度。
     */
    private static final int WIDTH = 3;

    /**
     * 滚动滑块的最小高度。
     */
    private static final int MIN_THUMB_HEIGHT = 18;

    /**
     * 滚动条所在区域的 X 坐标。
     */
    private final int x;

    /**
     * 滚动条所在区域的 Y 坐标。
     */
    private final int y;

    /**
     * 滚动区域的可视高度。
     */
    private final int height;

    /**
     * 当前滚动位置。
     *
     * <p>
     * 0 表示位于顶部。
     */
    private double scroll;

    /**
     * 当前是否正在拖动滚动条。
     */
    private boolean dragging;

    /**
     * 开始拖动时，
     * 鼠标 Y 坐标相对于滑块顶部的偏移。
     */
    private double draggingOffset;

    /**
     * 创建一个滚动条。
     *
     * @param x      滚动条 X 坐标
     * @param y      滚动条 Y 坐标
     * @param height 可视区域高度
     */
    public PossessionScrollbar(
            int x,
            int y,
            int height
    ) {
        this.x = x;
        this.y = y;
        this.height = height;
    }

    /**
     * 获取当前滚动位置。
     *
     * @return 当前滚动距离
     */
    public double getScroll() {
        return scroll;
    }

    /**
     * 设置滚动位置。
     *
     * <p>
     * 设置时会自动限制在合法范围内。
     *
     * @param scroll        目标滚动位置
     * @param contentHeight 内容总高度
     */
    public void setScroll(
            double scroll,
            int contentHeight
    ) {
        this.scroll =
                clampScroll(
                        scroll,
                        contentHeight
                );
    }

    /**
     * 处理鼠标滚轮。
     *
     * <p>
     * 当内容没有超出可视区域时，
     * 不需要进行滚动。
     *
     * @param scrollY       鼠标滚轮值
     * @param contentHeight 内容总高度
     *
     * @return 是否处理了此次滚动
     */
    public boolean scroll(
            double scrollY,
            int contentHeight
    ) {

        if (contentHeight <= height) {
            scroll = 0.0D;
            return false;
        }

        scroll =
                clampScroll(
                        scroll
                                - scrollY * 20.0D,
                        contentHeight
                );

        return true;
    }

    /**
     * 处理鼠标点击。
     *
     * <p>
     * 如果点击在滚动滑块上，
     * 则开始拖动滚动条。
     *
     * @param mouseX        鼠标 X 坐标
     * @param mouseY        鼠标 Y 坐标
     * @param contentHeight 内容总高度
     *
     * @return 是否点击到了滚动滑块
     */
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int contentHeight
    ) {

        /*
         * 内容没有超出可视区域，
         * 不需要显示滚动条。
         */
        if (contentHeight <= height) {
            return false;
        }

        int thumbHeight =
                getThumbHeight(
                        contentHeight
                );

        int thumbOffset =
                getThumbOffset(
                        contentHeight
                );

        int thumbY =
                y + thumbOffset;

        /*
         * 滚动条本身只有 3 像素宽，
         * 但实际点击范围适当放宽，
         * 方便鼠标操作。
         */
        if (
                mouseX >= x - 4
                        && mouseX <= x + WIDTH + 4
                        && mouseY >= thumbY
                        && mouseY <= thumbY + thumbHeight
        ) {

            dragging = true;

            draggingOffset =
                    mouseY - thumbY;

            return true;
        }

        return false;
    }

    /**
     * 处理鼠标拖动。
     *
     * <p>
     * 根据滑块在滚动条中的位置，
     * 反向计算实际的内容滚动距离。
     *
     * @param mouseX        鼠标 X 坐标
     * @param mouseY        鼠标 Y 坐标
     * @param contentHeight 内容总高度
     *
     * @return 当前是否正在处理滚动条拖动
     */
    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            int contentHeight
    ) {

        if (!dragging) {
            return false;
        }

        int thumbHeight =
                getThumbHeight(
                        contentHeight
                );

        int maxThumbOffset =
                height - thumbHeight;

        int maxScroll =
                Math.max(
                        0,
                        contentHeight - height
                );

        /*
         * 只有当内容确实可以滚动时，
         * 才需要计算新的滚动位置。
         */
        if (
                maxThumbOffset > 0
                        && maxScroll > 0
        ) {

            double thumbOffset =
                    mouseY
                            - y
                            - draggingOffset;

            thumbOffset =
                    Math.clamp(
                            thumbOffset,
                            0.0D,
                            (double) maxThumbOffset
                    );

            scroll =
                    thumbOffset
                            / maxThumbOffset
                            * maxScroll;
        }

        return true;
    }

    /**
     * 处理鼠标释放。
     *
     * @return 是否结束了一次滚动条拖动
     */
    public boolean mouseReleased() {

        if (!dragging) {
            return false;
        }

        dragging = false;
        draggingOffset = 0.0D;

        return true;
    }

    /**
     * 绘制滚动条。
     *
     * <p>
     * 当内容没有超出可视区域时，
     * 不绘制任何东西。
     *
     * @param graphics      Minecraft GUI 绘制对象
     * @param contentHeight 内容总高度
     */
    public void render(
            GuiGraphics graphics,
            int contentHeight
    ) {

        if (contentHeight <= height) {
            return;
        }

        /*
         * =========================
         * 绘制滚动槽
         * =========================
         */

        graphics.fill(
                x,
                y,
                x + WIDTH,
                y + height,
                0x5533333D
        );

        /*
         * =========================
         * 绘制滚动滑块
         * =========================
         */

        int thumbHeight =
                getThumbHeight(
                        contentHeight
                );

        int thumbOffset =
                getThumbOffset(
                        contentHeight
                );

        graphics.fill(
                x,
                y + thumbOffset,
                x + WIDTH,
                y + thumbOffset + thumbHeight,
                0xFF9999A8
        );
    }

    /**
     * 根据内容高度计算滚动滑块高度。
     *
     * <p>
     * 可视区域占全部内容的比例越大，
     * 滑块就越长。
     *
     * @param contentHeight 内容总高度
     *
     * @return 滚动滑块高度
     */
    private int getThumbHeight(
            int contentHeight
    ) {

        if (contentHeight <= height) {
            return height;
        }

        double visibleRatio =
                (double) height
                        / contentHeight;

        return Math.max(
                MIN_THUMB_HEIGHT,
                (int) (
                        height
                                * visibleRatio
                )
        );
    }

    /**
     * 根据当前滚动位置计算滑块偏移。
     *
     * @param contentHeight 内容总高度
     *
     * @return 滑块相对于滚动条顶部的偏移
     */
    private int getThumbOffset(
            int contentHeight
    ) {

        int thumbHeight =
                getThumbHeight(
                        contentHeight
                );

        int maxThumbOffset =
                height - thumbHeight;

        int maxScroll =
                Math.max(
                        0,
                        contentHeight - height
                );

        if (maxScroll <= 0) {
            return 0;
        }

        return (int) (
                scroll
                        / maxScroll
                        * maxThumbOffset
        );
    }

    /**
     * 限制滚动位置。
     *
     * <p>
     * 滚动位置不能小于 0，
     * 也不能超过内容允许的最大滚动距离。
     *
     * @param value         目标滚动位置
     * @param contentHeight 内容总高度
     *
     * @return 限制后的滚动位置
     */
    private double clampScroll(
            double value,
            int contentHeight
    ) {

        int maxScroll =
                Math.max(
                        0,
                        contentHeight - height
                );

        return Math.clamp(
                value,
                0.0D,
                (double) maxScroll
        );
    }
}