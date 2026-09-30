package com.qidate.qisplan2.client.screen.PossessionScreen;

import com.qidate.qisplan2.client.key.ModKeyMappings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PossessionScreen extends Screen {

    private static final int PANEL_WIDTH = 600;
    private static final int PANEL_HEIGHT = 320;

    private float panelScale = 1.0F;

    private int panelX;
    private int panelY;

    private int currentPage = 0;

    private final PossessionStatusRenderer statusRenderer;

    private final PossessionSuppressionRenderer suppressionRenderer;



    public PossessionScreen() {

        super(
                Component.literal(
                        "驭鬼者状态"
                )
        );

        statusRenderer =
                new PossessionStatusRenderer();

        suppressionRenderer =
                new PossessionSuppressionRenderer();
    }

    @Override
    protected void init() {

        panelScale = Math.min(
                (float) this.width / (PANEL_WIDTH + 2),
                (float) this.height / (PANEL_HEIGHT + 2)
        );

        int scaledWidth =
                Math.round(PANEL_WIDTH * panelScale);

        int scaledHeight =
                Math.round(PANEL_HEIGHT * panelScale);

        panelX =
                (this.width - scaledWidth) / 2;

        panelY =
                (this.height - scaledHeight) / 2;
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        this.renderBackground(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        graphics.pose().pushPose();

        graphics.pose().translate(
                panelX,
                panelY,
                0
        );

        graphics.pose().scale(
                panelScale,
                panelScale,
                1.0F
        );

        drawPanel(graphics);
        drawTabs(graphics);

        if (currentPage == 0) {

            statusRenderer.render(
                    graphics,
                    panelX,
                    panelY,
                    panelScale
            );

        } else {

            suppressionRenderer.render(
                    graphics
            );
        }

        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {

        if (button != 0) {
            return super.mouseClicked(
                    mouseX,
                    mouseY,
                    button
            );
        }

        /*
         * 将屏幕坐标转换为面板局部坐标。
         */
        double localX =
                (mouseX - panelX) / panelScale;

        double localY =
                (mouseY - panelY) / panelScale;

        /**
         * 状态页的滚动条。
         */
        if (currentPage == 0) {

            if (
                    statusRenderer.mouseClicked(
                            localX,
                            localY
                    )
            ) {
                return true;
            }
        }

        /*
         * 灵异页的压制交互。
         */
        if (
                currentPage == 1
                        && button == 0
        ) {

            if (
                    suppressionRenderer.mouseClicked(
                            localX,
                            localY
                    )
            ) {
                return true;
            }
        }

        int tabY = 24;
        int tabWidth = 70;
        int tabHeight = 20;

        int statusX = 12;
        int suppressionX =
                statusX + tabWidth + 4;

        /*
         * 状态页。
         */
        if (isInside(
                localX,
                localY,
                statusX,
                tabY,
                tabWidth,
                tabHeight
        )) {

            currentPage = 0;
            return true;
        }

        /*
         * 灵异页。
         */
        if (isInside(
                localX,
                localY,
                suppressionX,
                tabY,
                tabWidth,
                tabHeight
        )) {

            currentPage = 1;
            return true;
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY
    ) {

        if (currentPage == 0) {

            double localX =
                    (mouseX - panelX)
                            / panelScale;

            double localY =
                    (mouseY - panelY)
                            / panelScale;

            if (
                    statusRenderer.mouseScrolled(
                            localX,
                            localY,
                            scrollY
                    )
            ) {
                return true;
            }
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                scrollX,
                scrollY
        );
    }

    @Override
    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY
    ) {

        /*
         * 将屏幕坐标转换为面板局部坐标。
         */
        double localX =
                (mouseX - panelX)
                        / panelScale;

        double localY =
                (mouseY - panelY)
                        / panelScale;

        /*
         * =========================================================
         * 滚动条拖动
         * =========================================================
         */
        if (
                currentPage == 0
                        && button == 0
        ) {

            if (
                    statusRenderer.mouseDragged(
                            localX,
                            localY
                    )
            ) {
                return true;
            }
        }

        // =========================================================
        // 压制拖动
        // =========================================================
        if (
                currentPage == 1
                        && button == 0
        ) {

            if (
                    suppressionRenderer.mouseDragged(
                            localX,
                            localY
                    )
            ) {
                return true;
            }
        }

        return super.mouseDragged(
                mouseX,
                mouseY,
                button,
                dragX,
                dragY
        );
    }

    @Override
    public boolean mouseReleased(
            double mouseX,
            double mouseY,
            int button
    ) {

        /*
         * =========================================================
         * 状态页滚动条
         * =========================================================
         */
        if (
                currentPage == 0
                        && button == 0
        ) {

            if (
                    statusRenderer.mouseReleased()
            ) {
                return true;
            }
        }

        /*
         * 灵异页压制释放。
         */
        if (
                currentPage == 1
                        && button == 0
        ) {

            double localX =
                    (mouseX - panelX)
                            / panelScale;

            double localY =
                    (mouseY - panelY)
                            / panelScale;

            if (
                    suppressionRenderer.mouseReleased(
                            localX,
                            localY
                    )
            ) {
                return true;
            }
        }

        return super.mouseReleased(
                mouseX,
                mouseY,
                button
        );
    }

    private void drawPanel(
            GuiGraphics graphics
    ) {

        /*
         * 外框。
         */
        graphics.fill(
                -1,
                -1,
                PANEL_WIDTH + 1,
                PANEL_HEIGHT + 1,
                0xFF606060
        );

        /*
         * 主背景。
         */
        graphics.fill(
                0,
                0,
                PANEL_WIDTH,
                PANEL_HEIGHT,
                0xF0101016
        );

        /*
         * 顶部标题区域。
         */
        graphics.fill(
                0,
                0,
                PANEL_WIDTH,
                24,
                0xF0181820
        );

        /*
         * 标题。
         */

        graphics.drawCenteredString(
                this.font,
                "驭鬼者状态",
                PANEL_WIDTH / 2,
                7,
                0xFFFFFFFF
        );
    }

    private void drawTabs(
            GuiGraphics graphics
    ) {

        int tabY = 24;

        int tabWidth = 70;
        int tabHeight = 20;

        int statusX = 12;
        int suppressionX = statusX + tabWidth + 4;

        // =========================
        // 状态
        // =========================

        drawTab(
                graphics,
                statusX,
                tabY,
                tabWidth,
                tabHeight,
                "状态",
                currentPage == 0
        );

        // =========================
        // 灵异
        // =========================

        drawTab(
                graphics,
                suppressionX,
                tabY,
                tabWidth,
                tabHeight,
                "灵异",
                currentPage == 1
        );
    }

    private void drawTab(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            String text,
            boolean selected
    ) {

        int backgroundColor =
                selected
                        ? 0xFF30303A
                        : 0xFF181820;

        int borderColor =
                selected
                        ? 0xFF80808C
                        : 0xFF383840;

        // 外框
        graphics.fill(
                x,
                y,
                x + width,
                y + height,
                borderColor
        );

        // 内部
        graphics.fill(
                x + 1,
                y + 1,
                x + width - 1,
                y + height - 1,
                backgroundColor
        );

        graphics.drawCenteredString(
                this.font,
                text,
                x + width / 2,
                y + 6,
                selected
                        ? 0xFFFFFFFF
                        : 0xFFAAAAAA
        );
    }

    private boolean isInside(
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height
    ) {

        return mouseX >= x
                && mouseX < x + width
                && mouseY >= y
                && mouseY < y + height;
    }

    @Override
    public boolean isPauseScreen() {

        return false;
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {

        /*
         * H 再按一次关闭。
         */
        if (keyCode ==
                ModKeyMappings
                        .OPEN_POSSESSION_SCREEN
                        .getKey()
                        .getValue()) {

            this.onClose();

            return true;
        }

        return super.keyPressed(
                keyCode,
                scanCode,
                modifiers
        );
    }
}