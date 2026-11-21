package com.qidate.qisplan2.ghost.module.host;

import com.qidate.qisplan2.ghost.domain.*;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.domain.GhostDomainDefinition;
import com.qidate.qisplan2.ghost.module.event.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import net.minecraft.world.entity.player.Player;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;

/** 将有效覆盖转换成事实。此桥接器不决定伤害、袭击或某只厉鬼的规则。 */
public final class ModuleGhostDomainBehavior implements GhostDomainBehavior {
    private final GhostModuleHost host;
    private final ResourceLocation module;
    private final GhostDomainDefinition definition;
    private int elapsed;
    /** 每个鬼域拥有独立曝光周期；接触时间由宿主共享采样器统一维护。 */
    public ModuleGhostDomainBehavior(GhostModuleHost host, ResourceLocation module, GhostDomainDefinition definition) {
        this.host=host;this.module=module;this.definition=definition;
    }
    /** 只匹配同一实际宿主上的模块实例。 */
    public boolean belongsTo(GhostModuleHost other) { return ModuleGhostDomains.same(host,other); }
    /** 统一检查载体和能力状态；方块来源不依赖实体 UUID 存在。 */
    @Override public boolean sourcePresent(ServerLevel level,GhostDomain domain) {
        return ModuleGhostDomains.level(host)==level && ModuleGhostDomains.enabled(host,module);
    }
    /** 位置由宿主世界端口解释。 */
    @Override public Vec3 sourcePosition(ServerLevel level,GhostDomain domain) { return host.moduleWorld(module).position(); }
    /** 接触时间按 tick 采样；周期性曝光由模块声明的间隔控制。 */
    @Override public int getTickInterval() { return 1; }
    /** 对有效覆盖集合发出接触、看见及周期曝光，能力仍经过模块执行器。 */
    @Override public void onTick(ServerLevel level,GhostDomain domain,Set<Entity> entities) {
        if (!sourcePresent(level,domain)) return;
        var source=GhostEnvironmentAdapters.bearer(host);
        boolean exposure=definition.exposureInterval()>0 && ++elapsed>=definition.exposureInterval();
        if(exposure) elapsed=0;
        for(var entity : List.copyOf(entities)) {
            if(!(entity instanceof LivingEntity target) || !target.isAlive()) continue;
            if(!sourcePresent(level,domain)) break;

            if(target != source) {
                if(definition.touch()) GhostContactEvents.sample(host, source, target);
                if(definition.sight()) {
                    GhostModuleRuntime.dispatch(host,new TargetSeenEvent(target));
                    // “我看见目标”也意味着目标“被我看见”；沿用已有感知语义，不能反转为我被看见。
                    if(source != null) {
                        GhostModuleHost targetHost=target instanceof Player player ? new PlayerGhostModuleHost(player)
                                : target instanceof GhostModuleHost modules ? modules : null;
                        if(targetHost!=null)GhostModuleRuntime.dispatch(targetHost, new EntityPerceivedEvent(source,target));
                    }
                }
            }
            if(exposure) GhostModuleRuntime.dispatch(host, new GhostDomainExposureEvent(domain.getId(), source, target, domain.getStrength()));
        }
    }
    /** 世界管理器移除鬼域时同步撤销快速索引。 */
    @Override public void onRemove(ServerLevel level,GhostDomain domain) { ModuleGhostDomains.forget(host,module,domain); }
    /** 通用层数变化写回模块，以便转移、跨维度与读档重建。 */
    @Override public void onDomainLayerChanged(ServerLevel level,GhostDomain domain) { ModuleGhostDomains.saveLayer(host,module,domain.getLayer()); }
    /** 视觉与传送开关来自声明，层数运算保留框架默认实现。 */
    @Override public boolean hasEntityVision(ServerLevel level,GhostDomain domain) { return definition.entityVision(); }
    /** 是否允许框架化虹，不在这里实现某个厉鬼的技能。 */
    @Override public boolean canTeleport(ServerLevel level,GhostDomain domain,ServerPlayer player) { return definition.teleport(); }
}
