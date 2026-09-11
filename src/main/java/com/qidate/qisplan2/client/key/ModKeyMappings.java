package com.qidate.qisplan2.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {

    public static final String CATEGORY =
            "key.categories.qisplan2";

    /*
     * ========================================================
     * 驾驭界面
     * ========================================================
     */

    public static final KeyMapping OPEN_POSSESSION_SCREEN =
            new KeyMapping(
                    "key.qisplan2.possession_screen",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_H,
                    CATEGORY
            );

    /*
     * ========================================================
     * 鬼签
     * ========================================================
     */

    public static final KeyMapping GHOST_DIVINATION_LIFE =
            new KeyMapping(
                    "key.qisplan2.ghost_divination_life",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_KP_1,
                    CATEGORY
            );

    public static final KeyMapping GHOST_DIVINATION_DEATH =
            new KeyMapping(
                    "key.qisplan2.ghost_divination_death",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_KP_2,
                    CATEGORY
            );

    public static final KeyMapping GHOST_DIVINATION_GHOST =
            new KeyMapping(
                    "key.qisplan2.ghost_divination_ghost",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_KP_3,
                    CATEGORY
            );

    private ModKeyMappings() {}
}