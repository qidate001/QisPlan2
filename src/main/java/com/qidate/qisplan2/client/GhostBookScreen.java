package com.qidate.qisplan2.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class GhostBookScreen extends Screen {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "qisplan2",
                    "textures/gui/ghost_book.png"
            );

    /*
     * 先按 256 × 256 设计。
     */
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    private int left;
    private int top;

    public GhostBookScreen() {
        super(
                Component.translatable(
                        "screen.qisplan2.ghost_book"
                )
        );
    }

    @Override
    protected void init() {
        super.init();

        /*
         * 居中。
         */
        left =
                (this.width - TEXTURE_WIDTH) / 2;

        top =
                (this.height - TEXTURE_HEIGHT) / 2;
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {

        /*
         * 整体背景变暗。
         */
        this.renderTransparentBackground(
                graphics
        );

        /*
         * 绘制鬼书界面。
         */
        graphics.blit(
                TEXTURE,
                left,
                top,
                0,
                0,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT
        );

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

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}