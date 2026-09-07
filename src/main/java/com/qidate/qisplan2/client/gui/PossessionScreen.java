package com.qidate.qisplan2.client.gui;

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

    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 220;

    private int panelX;
    private int panelY;

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

    private void drawContent(
            GuiGraphics graphics
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        var player =
                minecraft.player;

        Map<
                ResourceLocation,
                PossessedGhostState
                > ghosts =
                player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                );

        /*
         * ==============================
         * 基础数据
         * ==============================
         */

        double bodyCorrosion =
                PossessionHandler.getEffectiveBodyCorrosion(
                        player
                );

        double damageReduction =
                1.0D
                        - Math.pow(
                        0.5D,
                        bodyCorrosion / 20.0D
                );

        damageReduction =
                Math.clamp(
                        damageReduction,
                        0.0D,
                        0.90D
                );

        double maxHealthBonus =
                40.0D
                        * (
                        1.0D
                                - Math.pow(
                                0.5D,
                                bodyCorrosion / 20.0D
                        )
                );


        int x =
                panelX + 14;

        int y =
                panelY + 36;


        /*
         * ==============================
         * 身体状态
         * ==============================
         */

        graphics.drawString(
                this.font,
                "驾驭鬼数量",
                x,
                y,
                0xFFAAAAAA
        );

        graphics.drawString(
                this.font,
                String.valueOf(
                        ghosts.size()
                ),
                x + 150,
                y,
                0xFFFFFFFF
        );

        y += 16;


        graphics.drawString(
                this.font,
                "肉身侵蚀",
                x,
                y,
                0xFFAAAAAA
        );

        graphics.drawString(
                this.font,
                String.format(
                        "%.1f",
                        bodyCorrosion
                ),
                x + 150,
                y,
                0xFFFFFFFF
        );

        y += 16;


        graphics.drawString(
                this.font,
                "非灵异减伤",
                x,
                y,
                0xFFAAAAAA
        );

        graphics.drawString(
                this.font,
                String.format(
                        "%.1f%%",
                        damageReduction * 100.0D
                ),
                x + 150,
                y,
                0xFFFFFFFF
        );

        y += 16;


        graphics.drawString(
                this.font,
                "生命上限加成",
                x,
                y,
                0xFFAAAAAA
        );

        graphics.drawString(
                this.font,
                String.format(
                        "+%.1f",
                        maxHealthBonus
                ),
                x + 150,
                y,
                0xFFFFFFFF
        );


        /*
         * ==============================
         * 分隔线
         * ==============================
         */

        y += 18;

        graphics.fill(
                x,
                y,
                panelX + PANEL_WIDTH - 14,
                y + 1,
                0xFF404048
        );

        y += 12;


        /*
         * ==============================
         * 驾驭的鬼
         * ==============================
         */

        graphics.drawString(
                this.font,
                "驾驭的鬼",
                x,
                y,
                0xFFFFFFFF
        );

        y += 16;


        for (Map.Entry<
                ResourceLocation,
                PossessedGhostState
                > entry : ghosts.entrySet()) {

            ResourceLocation ghostId =
                    entry.getKey();

            PossessedGhostState state =
                    entry.getValue();

            String name =
                    getGhostName(
                            ghostId
                    );

            graphics.drawString(
                    this.font,
                    name,
                    x,
                    y,
                    0xFFDDDDDD
            );

            graphics.drawString(
                    this.font,
                    String.format(
                            "%.0f%%",
                            state.revival()
                                    * 100.0D
                    ),
                    x + 150,
                    y,
                    0xFFFFFFFF
            );

            y += 14;

            /*
             * 防止鬼太多超出面板。
             */
            if (y >
                    panelY
                            + PANEL_HEIGHT
                            - 12) {

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