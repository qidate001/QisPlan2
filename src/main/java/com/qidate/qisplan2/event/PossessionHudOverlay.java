package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModAttachments;
import com.qidate.qisplan2.ghost.PossessedGhostState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
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

    private static final int BAR_WIDTH = 80;
    private static final int BAR_HEIGHT = 5;

    private static final int RIGHT_MARGIN = 2;
    private static final int BOTTOM_MARGIN = 11;

    /*
     * 两条之间的间距
     */
    private static final int BAR_GAP = 3;

    /*
     * 不同鬼之间的间距
     */
    private static final int GHOST_GAP = 6;


    /*
     * ==============================
     * 浅死机最大显示值
     * ==============================
     */

    private static final double MAX_SHALLOW_STUN = 100.0D;


    /*
     * ==============================
     * 颜色
     * ==============================
     */

    private static final int BAR_BACKGROUND =
            0xA0202027;

    private static final int REVIVAL_COLOR =
            0xFFB44AFF;

    private static final int SHALLOW_STUN_COLOR =
            0xFF6AA6FF;

    private static final int STUN_COLOR =
            0xFFE5484D;

    private static final int PERMANENT_STUN_COLOR =
            0xFFFFC83D;


    @SubscribeEvent
    public static void render(
            RenderGuiEvent.Post event
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null) {

            return;
        }

        if (minecraft.options.hideGui) {
            return;
        }


        Map<
                net.minecraft.resources.ResourceLocation,
                PossessedGhostState
                > ghosts =
                minecraft.player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                );


        if (ghosts.isEmpty()) {
            return;
        }


        GuiGraphics graphics =
                event.getGuiGraphics();


        int screenWidth =
                graphics.guiWidth();

        int screenHeight =
                graphics.guiHeight();


        /*
         * ==============================
         * 从右下角开始向上排列
         * ==============================
         */

        int x =
                screenWidth
                        - BAR_WIDTH
                        - RIGHT_MARGIN;

        int y =
                screenHeight
                        - BOTTOM_MARGIN
                        - BAR_HEIGHT;


        for (PossessedGhostState state :
                ghosts.values()) {

            drawGhostBars(
                    graphics,
                    state,
                    x,
                    y
            );


            /*
             * 当前鬼占两条：
             *
             * 第一条
             * 间隔
             * 第二条
             *
             * 然后鬼之间再留一点距离
             */

            y -=
                    BAR_HEIGHT
                            + BAR_GAP
                            + BAR_HEIGHT
                            + GHOST_GAP;
        }
    }


    /**
     * 绘制一只鬼的两条状态条。
     */
    private static void drawGhostBars(
            GuiGraphics graphics,
            PossessedGhostState state,
            int x,
            int y
    ) {

        /*
         * ==============================
         * 第一条：复苏
         * ==============================
         */

        drawProgressBar(
                graphics,
                x,
                y,
                BAR_WIDTH,
                BAR_HEIGHT,
                state.revival(),
                REVIVAL_COLOR
        );


        /*
         * ==============================
         * 第二条：死机
         * ==============================
         */

        int stunY =
                y
                        + BAR_HEIGHT
                        + BAR_GAP;


        double stunProgress;
        int stunColor;


        if (state.isPermanentlyStunned()) {

            /*
             * 永久死机
             */

            stunProgress = 1.0D;

            stunColor =
                    PERMANENT_STUN_COLOR;

        } else if (state.isStunned()) {

            /*
             * 普通死机
             *
             * 10 秒 = 满条
             */

            double seconds =
                    state.stunTicks()
                            / 20.0D;

            stunProgress =
                    Math.min(
                            1.0D,
                            seconds / 10.0D
                    );

            stunColor =
                    STUN_COLOR;

        } else {

            /*
             * 浅死机
             */

            double shallowStun =
                    state.shallowStun();

            stunProgress =
                    Math.min(
                            1.0D,
                            shallowStun
                                    / MAX_SHALLOW_STUN
                    );

            stunColor =
                    SHALLOW_STUN_COLOR;
        }


        drawProgressBar(
                graphics,
                x,
                stunY,
                BAR_WIDTH,
                BAR_HEIGHT,
                stunProgress,
                stunColor
        );
    }


    /**
     * 绘制进度条。
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
                Mth.clamp(
                        progress,
                        0.0D,
                        1.0D
                );


        /*
         * 背景
         */

        graphics.fill(
                x,
                y,
                x + width,
                y + height,
                BAR_BACKGROUND
        );


        /*
         * 当前值
         */

        int filled =
                (int) Math.round(
                        width * progress
                );


        if (filled <= 0) {
            return;
        }


        graphics.fill(
                x,
                y,
                x + filled,
                y + height,
                fillColor
        );
    }
}