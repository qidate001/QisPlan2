package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.PossessedGhostState;
import com.qidate.qisplan2.ghost.PossessionHandler;
import com.qidate.qisplan2.ghost.ability.GhostAbilityRegistry;
import com.qidate.qisplan2.ghost.ability.PossessedGhostAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.Map;

@EventBusSubscriber(
        modid = QisPlan2.MODID
)
public class PossessionHudOverlay {

    /*
     * ==============================
     * HUD 尺寸
     * ==============================
     */

    private static final int PANEL_WIDTH = 133;
    private static final int PANEL_HEIGHT = 41;

    private static final int RIGHT_MARGIN = 6;
    private static final int BOTTOM_MARGIN = 6;
    private static final int PANEL_GAP = 4;

    private static final int ICON_SIZE = 23;

    private static final int SHALLOW_STUN_COLOR = (int) 0xFF6AA6FF;
    private static final int STUN_COLOR = (int) 0xFFE5484D;
    private static final int PERMANENT_STUN_COLOR = (int) 0xFFFFC83D;



    /*
     * ==============================
     * 复苏 / 死机最大显示值
     * ==============================
     */

    private static final double MAX_SHALLOW_STUN = 100.0D;


    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null) {
            return;
        }

        Map<ResourceLocation, PossessedGhostState> ghosts =
                minecraft.player.getData(
                        QisPlan2.POSSESSED_GHOSTS
                );

        if (ghosts.isEmpty()) {
            return;
        }

        GuiGraphics graphics =
                event.getGuiGraphics();

        Font font =
                minecraft.font;

        int screenWidth =
                graphics.guiWidth();

        int screenHeight =
                graphics.guiHeight();

        /*
         * 从右下角开始向上排列。
         */
        int x =
                screenWidth
                        - PANEL_WIDTH
                        - RIGHT_MARGIN;

        int y =
                screenHeight
                        - BOTTOM_MARGIN
                        - PANEL_HEIGHT;


        for (var entry : ghosts.entrySet()) {

            ResourceLocation ghostId =
                    entry.getKey();

            PossessedGhostState state =
                    entry.getValue();

            drawGhostPanel(
                    graphics,
                    font,
                    ghostId,
                    state,
                    x,
                    y
            );

            y -= PANEL_HEIGHT + PANEL_GAP;
        }
    }


    /**
     * 绘制单只鬼的卡片。
     */
    private static void drawGhostPanel(
            GuiGraphics graphics,
            Font font,
            ResourceLocation ghostId,
            PossessedGhostState state,
            int x,
            int y
    ) {
        /*
         * 卡片背景
         */
        drawRoundedPanel(
                graphics,
                x,
                y,
                PANEL_WIDTH,
                PANEL_HEIGHT
        );

        /*
         * 鬼头像
         */
        drawGhostIcon(
                graphics,
                ghostId,
                x + 5,
                y + 5
        );

        /*
         * 名称
         */
        String name = getGhostName(ghostId);

        graphics.drawString(
                font,
                name,
                x + 34,
                y + 4,
                0xFFFFFFFF,
                true
        );

        /*
         * 复苏值
         */
        double revival = state.revival();

        graphics.drawString(
                font,
                "复苏",
                x + 34,
                y + 17,
                0xFFFFFFFF,
                true
        );

        drawProgressBar(
                graphics,
                x + 53,
                y + 18,
                43,
                4,
                revival,
                (int) 0xFFB44AFF
        );

        graphics.drawString(
                font,
                String.format(
                        "%.0f%%",
                        revival * 100.0D
                ),
                x + 99,
                y + 15,
                0xFFFFFFFF,
                true
        );

        /*
         * ========================================
         * 第二状态条
         *
         * 正常：
         *     浅死机值 → 蓝色
         *
         * 普通死机：
         *     剩余死机时间 → 红色
         *
         * 永久死机：
         *     ∞ → 金色
         * ========================================
         */

        String stunValueText;
        double stunProgress;
        int stunColor;

        if (state.isPermanentlyStunned()) {

            /*
             * 永久死机
             */
            stunValueText = "∞";
            stunProgress = 1.0D;
            stunColor = (int) 0xFFFFC83D;

        } else if (state.isStunned()) {

            /*
             * 普通死机
             */
            double seconds =
                    state.stunTicks() / 20.0D;

            stunValueText =
                    String.format(
                            "%.1fs",
                            seconds
                    );

            /*
             * 10 秒 = 满条
             */
            stunProgress =
                    Math.min(
                            1.0D,
                            seconds / 10.0D
                    );

            stunColor = (int) 0xFFE5484D;

        } else {

            /*
             * 正常状态：
             * 显示浅死机值
             */
            double shallowStun =
                    state.shallowStun();

            stunValueText =
                    String.format(
                            "%.0f",
                            shallowStun
                    );

            stunProgress =
                    Math.min(
                            1.0D,
                            shallowStun / MAX_SHALLOW_STUN
                    );

            stunColor = (int) 0xFF6AA6FF;
        }

        graphics.drawString(
                font,
                "死机",
                x + 34,
                y + 30,
                0xFFFFFFFF,
                true
        );

        /*
         * x + 53
         * 宽度 43
         * 高度 4
         */
        drawProgressBar(
                graphics,
                x + 53,
                y + 31,
                43,
                4,
                stunProgress,
                stunColor
        );

        graphics.drawString(
                font,
                stunValueText,
                x + 99,
                y + 28,
                0xFFFFFFFF,
                true
        );
    }


    /**
     * 绘制一个简单圆角卡片。
     */
    private static void drawRoundedPanel(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height
    ) {

        /*
         * 外框
         */
        graphics.fill(
                x + 4,
                y,
                x + width - 4,
                y + height,
                0xE0101018
        );

        graphics.fill(
                x,
                y + 4,
                x + width,
                y + height - 4,
                0xE0101018
        );

        /*
         * 顶部高光线
         */
        graphics.fill(
                x + 8,
                y + 1,
                x + width - 8,
                y + 2,
                0x60FFFFFF
        );
    }


    /**
     * 绘制鬼头像。
     *
     * 第一版先画一个圆形占位图标。
     */
    private static void drawGhostIcon(
            GuiGraphics graphics,
            ResourceLocation ghostId,
            int x,
            int y
    ) {
        graphics.fill(
                x,
                y,
                x + 23,
                y + 23,
                0xFF303040
        );

        graphics.fill(
                x + 2,
                y + 2,
                x + 21,
                y + 21,
                0xFF555565
        );

        if (ghostId.equals(
                PossessionHandler.NIGHT_WANDERER
        )) {
            graphics.fill(
                    x + 7,
                    y + 7,
                    x + 10,
                    y + 10,
                    0xFFFFFFFF
            );

            graphics.fill(
                    x + 14,
                    y + 7,
                    x + 17,
                    y + 10,
                    0xFFFFFFFF
            );
        }
    }


    /**
     * 进度条。
     */
    private static void drawProgressBar(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            double progress,
            int fillColor
    ) {

        progress =
                Math.max(
                        0.0D,
                        Math.min(
                                1.0D,
                                progress
                        )
                );

        /*
         * 背景
         */
        graphics.fill(
                x,
                y,
                x + width,
                y + height,
                0xFF202027
        );

        /*
         * 当前值
         */
        int filled =
                (int) Math.round(
                        width * progress
                );

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
     * 获取显示名称。
     */
    private static String getGhostName(
            ResourceLocation id
    ) {

        PossessedGhostAbility ability =
                GhostAbilityRegistry.get(id);

        if (ability != null) {
            return ability.displayName()
                    .getString();
        }

        /*
         * 没有对应能力时，
         * 才退回显示 ID。
         *
         * 主要用于防止数据中存在已经不存在的旧鬼。
         */
        return id.toString();
    }
}