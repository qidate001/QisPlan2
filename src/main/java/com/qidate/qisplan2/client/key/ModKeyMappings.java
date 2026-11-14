package com.qidate.qisplan2.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import java.util.Map;
import com.qidate.qisplan2.ghost.module.ability.GhostDomainShortcut;

/** 驭鬼界面与统一技能轮盘；具体厉鬼不再注册专用释放按键。 */
public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories.qisplan2";

    public static final KeyMapping OPEN_POSSESSION_SCREEN = new KeyMapping("key.qisplan2.possession_screen",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);

    public static final KeyMapping ABILITY_WHEEL = new KeyMapping("key.qisplan2.ability_wheel",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    /** 通用操作使用旧版默认键；设置菜单可独立重绑，不绑定任何具体厉鬼。 */
    public static final Map<GhostDomainShortcut, KeyMapping> DOMAIN_SHORTCUTS = Map.of(
            GhostDomainShortcut.TELEPORT, domainKey("teleport", GLFW.GLFW_KEY_R),
            GhostDomainShortcut.RAISE_TARGET, domainKey("raise_layer", GLFW.GLFW_KEY_PAGE_UP),
            GhostDomainShortcut.LOWER_TARGET, domainKey("lower_layer", GLFW.GLFW_KEY_PAGE_DOWN),
            GhostDomainShortcut.SELF_UP, domainKey("self_up", GLFW.GLFW_KEY_UP),
            GhostDomainShortcut.SELF_DOWN, domainKey("self_down", GLFW.GLFW_KEY_DOWN),
            GhostDomainShortcut.DOMAIN_UP, domainKey("layer_up", GLFW.GLFW_KEY_KP_ADD),
            GhostDomainShortcut.DOMAIN_DOWN, domainKey("layer_down", GLFW.GLFW_KEY_KP_SUBTRACT)
    );

    /** 构造通用鬼域按键，显示名由语言资源提供。 */
    private static KeyMapping domainKey(String name, int key) {
        return new KeyMapping("key.qisplan2.ghost_domain_" + name, InputConstants.Type.KEYSYM, key, CATEGORY);
    }

    /** 按键定义类不需要实例化。 */
    private ModKeyMappings() {}
}
