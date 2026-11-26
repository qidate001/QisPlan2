package com.qidate.qisplan2.ghost.module;

import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import com.qidate.qisplan2.ghost.module.event.ItemCarriedEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceSession;

/** 将携带底层事实解释成语义；仅查询已安装模块的观察计划，共用本轮裁决会话。 */
public final class GhostCarriedItemObserver {
    private GhostCarriedItemObserver() {}

    /** 每个范围每个模块每轮只查询一次实体；没有订阅、不暴露、死机或规则被压制时不扫描。 */
    static void observe(GhostModuleHost host, GhostInfluenceSession session) {
        for (var entry : List.copyOf(host.getModules())) {
            var subscriptions = GhostEventSubscriptions.carried(entry.id());
            if (subscriptions.isEmpty() || entry.deadlocked() || !host.carriesModule(entry.id())
                    || !host.suppression(entry.id()).canRunRules()) continue;
            var world = new GhostModuleContext(host, entry.id()).world();
            if (!world.present()) continue;
            var areas = new HashMap<AABB,List<LivingEntity>>();
            var delivered = new HashMap<GhostRule,Set<LivingEntity>>();
            for (var subscription : subscriptions) {
                if (!host.hasModule(entry.id())) break;
                var observation = subscription.observation();
                var seen = delivered.computeIfAbsent(subscription.rule(), ignored -> new HashSet<>());
                for (var target : areas.computeIfAbsent(observation.area(), world::livingEntities)) {
                    if (seen.contains(target)) continue;
                    // 背包结构归世界端口管理；查询命中即停止，不为每个相同物品重复触发。
                    ItemStack[] matched = {ItemStack.EMPTY};
                    boolean found = world.hasCarriedItem(target, stack -> {
                        if (!observation.items().matches(stack, target)) return false;
                        matched[0] = stack.copy(); return true;
                    });
                    if (found) {
                        seen.add(target);
                        GhostRuleRuntime.dispatchSubscribed(host, entry,
                                new ItemCarriedEvent(target, matched[0]), session, List.of(subscription.rule()));
                    }
                }
            }
        }
    }
}
