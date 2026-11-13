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

    /** 共享一次袭击结果，能力成功后立即阻断原袭击；普通伤害不经过此入口。 */
    public static boolean isBlocked(LivingEntity target, double intensity) {
        if (target.level().isClientSide() || !target.isAlive() || !Double.isFinite(intensity)) return false;
        var hosts = new ArrayList<GhostModuleHost>();
        if (target instanceof Player player) hosts.add(new PlayerGhostModuleHost(player));
        else if (target instanceof GhostModuleHost host) hosts.add(host);
        for (var stack : target.getAllSlots())
            if (!stack.isEmpty() && !GhostItemData.getModules(stack).isEmpty())
                hosts.add(new GhostItemModuleHost(stack, target));
        var event = new SupernaturalAttackEvent(target.getUUID(), Math.max(0, intensity));
        GhostModuleRuntime.dispatch(hosts, event);
        return event.isBlocked();
    }
}
