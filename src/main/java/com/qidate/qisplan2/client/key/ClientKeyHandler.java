package com.qidate.qisplan2.client.key;

import com.qidate.qisplan2.client.screen.AbilityWheelPreferences;
import com.mojang.blaze3d.platform.InputConstants;
import com.qidate.qisplan2.client.screen.PossessionScreen.PossessionScreen;
import com.qidate.qisplan2.client.screen.AbilityWheelScreen;
import net.minecraft.client.Minecraft;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityCatalog;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostData;
import com.qidate.qisplan2.network.payload.GhostAbilityUsePayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/** 按住打开、松开提交；一次按住最多打开一次，鼠标确认后不会立刻重开。 */
public final class ClientKeyHandler {
    private static boolean latched;

    /** 输入管理器无需实例化。 */
    private ClientKeyHandler() {}

    /** 注册面板、轮盘和模块声明的通用鬼域操作按键。 */
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.OPEN_POSSESSION_SCREEN);
        event.register(ModKeyMappings.ABILITY_WHEEL);
        for (var key : ModKeyMappings.WHEEL_SLOTS) event.register(key);
        ModKeyMappings.DOMAIN_SHORTCUTS.values().forEach(event::register);
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

    /**
     * 快捷键只选择能力，不执行效果。多个已开启鬼域提供同一操作时，按模块保存顺序取第一个。
     * 输入每帧消费，界面、失焦、死亡时丢弃，避免关闭界面后补发；服务端仍重新校验媒介。
     */
    private static void handleDomainShortcuts(Minecraft mc) {
        for (var binding : ModKeyMappings.DOMAIN_SHORTCUTS.entrySet()) {
            boolean clicked = false;
            while (binding.getValue().consumeClick()) clicked = true;
            if (!clicked || mc.screen != null || mc.player == null || mc.level == null
                    || !mc.player.isAlive() || mc.player.isSpectator() || !mc.isWindowActive()) continue;
            var views = PlayerGhostData.views(mc.player);
            GhostAbilityCatalog.all(new PlayerGhostModuleHost(mc.player)).stream()
                    .filter(entry -> entry.ability().domainShortcut() == binding.getKey())
                    .filter(entry -> views.containsKey(entry.moduleId()) && views.get(entry.moduleId()).domain()
                            .map(domain -> domain.open()).orElse(false))
                    .findFirst().ifPresent(entry -> PacketDistributor.sendToServer(
                            new GhostAbilityUsePayload(entry.moduleId(), entry.ability().getId())));
        }
    }

    /** 只在游戏中允许打开；失焦、死亡和其他界面都不会产生技能请求。 */
    public static void clientTick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        handleDomainShortcuts(mc);
        for (int i = 0; i < ModKeyMappings.WHEEL_SLOTS.length; i++) {
            boolean pressed = false;
            while (ModKeyMappings.WHEEL_SLOTS[i].consumeClick()) pressed = true;
            if (!pressed || mc.screen != null || mc.player == null || !mc.player.isAlive()
                    || mc.player.isSpectator() || !mc.isWindowActive()) continue;
            var slots = AbilityWheelPreferences.current()
                    .resolve(GhostAbilityCatalog.all(new PlayerGhostModuleHost(mc.player)));
            var entry = slots.get(i);
            if (entry != null) PacketDistributor.sendToServer(new GhostAbilityUsePayload(entry.moduleId(), entry.ability().getId()));
        }
        boolean held = wheelHeld();
        if (!held) latched = false;
        if (mc.screen instanceof AbilityWheelScreen wheel) {
            if (!mc.isWindowActive() || mc.player == null || !mc.player.isAlive()) wheel.cancel();
            else if (held && Screen.hasAltDown()) {
                wheel.cancel();
                mc.setScreen(new PossessionScreen(PossessionScreen.ABILITIES_PAGE));
            }
            else if (!held) wheel.confirm();
        } else if (held && !latched) {
            latched = true;
            if (mc.screen == null && mc.player != null && mc.level != null && mc.player.isAlive()
                    && !mc.player.isSpectator() && mc.isWindowActive()) {
                if (Screen.hasAltDown()) mc.setScreen(new PossessionScreen(PossessionScreen.ABILITIES_PAGE));
                else mc.setScreen(new AbilityWheelScreen());
            }
        }
        while (ModKeyMappings.OPEN_POSSESSION_SCREEN.consumeClick())
            if (mc.screen == null && mc.player != null) mc.setScreen(new PossessionScreen());
    }
}
