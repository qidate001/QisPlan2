package com.qidate.qisplan2.client.screen;

import com.qidate.qisplan2.menu.GhostStoveMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class GhostStoveScreen
        extends AbstractContainerScreen<GhostStoveMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "qisplan2",
                    "textures/gui/ghost_stove.png"
            );

    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "qisplan2",
                    "textures/gui/ghost_stove_arrow.png"
            );

    /*
     * 这里先假设你的 GUI 图片是 176×166。
     *
     * 后面你换成自己的尺寸时一起改。
     */
    private static final int TEXTURE_WIDTH = 176;
    private static final int TEXTURE_HEIGHT = 166;

    public GhostStoveScreen(
            GhostStoveMenu menu,
            net.minecraft.world.entity.player.Inventory inventory,
            Component title
    ) {
        super(
                menu,
                inventory,
                title
        );

        this.imageWidth =
                TEXTURE_WIDTH;

        this.imageHeight =
                TEXTURE_HEIGHT;
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        int x =
                (width - imageWidth) / 2;

        int y =
                (height - imageHeight) / 2;

        /*
         * ==============================
         * GUI 背景
         * ==============================
         */
        graphics.blit(
                TEXTURE,
                x,
                y,
                0,
                0,
                imageWidth,
                imageHeight,
                imageWidth,
                imageHeight
        );

        /*
         * ==============================
         * 炼制进度箭头
         * ==============================
         */

        int progress =
                menu.getCookProgress();

        if (progress <= 0) {
            return;
        }

        /*
         * 箭头原始尺寸。
         */
        int arrowWidth = 24;
        int arrowHeight = 17;

        /*
         * 根据炼制进度计算当前显示宽度。
         */
        int filledWidth =
                arrowWidth * progress / 100;

        /*
         * 鬼灶台箭头位置。
         */
        int arrowX =
                x + 100;

        int arrowY =
                y + 38;

        /*
         * 从 ghost_stove_arrow.png
         * 的左侧逐渐裁剪出来。
         */
        graphics.blit(
                ARROW_TEXTURE,
                arrowX,
                arrowY,
                0,
                0,
                filledWidth,
                arrowHeight,
                arrowWidth,
                arrowHeight
        );
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {

        renderBackground(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        renderTooltip(
                graphics,
                mouseX,
                mouseY
        );
    }
}