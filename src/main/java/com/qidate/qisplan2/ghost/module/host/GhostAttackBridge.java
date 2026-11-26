package com.qidate.qisplan2.ghost.module.host;

import com.qidate.qisplan2.ghost.curse.Curse;

import com.qidate.qisplan2.ghost.curse.CurseManager;
import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.GhostModuleDataModifier;
import com.qidate.qisplan2.ghost.module.event.SupernaturalAttackEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostAttackContext;
import com.qidate.qisplan2.ghost.module.influence.GhostAttackResponses;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluence;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceExecution;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceProvider;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemData;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import com.qidate.qisplan2.death.SupernaturalIntensityTracker;
import java.util.ArrayList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** 把世界目标适配为参与对抗的模块及持续影响；不识别任何具体厉鬼。 */
public final class GhostAttackBridge {
    private GhostAttackBridge() {}

    /** 所有最终伤害入口都可调用；已处于裁决效果中时复用结果，其他来源建立独立对抗。 */
    public static SupernaturalAttackEvent resolve(LivingEntity target, double intensity) {
        var resolved = GhostInfluenceExecution.claimResolvedAttack(target.getUUID());
        return resolved != null ? resolved : resolveFresh(target, intensity, null);
    }

    /** 收集目标实际携带的模块和有效诅咒，按声明顺序执行；无关模块不会被全局遍历。 */
    public static SupernaturalAttackEvent resolveFresh(LivingEntity target, double intensity, GhostInfluence source) {
        var event = new SupernaturalAttackEvent(target.getUUID(), Math.max(0, intensity));
        if (target.level().isClientSide() || !target.isAlive()) return event;
        if (GhostPersistentSuppression.isGhost(target)) { event.block(); return event; }
        if (intensity <= 0) { event.block(); return event; }
        var hosts = new ArrayList<GhostModuleHost>();
        if (target instanceof Player player) hosts.add(new PlayerGhostModuleHost(player));
        else if (target instanceof GhostModuleHost host) hosts.add(host);
        for (var stack : target.getAllSlots())
            if (!stack.isEmpty() && !GhostItemData.getModules(stack).isEmpty())
                hosts.add(new GhostItemModuleHost(stack, target));
        var responses = GhostAttackResponses.collect(hosts);
        var server = target.getServer();
        for (var curse : CurseManager.getByTarget(target.getUUID())) {
            if (server == null || !curse.isValid(server) || curse.status(server) != Curse.Status.RUNNING || !(curse instanceof GhostInfluenceProvider provider)) continue;
            var response = provider.attackResponse(() -> CurseManager.remove(server, curse.getId()));
            if (response != null) responses.add(response);
        }
        var context = new GhostAttackContext(event, source == null ? null : source.moduleId(), () -> {
            if (source != null && source.host() instanceof GhostModuleDataModifier modifier
                    && source.host().hasModule(source.moduleId())) modifier.setDeadlocked(source.moduleId(), true);
        }, () -> SupernaturalIntensityTracker.addAndGetRecentIntensity(target, event.remainingIntensity()));
        GhostAttackResponses.apply(responses, context);
        GhostModuleRuntime.observeSleeping(hosts, event);
        return event;
    }
}
