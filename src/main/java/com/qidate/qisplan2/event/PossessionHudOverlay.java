package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.PossessedGhostState;
import com.qidate.qisplan2.ghost.PossessionHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.Map;

public class PossessionHudOverlay {

    public static void render(RenderGuiEvent.Post event) {

        Minecraft minecraft = Minecraft.getInstance();

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

        int x =
                graphics.guiWidth() - 8;

        int y =
                graphics.guiHeight() - 28;

        /*
         * 从下往上显示。
         */
        for (var entry : ghosts.entrySet()) {

            ResourceLocation ghost =
                    entry.getKey();

            PossessedGhostState state =
                    entry.getValue();

            /*
             * 当前只给夜游鬼显示中文名称。
             */
            String name =
                    getGhostName(ghost);

            String revivalText =
                    String.format(
                            "%.1f%%",
                            state.revival() * 100.0D
                    );

            String shallowText =
                    String.format(
                            "%.1f",
                            state.shallowStun()
                    );

            String text =
                    name
                            + " 复苏 "
                            + revivalText
                            + "  浅死机 "
                            + shallowText;

            int width =
                    font.width(text);

            graphics.drawString(
                    font,
                    text,
                    x - width,
                    y,
                    0xFFFFFFFF,
                    true
            );

            y -= 12;
        }
    }

    private static String getGhostName(
            ResourceLocation id
    ) {

        if (id.equals(
                PossessionHandler.NIGHT_WANDERER
        )) {
            return "夜游鬼";
        }

        return id.toString();
    }
}