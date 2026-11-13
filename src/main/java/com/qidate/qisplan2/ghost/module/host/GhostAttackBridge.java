package com.qidate.qisplan2.ghost.module.host;

import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.event.SupernaturalAttackEvent;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemData;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import java.util.ArrayList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** 将灵异袭击事实发给受袭实体及其装备上的模块，不识别任何具体厉鬼。 */
public final class GhostAttackBridge {
    /** 仅提供静态分发入口。 */
    private GhostAttackBridge() {}

    /** 返回完整或部分抵挡后的事件结果；普通伤害不经过此入口。 */
    public static SupernaturalAttackEvent resolve(LivingEntity target, double intensity) {
        var event = new SupernaturalAttackEvent(target.getUUID(), Math.max(0, intensity));
        if (target.level().isClientSide() || !target.isAlive()) return event;
        var hosts = new ArrayList<GhostModuleHost>();
        if (target instanceof Player player) hosts.add(new PlayerGhostModuleHost(player));
        else if (target instanceof GhostModuleHost host) hosts.add(host);
        for (var stack : target.getAllSlots())
            if (!stack.isEmpty() && !GhostItemData.getModules(stack).isEmpty())
                hosts.add(new GhostItemModuleHost(stack, target));
        GhostModuleRuntime.dispatch(hosts, event);
        return event;
    }
}
