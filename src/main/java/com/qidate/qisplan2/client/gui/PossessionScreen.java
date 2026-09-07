package com.qidate.qisplan2.client.gui;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.QisPlan2Client;
import com.qidate.qisplan2.core.ModAttachments;
import com.qidate.qisplan2.ghost.PossessedGhostState;
import com.qidate.qisplan2.ghost.PossessionHandler;
import com.qidate.qisplan2.ghost.ability.GhostAbilityRegistry;
import com.qidate.qisplan2.ghost.ability.PossessedGhostAbility;
import com.qidate.qisplan2.ghost.corrosion.CorrosionMatrix;
import com.qidate.qisplan2.ghost.corrosion.CorrosionType;
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

    private static final ResourceLocation HUMAN_BODY = body("human_body");

    private static final ResourceLocation BRAIN = body("brain");
    private static final ResourceLocation HEART = body("heart");
    private static final ResourceLocation LUNG = body("lung");
    private static final ResourceLocation STOMACH = body("stomach");
    private static final ResourceLocation LIVER = body("liver");
    private static final ResourceLocation KIDNEY = body("kidney");
    private static final ResourceLocation PANCREAS = body("pancreas");
    private static final ResourceLocation GALLBLADDER = body("gallbladder");
    private static final ResourceLocation SPLEEN = body("spleen");
    private static final ResourceLocation INTESTINE = body("intestine");

    private static final ResourceLocation EYE = body("eye");
    private static final ResourceLocation EAR = body("ear");
    private static final ResourceLocation NOSE = body("nose");
    private static final ResourceLocation MOUTH = body("mouth");
    private static final ResourceLocation HAND = body("hand");
    private static final ResourceLocation FOOT = body("foot");

    private static final ResourceLocation LINE = body("line");

    private static ResourceLocation body(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                QisPlan2.MODID,
                "textures/gui/body/" + name + ".png"
        );
    }

    private static final ResourceLocation[] ORGAN_OVERLAYS = {
            BRAIN,
            HEART,
            LUNG,
            STOMACH,
            LIVER,
            KIDNEY,
            PANCREAS,
            GALLBLADDER,
            SPLEEN,
            INTESTINE,
            EYE,
            EAR,
            NOSE,
            MOUTH,
            HAND,
            FOOT
    };

    private static final CorrosionType[] ORGAN_TYPES = {
            CorrosionType.BRAIN,
            CorrosionType.HEART,
            CorrosionType.LUNG,
            CorrosionType.STOMACH,
            CorrosionType.LIVER,
            CorrosionType.KIDNEY,
            CorrosionType.PANCREAS,
            CorrosionType.GALLBLADDER,
            CorrosionType.SPLEEN,
            CorrosionType.INTESTINE,
            CorrosionType.EYE,
            CorrosionType.EAR,
            CorrosionType.NOSE,
            CorrosionType.MOUTH,
            CorrosionType.HAND,
            CorrosionType.FOOT
    };

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

        CorrosionMatrix matrix =
                PossessionHandler.getCorrosionMatrix(player);

        /*
         * 底图：受 GLOBAL 侵蚀影响
         */
        drawCorrosionLayer(
                graphics,
                HUMAN_BODY,
                bodyX,
                bodyY,
                bodyWidth,
                bodyHeight,
                matrix.total(CorrosionType.GLOBAL)
        );

        /*
         * 各器官：受自身侵蚀影响
         */
        for (int i = 0; i < ORGAN_OVERLAYS.length; i++) {

            drawCorrosionLayer(
                    graphics,
                    ORGAN_OVERLAYS[i],
                    bodyX,
                    bodyY,
                    bodyWidth,
                    bodyHeight,
                    matrix.total(ORGAN_TYPES[i])
            );
        }


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

    private void drawCorrosionLayer(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height,
            double corrosion
    ) {

        double ratio = Math.clamp(
                corrosion / 100.0D,
                0.0D,
                1.0D
        );

        /*
         * 侵蚀越高越暗
         */
        float brightness = (float) Math.max(
                0.02D,
                Math.pow(1.0D - ratio, 2.5D)
        );

        graphics.setColor(
                brightness,
                brightness,
                brightness,
                1.0F
        );

        graphics.blit(
                texture,
                x,
                y,
                0,
                0,
                width,
                height,
                width,
                height
        );

        graphics.setColor(
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );
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