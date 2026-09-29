package com.qidate.qisplan2.client.curse;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * 诅咒 HUD。
 *
 * <p>
 * 第一版只负责显示：
 * 图标 + 诅咒名称。
 * </p>
 */
@EventBusSubscriber(
        modid = QisPlan2.MODID
)
public final class CurseHudOverlay {

    private CurseHudOverlay() {
    }

    /**
     * 图标大小。
     */
    private static final int ICON_SIZE = 16;

    /**
     * HUD 左下角基础位置。
     */
    private static final int LEFT = 8;
    private static final int BOTTOM = 8;

    /**
     * 每个诅咒之间的垂直间距。
     */
    private static final int SPACING = 4;

    @SubscribeEvent
    public static void onRenderGui(
            RenderGuiEvent.Post event
    ) {

        /*
         * 当前没有诅咒，
         * 就不需要绘制任何东西。
         */
        if (ClientCurseState.isEmpty()) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        /*
         * 没有玩家时不绘制。
         */
        if (minecraft.player == null) {
            return;
        }

        GuiGraphics guiGraphics =
                event.getGuiGraphics();

        Font font =
                minecraft.font;

        /*
         * 从屏幕底部向上排列。
         */
        int y =
                guiGraphics.guiHeight()
                        - BOTTOM
                        - ICON_SIZE;

        for (ClientCurseState.CurseData curse :
                ClientCurseState.getAll()
        ) {

            /*
             * 临时使用红色方块作为诅咒图标。
             *
             * 后续再替换成真正的诅咒图标。
             */
            guiGraphics.fill(
                    LEFT,
                    y,
                    LEFT + ICON_SIZE,
                    y + ICON_SIZE,
                    0xFFFF0000
            );

            /*
             * 第一版暂时直接显示诅咒类型名称。
             *
             * 例如：
             * ghost_divination
             * ghost_tombstone
             *
             * 后续再接入语言文件。
             */
            String name =
                    curse.type().getPath();

            /*
             * 在图标右侧显示诅咒名称。
             */
            guiGraphics.drawString(
                    font,
                    name,
                    LEFT + ICON_SIZE + 4,
                    y + 4,
                    0xFFFFFFFF,
                    true
            );

            /*
             * 下一个诅咒向上排列。
             */
            y -=
                    ICON_SIZE + SPACING;
        }
    }
}