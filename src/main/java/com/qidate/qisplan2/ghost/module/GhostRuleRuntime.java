package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceSession;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 统一的厉鬼规则运行时。
 *
 * 负责将语义事件分发给宿主所持有模块定义中的规则。
 * 同一次事件中的规则可以共享灵异影响会话。
 */
public final class GhostRuleRuntime {

    private GhostRuleRuntime() {
    }

    /**
     * 向宿主持有的所有模块规则分发事件。
     *
     * 兼容旧调用：本方法自行创建会话，并在分发完成后裁决。
     * 若调用方需要让多个模块共享同一会话，应使用
     * 带有 session 参数的重载。
     */
    public static void dispatch(
            GhostModuleHost host,
            GhostEvent event
    ) {
        if (host == null || event == null) {
            return;
        }

        GhostInfluenceSession session =
                new GhostInfluenceSession();

        try {
            List<GhostModuleData.Entry> moduleEntries =
                    List.copyOf(host.getModules());

            for (GhostModuleData.Entry entry : moduleEntries) {
                if (!canRunRules(host, entry.id())) {
                    continue;
                }

                dispatchModule(host, entry, event, session);
            }
        } finally {
            session.resolveAndApply();
        }
    }

    /**
     * 向宿主持有的所有模块规则分发事件。
     *
     * 使用调用方提供的会话，不在此处自行裁决。
     */
    public static void dispatch(
            GhostModuleHost host,
            GhostEvent event,
            GhostInfluenceSession session
    ) {
        if (host == null || event == null || session == null) {
            return;
        }

        List<GhostModuleData.Entry> moduleEntries =
                List.copyOf(host.getModules());

        for (GhostModuleData.Entry entry : moduleEntries) {
            if (!canRunRules(host, entry.id())) {
                continue;
            }

            dispatchModule(host, entry, event, session);
        }
    }

    /**
     * 只向指定模块的规则分发事件。
     *
     * 兼容旧调用：本方法自行创建会话，并在分发完成后裁决。
     */
    public static void dispatchModule(
            GhostModuleHost host,
            GhostModuleData.Entry entry,
            GhostEvent event
    ) {
        if (host == null || entry == null || event == null) {
            return;
        }

        GhostInfluenceSession session =
                new GhostInfluenceSession();

        try {
            dispatchModule(host, entry, event, session);
        } finally {
            session.resolveAndApply();
        }
    }

    /**
     * 向指定模块分发事件，并使用共享会话。
     *
     * 本方法不负责结束会话，由最外层调用方统一裁决。
     */
    public static void dispatchModule(
            GhostModuleHost host,
            GhostModuleData.Entry entry,
            GhostEvent event,
            GhostInfluenceSession session
    ) {
        if (host == null || entry == null
                || event == null || session == null) {
            return;
        }

        if (!canRunRules(host, entry.id())) {
            return;
        }

        GhostModule module =
                GhostModuleRegistry.get(entry.id());

        if (module == null) {
            return;
        }

        List<GhostRule> rules =
                List.copyOf(module.getRules());

        for (GhostRule rule : rules) {
            if (!canRunRules(host, entry.id())) {
                break;
            }

            if (!rule.supports(event)) {
                continue;
            }

            GhostRuleContext context =
                    new GhostRuleContext(
                            host,
                            entry.id(),
                            rule.getId(),
                            session
                    );

            if (!rule.canTrigger(context, event)) {
                continue;
            }

            // canTrigger 也可能改变模块状态。
            if (!canRunRules(host, entry.id())) {
                break;
            }

            rule.onTrigger(context, event);
        }
    }

    /**
     * 检查模块是否存在，且没有沉睡或死机。
     */
    private static boolean canRunRules(
            GhostModuleHost host,
            ResourceLocation moduleId
    ) {
        if (!host.hasModule(moduleId)) {
            return false;
        }

        for (GhostModuleData.Entry currentEntry : host.getModules()) {
            if (currentEntry.id().equals(moduleId)) {
                return !currentEntry.sleeping()
                        && !currentEntry.deadlocked();
            }
        }

        return false;
    }
}