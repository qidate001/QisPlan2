package com.qidate.qisplan2.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** 驭鬼界面与统一技能轮盘；具体厉鬼不再注册专用释放按键。 */
public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories.qisplan2";
    public static final KeyMapping OPEN_POSSESSION_SCREEN = new KeyMapping("key.qisplan2.possession_screen",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);
    public static final KeyMapping ABILITY_WHEEL = new KeyMapping("key.qisplan2.ability_wheel",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    /** 按键定义类不需要实例化。 */
    private ModKeyMappings() {}
}
