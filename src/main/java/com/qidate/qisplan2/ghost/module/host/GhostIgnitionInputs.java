package com.qidate.qisplan2.ghost.module.host;

import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.event.IgnitionChangedEvent;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomModelData;

/** 输入、旧存档迁移和模型表现属于适配层，不属于鬼烛模块的规则。 */
public final class GhostIgnitionInputs {
    /**
     * 禁止外部实例化此工具类；功能统一通过静态入口调用。
     */
    private GhostIgnitionInputs() {}

    /**
     * 写入指定模块的点燃状态，并发布点燃状态变化事实。
     * 不支持点燃、模块不存在或燃料已耗尽时拒绝点燃；熄灭会清除效果标记。
     */
    public static boolean setLit(GhostModuleHost host, ResourceLocation id, boolean lit) {
        var module = GhostModuleRegistry.get(id);
        if (module == null || !module.supportsIgnition() || !host.hasModule(id)
                || !(host instanceof GhostModuleCustomDataModifier modifier)) return false;
        var custom = modifier.getModuleCustomData(id);
        if (custom.contains("remainingFuel") && custom.getDouble("remainingFuel") <= 0 && lit) return false;
        custom.putBoolean("lit", lit);
        if (!lit) custom.remove("activeEffect");
        modifier.setModuleCustomData(id, custom);
        GhostModuleRuntime.dispatch(host, new IgnitionChangedEvent(lit));
        return true;
    }

    /**
     * 迁移旧物品模型中保存的点燃状态。
     * 只有模块尚未保存 lit 字段时才读取模型，已有模块状态不会被外观覆盖。
     */
    public static void initialize(GhostItemModuleHost host) {
        for (var entry : host.getModules()) {
            var module = GhostModuleRegistry.get(entry.id());
            if (module == null || !module.supportsIgnition() || entry.customData().contains("lit")) continue;
            var data = entry.customData();
            data.putBoolean("lit", host.stack().has(DataComponents.CUSTOM_MODEL_DATA));
            host.setModuleCustomData(entry.id(), data);
        }
    }

    /**
     * 切换物品中可用且支持点燃的模块，并同步物品外观。
     * @return 是否至少成功处理了一个模块
     */
    public static boolean toggle(GhostItemModuleHost host) {
        initialize(host);
        boolean handled = false;
        for (var entry : host.getModules()) {
            var module = GhostModuleRegistry.get(entry.id());
            if (module == null || !module.supportsIgnition() || !GhostModuleCombatState.available(entry)) continue;
            handled |= setLit(host, entry.id(), !entry.customData().getBoolean("lit"));
        }
        syncModel(host);
        return handled;
    }

    /**
     * 根据模块的可用性和点燃状态更新物品模型组件。
     * 模型只展示结果，不能替代模块数据成为点燃状态的来源。
     */
    public static void syncModel(GhostItemModuleHost host) {
        boolean supports = false, lit = false;
        for (var entry : host.getModules()) {
            var module = GhostModuleRegistry.get(entry.id());
            if (module == null || !module.supportsIgnition()) continue;
            supports = true;
            lit |= GhostModuleCombatState.available(entry) && entry.customData().getBoolean("lit");
        }
        if (!supports) return;
        if (lit) host.stack().set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        else host.stack().remove(DataComponents.CUSTOM_MODEL_DATA);
    }
}
