package com.qidate.qisplan2.client.gui;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.QisPlan2Client;
import com.qidate.qisplan2.core.ModAttachments;
import com.qidate.qisplan2.ghost.PossessedGhostState;
import com.qidate.qisplan2.ghost.PossessionHandler;
import com.qidate.qisplan2.ghost.ability.GhostAbilityRegistry;
import com.qidate.qisplan2.ghost.ability.PossessedGhostAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class PossessionScreen extends Screen {

    private static final int PANEL_WIDTH = 600;
    private static final int PANEL_HEIGHT = 320;

    private int panelX;
    private int panelY;

    private static final ResourceLocation HUMAN_BODY =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "textures/gui/body/human_body.png"
            );

    private static final ResourceLocation BRAIN =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "textures/gui/body/brain.png"
            );

    private static final ResourceLocation HEART =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "textures/gui/body/heart.png"
            );

    public PossessionScreen() {

        super(
                Component.literal(
                        "驭鬼者状态"
                )
        );
    }

    @Override
    protected void init() {

        panelX =
                (this.width - PANEL_WIDTH) / 2;

        panelY =
                (this.height - PANEL_HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        drawPanel(graphics);
        drawContent(graphics);
    }

    private void drawPanel(
            GuiGraphics graphics
    ) {

        /*
         * 外框。
         */
        graphics.fill(
                panelX - 1,
                panelY - 1,
                panelX + PANEL_WIDTH + 1,
                panelY + PANEL_HEIGHT + 1,
                0xFF606060
        );

        /*
         * 主背景。
         */
        graphics.fill(
                panelX,
                panelY,
                panelX + PANEL_WIDTH,
                panelY + PANEL_HEIGHT,
                0xF0101016
        );

        /*
         * 顶部标题区域。
         */
        graphics.fill(
                panelX,
                panelY,
                panelX + PANEL_WIDTH,
                panelY + 24,
                0xF0181820
        );

        /*
         * 标题。
         */
        graphics.drawCenteredString(
                this.font,
                "驭鬼者状态",
                panelX + PANEL_WIDTH / 2,
                panelY + 7,
                0xFFFFFFFF
        );
    }

    private void drawContent(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        var player = minecraft.player;

        Map<ResourceLocation, PossessedGhostState> ghosts =
                player.getData(ModAttachments.POSSESSED_GHOSTS);

        double bodyCorrosion =
                PossessionHandler.getEffectiveBodyCorrosion(player);

        double damageReduction =
                1.0D - Math.pow(0.5D, bodyCorrosion / 20.0D);
        damageReduction = Math.clamp(damageReduction, 0.0D, 0.90D);

        double maxHealthBonus =
                40.0D * (1.0D - Math.pow(0.5D, bodyCorrosion / 20.0D));

        // =========================
        // 左侧：基础状态
        // =========================

        int leftX = panelX + 14;
        int topY = panelY + 38;

        graphics.drawString(
                this.font,
                "驾驭鬼数量",
                leftX,
                topY,
                0xFFAAAAAA
        );

        graphics.drawString(
                this.font,
                String.valueOf(ghosts.size()),
                leftX + 110,
                topY,
                0xFFFFFFFF
        );

        topY += 18;

        graphics.drawString(
                this.font,
                "肉身侵蚀",
                leftX,
                topY,
                0xFFAAAAAA
        );

        graphics.drawString(
                this.font,
                String.format("%.1f", bodyCorrosion),
                leftX + 110,
                topY,
                0xFFFFFFFF
        );

        topY += 18;

        graphics.drawString(
                this.font,
                "非灵异减伤",
                leftX,
                topY,
                0xFFAAAAAA
        );

        graphics.drawString(
                this.font,
                String.format("%.1f%%", damageReduction * 100.0D),
                leftX + 110,
                topY,
                0xFFFFFFFF
        );

        topY += 18;

        graphics.drawString(
                this.font,
                "生命上限加成",
                leftX,
                topY,
                0xFFAAAAAA
        );

        graphics.drawString(
                this.font,
                String.format("+%.1f", maxHealthBonus),
                leftX + 110,
                topY,
                0xFFFFFFFF
        );


        // =========================
        // 中央：人体图
        // =========================

        int bodyWidth = 168;
        int bodyHeight = 278;

        int bodyX =
                panelX + (PANEL_WIDTH - bodyWidth) / 2;

        int bodyY =
                panelY + 26;

        graphics.blit(
                HUMAN_BODY,
                bodyX,
                bodyY,
                0,
                0,
                bodyWidth,
                bodyHeight,
                bodyWidth,
                bodyHeight
        );

        // 大脑
        graphics.setColor(
                1.0F,
                0.2F,
                0.2F,
                0.75F
        );

        graphics.blit(
                BRAIN,
                bodyX,
                bodyY,
                0,
                0,
                bodyWidth,
                bodyHeight,
                bodyWidth,
                bodyHeight
        );

        graphics.setColor(
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );


        // =========================
        // 右侧：驾驭的鬼
        // =========================

        int rightX =
                panelX + PANEL_WIDTH - 145;

        int rightY =
                panelY + 38;

        graphics.drawString(
                this.font,
                "驾驭的鬼",
                rightX,
                rightY,
                0xFFFFFFFF
        );

        rightY += 20;

        for (Map.Entry<ResourceLocation, PossessedGhostState> entry
                : ghosts.entrySet()) {

            ResourceLocation ghostId = entry.getKey();
            PossessedGhostState state = entry.getValue();

            String name = getGhostName(ghostId);

            graphics.drawString(
                    this.font,
                    name,
                    rightX,
                    rightY,
                    0xFFDDDDDD
            );

            graphics.drawString(
                    this.font,
                    String.format(
                            "%.0f%%",
                            state.revival() * 100.0D
                    ),
                    rightX,
                    rightY + 12,
                    0xFFFFFFFF
            );

            rightY += 32;

            if (rightY > panelY + PANEL_HEIGHT - 20) {
                break;
            }
        }
    }

    private String getGhostName(
            ResourceLocation id
    ) {

        PossessedGhostAbility ability =
                GhostAbilityRegistry.get(
                        id
                );

        if (ability != null) {

            return ability
                    .displayName()
                    .getString();
        }

        return id.toString();
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
                QisPlan2Client
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