package com.qidate.qisplan2.client.screen.PossessionScreen;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.client.curse.ClientCurseState;
import com.qidate.qisplan2.client.curse.CurseClientRegistry;
import com.qidate.qisplan2.core.ModAttachments;
import com.qidate.qisplan2.core.QisConfig;
import com.qidate.qisplan2.ghost.corrosion.CorrosionMatrix;
import com.qidate.qisplan2.ghost.corrosion.CorrosionType;
import com.qidate.qisplan2.ghost.possession.ability.GhostAbilityRegistry;
import com.qidate.qisplan2.ghost.possession.ability.PossessedGhostAbility;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import com.qidate.qisplan2.ghost.possession.suppression.GhostSuppressionSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

import static com.qidate.qisplan2.client.screen.PossessionScreen.PossessionScreen.DEFAULT_SUPPRESSION_ICON;

/**
 * 驭鬼者状态页渲染器。
 *
 * <p>
 * 负责状态页的全部视觉内容以及状态页滚动条。
 *
 * <p>
 * 不负责 Screen 生命周期、页面切换以及其他页面的交互。
 */
public class PossessionStatusRenderer {


    private static final int CARD_WIDTH = 145;
    private static final int CARD_HEIGHT = 64;

    private static final int CURSE_CARD_WIDTH = 150;
    private static final int CURSE_CARD_HEIGHT = 58;
    private static final int CURSE_CARD_GAP = 6;

    private static final int PANEL_WIDTH = 600;
    private static final int PANEL_HEIGHT = 320;

    /**
     * 诅咒卡片滚动区域的 X 坐标。
     */
    private static final int CURSE_CARD_AREA_X = 14;

    /**
     * 诅咒卡片滚动区域的 Y 坐标。
     *
     * <p>
     * 基础状态信息绘制完成后，
     * 从这里开始显示诅咒卡片。
     */
    private static final int CURSE_CARD_AREA_Y = 124;

    /**
     * 诅咒卡片滚动区域的宽度。
     */
    private static final int CURSE_CARD_AREA_WIDTH =
            CURSE_CARD_WIDTH + 8;

    /**
     * 诅咒卡片滚动区域的高度。
     */
    private static final int CURSE_CARD_AREA_HEIGHT =
            PANEL_HEIGHT
                    - CURSE_CARD_AREA_Y
                    - 12;

    private static final int GHOST_CARD_AREA_X =
            PANEL_WIDTH - CARD_WIDTH - 20;

    private static final int GHOST_CARD_AREA_Y = 36;

    private static final int GHOST_CARD_AREA_WIDTH =
            CARD_WIDTH + 8;

    private static final int GHOST_CARD_AREA_HEIGHT =
            PANEL_HEIGHT - GHOST_CARD_AREA_Y - 12;

    private static final int ICON_SIZE = 18;

    private static final int SHALLOW_STUN_COLOR = 0xFF6AA6FF;
    private static final int STUN_COLOR = 0xFFE5484D;
    private static final int PERMANENT_STUN_COLOR = 0xFFFFC83D;
    private static final double MAX_SHALLOW_STUN = 100.0D;


    /**
     * 驭鬼卡片滚动条。
     *
     * <p>
     * 负责控制右侧驭鬼卡片列表的滚动位置。
     */
    private final PossessionScrollbar ghostScrollbar =
            new PossessionScrollbar(
                    GHOST_CARD_AREA_X
                            + GHOST_CARD_AREA_WIDTH
                            - 3,
                    GHOST_CARD_AREA_Y,
                    GHOST_CARD_AREA_HEIGHT
            );

    /**
     * 诅咒卡片滚动条。
     *
     * <p>
     * 负责控制左侧诅咒卡片列表的滚动位置。
     *
     * <p>
     * 与右侧驭鬼卡片使用独立的滚动位置，
     * 两者互不影响。
     */
    private final PossessionScrollbar curseScrollbar =
            new PossessionScrollbar(
                    CURSE_CARD_AREA_X
                            + CURSE_CARD_AREA_WIDTH
                            - 3,
                    CURSE_CARD_AREA_Y,
                    CURSE_CARD_AREA_HEIGHT
            );

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
    private static final ResourceLocation BONE = body("bone");

    private static final ResourceLocation HUMAN_BODY_WHITE = bodyWhite("human_body");

    private static final ResourceLocation BRAIN_WHITE = bodyWhite("brain");
    private static final ResourceLocation HEART_WHITE = bodyWhite("heart");
    private static final ResourceLocation LUNG_WHITE = bodyWhite("lung");
    private static final ResourceLocation STOMACH_WHITE = bodyWhite("stomach");
    private static final ResourceLocation LIVER_WHITE = bodyWhite("liver");
    private static final ResourceLocation KIDNEY_WHITE = bodyWhite("kidney");
    private static final ResourceLocation PANCREAS_WHITE = bodyWhite("pancreas");
    private static final ResourceLocation GALLBLADDER_WHITE = bodyWhite("gallbladder");
    private static final ResourceLocation SPLEEN_WHITE = bodyWhite("spleen");
    private static final ResourceLocation INTESTINE_WHITE = bodyWhite("intestine");
    private static final ResourceLocation EYE_WHITE = bodyWhite("eye");
    private static final ResourceLocation EAR_WHITE = bodyWhite("ear");
    private static final ResourceLocation NOSE_WHITE = bodyWhite("nose");
    private static final ResourceLocation MOUTH_WHITE = bodyWhite("mouth");
    private static final ResourceLocation HAND_WHITE = bodyWhite("hand");
    private static final ResourceLocation FOOT_WHITE = bodyWhite("foot");
    private static final ResourceLocation BONE_WHITE = bodyWhite("bone");

    private static final ResourceLocation[] ORGAN_OVERLAYS = {
            BRAIN, HEART, LUNG, STOMACH, LIVER, KIDNEY,
            PANCREAS, GALLBLADDER, SPLEEN, INTESTINE,
            EYE, EAR, NOSE, MOUTH, HAND, FOOT, BONE
    };

    private static final ResourceLocation[] ORGAN_OVERLAYS_WHITE = {
            BRAIN_WHITE, HEART_WHITE, LUNG_WHITE, STOMACH_WHITE,
            LIVER_WHITE, KIDNEY_WHITE, PANCREAS_WHITE,
            GALLBLADDER_WHITE, SPLEEN_WHITE, INTESTINE_WHITE,
            EYE_WHITE, EAR_WHITE, NOSE_WHITE, MOUTH_WHITE,
            HAND_WHITE, FOOT_WHITE, BONE_WHITE
    };

    private static ResourceLocation body(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                QisPlan2.MODID,
                "textures/gui/body/" + name + ".png"
        );
    }

    private static ResourceLocation bodyWhite(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                QisPlan2.MODID,
                "textures/gui/body_white/" + name + ".png"
        );
    }

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
            CorrosionType.FOOT,
            CorrosionType.BONE
    };

    private record OrganAnchor(
            CorrosionType type,
            int x1,
            int y1,
            int x2,
            int y2
    ) {}

    private static final OrganAnchor[] ORGAN_ANCHORS = {
            new OrganAnchor(CorrosionType.BRAIN, 60, 19, 118, 19),         // 脑 (正)

            new OrganAnchor(CorrosionType.EYE, 190, 29, 140, 29),          // 眼 (逆)
            new OrganAnchor(CorrosionType.NOSE, 185, 40, 130, 40),         // 鼻 (逆)
            new OrganAnchor(CorrosionType.EAR, 70, 30, 112, 30),           // 耳 (正)
            new OrganAnchor(CorrosionType.MOUTH, 77, 49, 130, 49),         // 嘴 (正)

            new OrganAnchor(CorrosionType.LUNG, 219, 80, 142, 80),         // 肺 (逆)
            new OrganAnchor(CorrosionType.HEART, 219, 100, 133, 100),      // 心 (逆)
            new OrganAnchor(CorrosionType.STOMACH, 219, 128, 150, 128),    // 胃 (逆)
            new OrganAnchor(CorrosionType.SPLEEN, 250, 135, 156, 135),     // 脾 (逆)
            new OrganAnchor(CorrosionType.LIVER, 43, 127, 112, 127),       // 肝 (正)
            new OrganAnchor(CorrosionType.GALLBLADDER, 43, 142, 113, 142), // 胆 (正)
            new OrganAnchor(CorrosionType.KIDNEY, 43, 160, 110, 160),      // 肾 (正)
            new OrganAnchor(CorrosionType.PANCREAS, 219, 153, 140, 153),   // 胰 (逆)
            new OrganAnchor(CorrosionType.INTESTINE, 219, 179, 147, 179),  // 肠 (逆)

            new OrganAnchor(CorrosionType.HAND, 33, 207, 53, 207),         // 手 (正)
            new OrganAnchor(CorrosionType.BONE, 205, 278, 151, 278),       // 骨 (逆)
            new OrganAnchor(CorrosionType.FOOT, 193, 380, 155, 380),       // 足 (逆)
    };

    private Font font;

    public void render(
            GuiGraphics graphics,
            int panelX,
            int panelY,
            float panelScale
    ) {
        this.font =
                Minecraft.getInstance().font;

        drawStatusPage(
                graphics,
                panelX,
                panelY,
                panelScale
        );
    }

    private void drawStatusPage(
            GuiGraphics graphics,
            int panelX,
            int panelY,
            float panelScale
    ) {
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

        int leftX = 14;
        int topY = 52;

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

        topY += 18;

        drawCurseCards(
                graphics,
                CURSE_CARD_AREA_X,
                CURSE_CARD_AREA_Y,
                panelX,
                panelY,
                panelScale
        );


        // =========================
        // 中央：人体图
        // =========================

        int bodyWidth = 168;
        int bodyHeight = 278;

        int bodyX = (PANEL_WIDTH - bodyWidth) / 2;
        int bodyY = 26;

        CorrosionMatrix matrix =
                PossessionHandler.getCorrosionMatrix(player);

        boolean whiteMode =
                QisConfig.CLIENT.WHITE_BODY_TEXTURE.get();

        ResourceLocation bodyTexture =
                whiteMode
                        ? HUMAN_BODY_WHITE
                        : HUMAN_BODY;

        ResourceLocation[] organTextures =
                whiteMode
                        ? ORGAN_OVERLAYS_WHITE
                        : ORGAN_OVERLAYS;

        /*
         * 底图：受 GLOBAL 侵蚀影响
         */
        drawCorrosionLayer(
                graphics,
                bodyTexture,
                bodyX,
                bodyY,
                bodyWidth,
                bodyHeight,
                matrix.total(CorrosionType.GLOBAL)
        );

        /*
         * 各器官：受自身侵蚀影响
         */
        for (int i = 0; i < organTextures.length; i++) {

            drawCorrosionLayer(
                    graphics,
                    organTextures[i],
                    bodyX,
                    bodyY,
                    bodyWidth,
                    bodyHeight,
                    matrix.total(ORGAN_TYPES[i])
            );
        }

        /*
         * =========================
         * 器官引导线 + 侵蚀值
         * =========================
         */

        for (OrganAnchor anchor : ORGAN_ANCHORS) {

            int startX =
                    scaleBodyX(
                            anchor.x1(),
                            bodyX,
                            bodyWidth
                    );

            int startY =
                    scaleBodyY(
                            anchor.y1(),
                            bodyY,
                            bodyHeight
                    );

            int endX =
                    scaleBodyX(
                            anchor.x2(),
                            bodyX,
                            bodyWidth
                    );

            int endY =
                    scaleBodyY(
                            anchor.y2(),
                            bodyY,
                            bodyHeight
                    );

            int value =
                    matrix.total(anchor.type());

            drawLine(
                    graphics,
                    startX,
                    startY,
                    endX,
                    endY,
                    String.valueOf(value)
            );
        }


        // =========================
        // 右侧：驾驭的鬼
        // =========================

        int rightX = PANEL_WIDTH - CARD_WIDTH - 12;

        int cardStep =
                CARD_HEIGHT + 8;

        int contentHeight =
                ghosts.size() * cardStep - 8;

        // =========================
        // 裁剪鬼卡片区域
        // =========================

        graphics.enableScissor(
                panelX + (int) (GHOST_CARD_AREA_X * panelScale),
                panelY + (int) (GHOST_CARD_AREA_Y * panelScale),
                panelX + (int) ((GHOST_CARD_AREA_X + GHOST_CARD_AREA_WIDTH) * panelScale),
                panelY + (int) ((GHOST_CARD_AREA_Y + GHOST_CARD_AREA_HEIGHT) * panelScale)
        );

        /*
         * 根据滚动条当前位置，
         * 计算第一张鬼卡片的 Y 坐标。
         */
        int rightY =
                GHOST_CARD_AREA_Y
                        - (int) ghostScrollbar.getScroll();

        for (var entry : ghosts.entrySet()) {

            drawGhostCard(
                    graphics,
                    entry.getKey(),
                    entry.getValue(),
                    rightX,
                    rightY
            );

            rightY += cardStep;
        }

        graphics.disableScissor();

        /*
         * 绘制驭鬼卡片滚动条。
         *
         * <p>
         * 当内容没有超出可视区域时，
         * 滚动条会自动隐藏。
         */
        ghostScrollbar.render(
                graphics,
                contentHeight
        );
    }

    private void drawGhostCard(
            GuiGraphics graphics,
            ResourceLocation ghostId,
            PossessedGhostState state,
            int x,
            int y
    ) {

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        // =========================
        // 背景
        // =========================

        graphics.fill(
                x,
                y,
                x + CARD_WIDTH,
                y + CARD_HEIGHT,
                0xD0181820
        );

        // 顶部高光
        graphics.fill(
                x + 6,
                y + 1,
                x + CARD_WIDTH - 6,
                y + 2,
                0x40FFFFFF
        );

        // =========================
        // 图标
        // =========================

        graphics.fill(
                x + 5,
                y + 5,
                x + 5 + ICON_SIZE,
                y + 5 + ICON_SIZE,
                0xFF555565
        );

        // =========================
        // 名称
        // =========================

        graphics.drawString(
                this.font,
                getGhostName(ghostId),
                x + 28,
                y + 5,
                0xFFFFFFFF
        );

        // =========================================================
        // 复苏
        // =========================================================

        graphics.drawString(
                this.font,
                "复苏",
                x + 28,
                y + 20,
                0xFFCCCCCC
        );

        drawProgressBar(
                graphics,
                x + 52,
                y + 22,
                58,
                3,
                state.revival(),
                0xFFB44AFF
        );

        graphics.drawString(
                this.font,
                String.format(
                        "%.0f%%",
                        state.revival() * 100
                ),
                x + 114,
                y + 17,
                0xFFFFFFFF
        );

        // =========================================================
        // 死机
        // =========================================================

        double progress;
        int color;
        String value;

        if (state.isPermanentlyStunned()) {

            progress = 1.0D;
            color = PERMANENT_STUN_COLOR;
            value = "∞";

        } else if (state.isStunned()) {

            double sec =
                    state.stunTicks() / 20.0D;

            progress =
                    Math.min(
                            1.0D,
                            sec / 10.0D
                    );

            color = STUN_COLOR;

            value =
                    String.format(
                            "%.1fs",
                            sec
                    );

        } else {

            progress =
                    Math.min(
                            1.0D,
                            state.shallowStun()
                                    / MAX_SHALLOW_STUN
                    );

            color = SHALLOW_STUN_COLOR;

            value =
                    String.format(
                            "%.0f",
                            state.shallowStun()
                    );
        }

        graphics.drawString(
                this.font,
                "死机",
                x + 28,
                y + 35,
                0xFFCCCCCC
        );

        drawProgressBar(
                graphics,
                x + 52,
                y + 37,
                58,
                3,
                progress,
                color
        );

        graphics.drawString(
                this.font,
                value,
                x + 114,
                y + 32,
                0xFFFFFFFF
        );

        // =========================================================
        // 当前压制
        // =========================================================

        double suppression =
                GhostSuppressionSystem.getSuppression(
                        minecraft.player,
                        ghostId
                );

        graphics.drawString(
                this.font,
                "压制",
                x + 28,
                y + 50,
                0xFFCCCCCC
        );

        // 压制进度条
        drawProgressBar(
                graphics,
                x + 52,
                y + 52,
                58,
                3,
                suppression,
                0xFF8E6BFF
        );

        graphics.drawString(
                this.font,
                String.format(
                        "%.1f%%",
                        suppression * 100.0D
                ),
                x + 114,
                y + 47,
                0xFFFFFFFF
        );
    }

    private void drawProgressBar(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            double progress,
            int fillColor
    ) {

        progress = Math.clamp(progress, 0.0D, 1.0D);

        graphics.fill(
                x,
                y,
                x + width,
                y + height,
                0xFF202027
        );

        int filled = (int)Math.round(width * progress);

        if (filled > 0) {

            graphics.fill(
                    x,
                    y,
                    x + filled,
                    y + height,
                    fillColor
            );
        }
    }

    /**
     * 绘制状态页中的诅咒卡片列表。
     *
     * <p>
     * 诅咒卡片拥有独立的滚动区域，
     * 不会影响右侧驭鬼卡片的滚动位置。
     */
    private void drawCurseCards(
            GuiGraphics graphics,
            int x,
            int y,
            int panelX,
            int panelY,
            float panelScale
    ) {

        int contentHeight =
                getCurseCardContentHeight();

        /*
         * 防止诅咒数量减少后，
         * 滚动位置停留在已经不存在的位置。
         */
        curseScrollbar.setScroll(
                curseScrollbar.getScroll(),
                contentHeight
        );

        /*
         * =========================
         * 裁剪诅咒卡片区域
         * =========================
         */
        graphics.enableScissor(
                panelX + (int) (
                        CURSE_CARD_AREA_X
                                * panelScale
                ),
                panelY + (int) (
                        CURSE_CARD_AREA_Y
                                * panelScale
                ),
                panelX + (int) (
                        (
                                CURSE_CARD_AREA_X
                                        + CURSE_CARD_AREA_WIDTH
                        )
                                * panelScale
                ),
                panelY + (int) (
                        (
                                CURSE_CARD_AREA_Y
                                        + CURSE_CARD_AREA_HEIGHT
                        )
                                * panelScale
                )
        );

        /*
         * 根据当前滚动位置，
         * 计算第一张卡片的 Y 坐标。
         */
        int cardY =
                CURSE_CARD_AREA_Y
                        - (int) curseScrollbar.getScroll();

        for (
                ClientCurseState.CurseData curse
                : ClientCurseState.getAll()
        ) {

            CurseClientRegistry.CurseClientType clientType =
                    CurseClientRegistry.get(
                            curse.type()
                    );

            /*
             * 已注册但禁止检测的诅咒不显示。
             */
            if (
                    clientType != null
                            && !clientType.canDetect()
            ) {
                continue;
            }

            ResourceLocation icon =
                    clientType != null
                            && clientType.icon() != null
                            ? clientType.icon()
                            : DEFAULT_SUPPRESSION_ICON;

            Component name =
                    Component.translatable(
                            "curse."
                                    + curse.type().getNamespace()
                                    + "."
                                    + curse.type().getPath()
                    );

            graphics.fill(
                    x,
                    cardY,
                    x + CURSE_CARD_WIDTH,
                    cardY + CURSE_CARD_HEIGHT,
                    0xD0181820
            );

            graphics.fill(
                    x + 6,
                    cardY + 1,
                    x + CURSE_CARD_WIDTH - 6,
                    cardY + 2,
                    0x40FFFFFF
            );

            graphics.blit(
                    icon,
                    x + 6,
                    cardY + 6,
                    0,
                    0,
                    ICON_SIZE,
                    ICON_SIZE,
                    ICON_SIZE,
                    ICON_SIZE
            );

            graphics.drawString(
                    this.font,
                    name,
                    x + 30,
                    cardY + 6,
                    0xFFFFFFFF
            );

            graphics.drawString(
                    this.font,
                    "强度",
                    x + 30,
                    cardY + 24,
                    0xFFAAAAAA
            );

            graphics.drawString(
                    this.font,
                    String.valueOf(
                            curse.strength()
                    ),
                    x + 90,
                    cardY + 24,
                    0xFFFFFFFF
            );

            graphics.drawString(
                    this.font,
                    "持续时间",
                    x + 30,
                    cardY + 39,
                    0xFFAAAAAA
            );

            graphics.drawString(
                    this.font,
                    formatCurseDuration(
                            curse.remainingTicks()
                    ),
                    x + 90,
                    cardY + 39,
                    0xFFFFFFFF
            );

            cardY +=
                    CURSE_CARD_HEIGHT
                            + CURSE_CARD_GAP;
        }

        graphics.disableScissor();

        /*
         * =========================
         * 绘制诅咒滚动条
         * =========================
         */
        curseScrollbar.render(
                graphics,
                contentHeight
        );
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

    private void drawLine(
            GuiGraphics graphics,
            int x1,
            int y1,
            int x2,
            int y2,
            String text
    ) {
        /*
         * 画横线
         */
        graphics.fill(
                Math.min(x1, x2),
                y1,
                Math.max(x1, x2),
                y1 + 1,
                0xFFFFFFFF
        );

        /*
         * 判断方向
         *
         * x1 < x2：
         * 数字在左，器官在右
         *
         * x1 > x2：
         * 数字在右，器官在左
         */
        int textWidth = this.font.width(text);

        int textX;

        if (x1 < x2) {
            // 数字在左侧
            textX = x1 - textWidth - 4;
        } else {
            // 数字在右侧
            textX = x1 + 4;
        }

        /*
         * 让文字垂直居中在线上
         */
        int textY =
                y1 - this.font.lineHeight / 2;

        graphics.drawString(
                this.font,
                text,
                textX,
                textY,
                0xFFFFFFFF
        );
    }

    private int scaleBodyX(
            int originalX,
            int bodyX,
            int bodyWidth
    ) {
        return bodyX + originalX * bodyWidth / 275;
    }

    private int scaleBodyY(
            int originalY,
            int bodyY,
            int bodyHeight
    ) {
        return bodyY + originalY * bodyHeight / 413;
    }



    public boolean mouseClicked(
            double mouseX,
            double mouseY
    ) {

        int curseContentHeight =
                getCurseCardContentHeight();

        if (
                curseScrollbar.mouseClicked(
                        mouseX,
                        mouseY,
                        curseContentHeight
                )
        ) {
            return true;
        }

        int ghostContentHeight =
                getGhostCardContentHeight();

        if (
                ghostScrollbar.mouseClicked(
                        mouseX,
                        mouseY,
                        ghostContentHeight
                )
        ) {
            return true;
        }

        return false;
    }

    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollY
    ) {

        boolean insideCurseArea =
                mouseX >= CURSE_CARD_AREA_X
                        && mouseX <=
                        CURSE_CARD_AREA_X
                                + CURSE_CARD_AREA_WIDTH
                        && mouseY >= CURSE_CARD_AREA_Y
                        && mouseY <=
                        CURSE_CARD_AREA_Y
                                + CURSE_CARD_AREA_HEIGHT;

        if (insideCurseArea) {

            int contentHeight =
                    getCurseCardContentHeight();

            if (
                    curseScrollbar.scroll(
                            scrollY,
                            contentHeight
                    )
            ) {
                return true;
            }
        }

        boolean insideGhostArea =
                mouseX >= GHOST_CARD_AREA_X
                        && mouseX <=
                        GHOST_CARD_AREA_X
                                + GHOST_CARD_AREA_WIDTH
                        && mouseY >= GHOST_CARD_AREA_Y
                        && mouseY <=
                        GHOST_CARD_AREA_Y
                                + GHOST_CARD_AREA_HEIGHT;

        if (insideGhostArea) {

            int contentHeight =
                    getGhostCardContentHeight();

            if (
                    ghostScrollbar.scroll(
                            scrollY,
                            contentHeight
                    )
            ) {
                return true;
            }
        }

        return false;
    }

    public boolean mouseDragged(
            double mouseX,
            double mouseY
    ) {

        int curseContentHeight =
                getCurseCardContentHeight();

        if (
                curseScrollbar.mouseDragged(
                        mouseX,
                        mouseY,
                        curseContentHeight
                )
        ) {
            return true;
        }

        int ghostContentHeight =
                getGhostCardContentHeight();

        if (
                ghostScrollbar.mouseDragged(
                        mouseX,
                        mouseY,
                        ghostContentHeight
                )
        ) {
            return true;
        }

        return false;
    }

    public boolean mouseReleased() {

        if (
                curseScrollbar.mouseReleased()
        ) {
            return true;
        }

        if (
                ghostScrollbar.mouseReleased()
        ) {
            return true;
        }

        return false;
    }

    private int getGhostCardContentHeight() {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return 0;
        }

        int cardStep =
                CARD_HEIGHT + 8;

        return Math.max(
                0,
                minecraft.player
                        .getData(
                                ModAttachments.POSSESSED_GHOSTS
                        )
                        .size()
                        * cardStep
                        - 8
        );
    }

    private String formatCurseDuration(
            int ticks
    ) {

        if (ticks < 0) {
            return "∞";
        }

        int totalSeconds =
                ticks / 20;

        int minutes =
                totalSeconds / 60;

        int seconds =
                totalSeconds % 60;

        return String.format(
                "%02d:%02d",
                minutes,
                seconds
        );
    }

    /**
     * 获取诅咒卡片列表的完整内容高度。
     *
     * <p>
     * 这个高度包含所有实际可显示的诅咒卡片，
     * 用于计算最大滚动距离。
     */
    private int getCurseCardContentHeight() {

        int count =
                getVisibleCurseCount();

        if (count <= 0) {
            return 0;
        }

        int cardStep =
                CURSE_CARD_HEIGHT
                        + CURSE_CARD_GAP;

        return count * cardStep
                - CURSE_CARD_GAP;
    }

    /**
     * 获取当前状态页实际可显示的诅咒数量。
     *
     * <p>
     * 未注册的诅咒默认显示。
     *
     * <p>
     * 已注册但禁止检测的诅咒不会显示。
     */
    private int getVisibleCurseCount() {

        int count = 0;

        for (
                ClientCurseState.CurseData curse
                : ClientCurseState.getAll()
        ) {

            CurseClientRegistry.CurseClientType clientType =
                    CurseClientRegistry.get(
                            curse.type()
                    );

            /*
             * 已注册且禁止检测的诅咒不显示。
             */
            if (
                    clientType != null
                            && !clientType.canDetect()
            ) {
                continue;
            }

            count++;
        }

        return count;
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
}