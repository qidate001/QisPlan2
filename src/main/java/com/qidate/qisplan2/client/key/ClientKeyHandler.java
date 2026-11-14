package com.qidate.qisplan2.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import com.qidate.qisplan2.client.screen.PossessionScreen.PossessionScreen;
import com.qidate.qisplan2.client.screen.AbilityWheelScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/** 按住打开、松开提交；一次按住最多打开一次，鼠标确认后不会立刻重开。 */
public final class ClientKeyHandler {
    private static boolean latched;
    /** 输入管理器无需实例化。 */
    private ClientKeyHandler() {}
    /** 只注册界面与轮盘两个通用按键。 */
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.OPEN_POSSESSION_SCREEN);
        event.register(ModKeyMappings.ABILITY_WHEEL);
    }
    /** 读取实际按键状态，避免打开 Screen 后 KeyMapping 被原版清零造成误释放。 */
    public static boolean wheelHeld() {
        var key = ModKeyMappings.ABILITY_WHEEL.getKey();
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (key.getValue() < 0) return false;
        if (key.getType() == InputConstants.Type.MOUSE)
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        return InputConstants.isKeyDown(window, key.getValue());
    }
    /** 只在游戏中允许打开；失焦、死亡和其他界面都不会产生技能请求。 */
    public static void clientTick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        boolean held = wheelHeld();
        if (!held) latched = false;
        if (mc.screen instanceof AbilityWheelScreen wheel) {
            if (!mc.isWindowActive() || mc.player == null || !mc.player.isAlive()) wheel.cancel();
            else if (!held) wheel.confirm();
        } else if (held && !latched) {
            latched = true;
            if (mc.screen == null && mc.player != null && mc.level != null && mc.player.isAlive()
                    && !mc.player.isSpectator() && mc.isWindowActive()) mc.setScreen(new AbilityWheelScreen());
        }
        while (ModKeyMappings.OPEN_POSSESSION_SCREEN.consumeClick())
            if (mc.screen == null && mc.player != null) mc.setScreen(new PossessionScreen());
    }
}
