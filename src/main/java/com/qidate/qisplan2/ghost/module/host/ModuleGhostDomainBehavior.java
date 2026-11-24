package com.qidate.qisplan2.ghost.module.host;

import com.qidate.qisplan2.ghost.domain.*;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.domain.GhostDomainDefinition;
import com.qidate.qisplan2.ghost.module.domain.GhostDomainSettings;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityExecutor;
import com.qidate.qisplan2.ghost.module.ability.GhostTargetParameters;
import com.qidate.qisplan2.ghost.module.event.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
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
    private int automationTicks;
    private GhostDomainSettings settings=GhostDomainSettings.DEFAULT;
    private CompoundTag settingsTag;
    /** 管理器同样校验模块成长上限，命令不能绕过能力入口限制。 */
    @Override public int maxGrade(){return ModuleGhostDomains.maxGrade(host,module);}
    /** 默认等级不受成长门槛或强度降低影响。 */
    @Override public int minGrade(){return definition.minGrade();}
    /** 分层是模块声明的能力，等级高并不意味着拥有多个独立层。 */
    @Override public boolean supportsLayering() { return definition.layered(); }
    /** 配置只在变化时重新解码，边界检查不会为每个实体反复解析完整配置。 */
    private GhostDomainSettings settings() {
        var next=((GhostModuleCustomDataModifier)host).getModuleCustomData(module).getCompound(GhostDomainSettings.KEY);
        if(!next.equals(settingsTag)){settingsTag=next.copy();settings=GhostDomainSettings.read(((GhostModuleCustomDataModifier)host).getModuleCustomData(module), definition.defaultSettings());}
        return settings;
    }
    /** 来源自身不受自己鬼域的进出限制，其余实体由配置分类判断。 */
    @Override public boolean permitsCrossing(ServerLevel level,GhostDomain domain,Entity target,boolean entering) {
        if(target==GhostEnvironmentAdapters.bearer(host))return true;
        var facts=GhostDomainTargetFacts.of(target);var access=entering?settings().enter():settings().leave();
        return access.allows(facts.group(),facts.name(),facts.type());
    }
    /** 每秒执行一次自动化；按列表顺序第一条匹配规则生效，避免多个分层规则反复争抢目标。 */
    private void automate(ServerLevel level, GhostDomain domain, Set<Entity> entities) {
        if(++automationTicks<20)return;automationTicks=0;
        var rules=settings().rules();if(rules.isEmpty())return;
        var definition=GhostModuleRegistry.get(module);if(definition==null)return;
        var allowed=definition.domainPanel().automations();
        for(Entity entity:entities){
            if(!(entity instanceof LivingEntity target) || !target.isAlive() || target==GhostEnvironmentAdapters.bearer(host))continue;
            var facts=GhostDomainTargetFacts.of(target);
            for(var rule:rules){
                if(!rule.enabled() || allowed.stream().noneMatch(a->a.ability().equals(rule.ability()))
                        || !GhostDomainSettings.matches(rule.targets(),facts.group(),facts.name(),facts.type(),true))continue;
                if(!host.hasModule(module))return;
                GhostDomainInfluenceScope.run(GhostDomainManager.get(level).influence(domain,target), () ->
                        GhostAbilityExecutor.tryExecuteWithResult(new GhostAbilityContext(host,module,rule.ability(),
                        GhostAbilityContext.Source.CONTINUATION,null,()->{},new GhostTargetParameters(target))));
                break;
            }
        }
    }
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
        automate(level,domain,entities);
        if(!sourcePresent(level,domain))return;
        var source=GhostEnvironmentAdapters.bearer(host);
        boolean exposure=definition.exposureInterval()>0 && ++elapsed>=definition.exposureInterval();
        if(exposure) elapsed=0;
        for(var entity : List.copyOf(entities)) {
            if(!(entity instanceof LivingEntity target) || !target.isAlive()) continue;
            if(!sourcePresent(level,domain)) break;

            double weight=GhostDomainManager.get(level).influence(domain,target);
            if(weight<=0)continue;
            final boolean exposeNow=exposure;
            GhostDomainInfluenceScope.run(weight,()->{
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
            if(exposeNow) GhostModuleRuntime.dispatch(host, new GhostDomainExposureEvent(domain.getId(), source, target, domain.getStrength()));
            });
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
