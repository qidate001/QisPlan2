package com.qidate.qisplan2.client.curse;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * 诅咒 HUD。
 *
 * <p>
 * 负责显示当前玩家能够察觉的诅咒。
 * 这里只处理客户端显示，不参与诅咒实际逻辑。
 * </p>
 */
@EventBusSubscriber(modid = QisPlan2.MODID)
public final class CurseHudOverlay {

    private CurseHudOverlay() {
    }

    private static final int ICON_SIZE = 16;

    private static final int LEFT = 8;

    private static final int BOTTOM = 8;

    private static final int SPACING = 4;

    /**
     * 未注册专属图标时使用的默认诅咒图标。
     */
    private static final ResourceLocation DEFAULT_SUPPRESSION_ICON =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "textures/gui/suppression.png"
            );

    @SubscribeEvent
    public static void onRenderGui(
            RenderGuiEvent.Post event
    ) {

        if (ClientCurseState.isEmpty()) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        GuiGraphics guiGraphics =
                event.getGuiGraphics();

        Font font =
                minecraft.font;

        int y =
                guiGraphics.guiHeight()
                        - BOTTOM
                        - ICON_SIZE;

        for (ClientCurseState.CurseData curse :
                ClientCurseState.getAll()
        ) {

            /*
             * ========================================================
             * 获取客户端显示定义
             * ========================================================
             */

            CurseClientRegistry.CurseClientType clientType =
                    CurseClientRegistry.get(
                            curse.type()
                    );

            /*
             * ========================================================
             * 判断玩家是否能够察觉该诅咒
             * ========================================================
             *
             * 没有注册客户端定义时，
             * 默认允许显示，避免漏注册导致诅咒完全消失。
             */

            if (clientType != null
                    && !clientType.canDetect()) {

                continue;
            }

            /*
             * ========================================================
             * 获取图标
             * ========================================================
             */

            ResourceLocation icon =
                    DEFAULT_SUPPRESSION_ICON;

            if (clientType != null
                    && clientType.icon() != null) {

                icon =
                        clientType.icon();
            }

            /*
             * ========================================================
             * 绘制图标
             * ========================================================
             */

            guiGraphics.blit(
                    icon,
                    LEFT,
                    y,
                    0,
                    0,
                    ICON_SIZE,
                    ICON_SIZE,
                    ICON_SIZE,
                    ICON_SIZE
            );

            /*
             * ========================================================
             * 绘制名称
             * ========================================================
             */

            Component name =
                    Component.translatable(
                            "curse."
                                    + curse.type().getNamespace()
                                    + "."
                                    + curse.type().getPath()
                    );

            guiGraphics.drawString(
                    font,
                    name,
                    LEFT + ICON_SIZE + 4,
                    y + 4,
                    0xFFFFFFFF,
                    true
            );

            /*
             * ========================================================
             * 下一个诅咒
             * ========================================================
             */

            y -=
                    ICON_SIZE + SPACING;
        }
    }
}