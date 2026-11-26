package com.qidate.qisplan2.ghost.module.host;

import com.qidate.qisplan2.ghost.isolation.GhostIsolationSystem;
import com.qidate.qisplan2.ghost.domain.GhostDomainManager;
import net.minecraft.core.BlockPos;

import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceExecution;

import net.minecraft.nbt.CompoundTag;

import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import com.qidate.qisplan2.ghost.module.host.attachment.GhostAttachments;
import com.qidate.qisplan2.ghost.module.suppression.*;
import com.qidate.qisplan2.ghost.curse.CurseManager;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** 鬼域与持续能力维护的关系表。稳定关系幂等更新，查询不会重新扫描世界。 */
public final class GhostPersistentSuppression {
    private static final Map<LivingEntity, GhostSuppressionLedger> TARGETS = new WeakHashMap<>();
    private GhostPersistentSuppression() {}
    /** 只保留当前权限查询的抵挡解释，不额外重放规则；弱键不延长实体生命周期。 */
    private static final Map<LivingEntity, ProtectionReport> PROTECTION = new WeakHashMap<>();
    public record ProtectionReport(GhostSuppressionState raw, GhostSuppressionState effective,
                                   List<CurseManager.SuppressionBlocker> blockers) {}
    /** 命令读取最近一次实际查询结果，不再次调用诅咒能力。 */
    public static ProtectionReport protectionReport(LivingEntity target) { return PROTECTION.get(target); }

    /** 接口边界负责识别身体，厉鬼定义只声明效果。 */
    public static GhostModuleHost host(LivingEntity target){return target instanceof Player player?new PlayerGhostModuleHost(player):target instanceof GhostModuleHost host?host:null;}
    /** 身体承载与装备是不同宿主，但所在环境可以同时压制两者。 */
    private static boolean carriesSupernaturalContent(LivingEntity target){
        return !CurseManager.getByTarget(target.getUUID()).isEmpty()
                || GhostLivingHosts.collect(target).stream().anyMatch(h->h.getModules().stream().anyMatch(e->h.carriesModule(e.id())));
    }
    /**
     * 非身体宿主使用同一环境压制接口。物品从携带者取得位置和外来关系；
     * 有位置的方块从已缓存鬼域几何查询，不遍历世界中的厉鬼或方块。
     */
    public static GhostSuppressionState environment(GhostModuleHost host,ResourceLocation id){
        double strength=host.getModules().stream().filter(e->e.id().equals(id)).mapToDouble(e->e.intensity()).findFirst().orElse(0);
        var bearer=GhostEnvironmentAdapters.bearer(host);
        if(bearer!=null)return protect(bearer,curseState(bearer,new UUID(0,0),id,strength));
        var level=ModuleGhostDomains.level(host);if(level==null)return GhostSuppressionState.NONE;
        var point=host.moduleWorld(id).position();
        if(point==null || GhostIsolationSystem.getRegionId(level,BlockPos.containing(point))!=null)return GhostSuppressionState.NONE;
        var manager=GhostDomainManager.get(level);
        var result=GhostSuppressionState.NONE;
        for(var domain:manager.getDomains())if(domain.getBehavior() instanceof ModuleGhostDomainBehavior behavior && !behavior.belongsTo(host)){
            var offer=behavior.suppressionAt(domain,manager.conflict().weight(domain.getId(),point));
            if(offer!=null)result=result.merge(of(offer,strength));
        }
        return result;
    }
    /** 更新一条外来关系；普通目标也可有诅咒，不能在此仅过滤实体厉鬼。 */
    public static void put(LivingEntity target,UUID source,GhostSuppression offer,GhostSuppressionLedger.Source owner){
        if(!carriesSupernaturalContent(target)){remove(target,source);return;}
        TARGETS.computeIfAbsent(target,k->new GhostSuppressionLedger()).put(source,new GhostSuppressionLedger.Relation(offer,false,null,owner));}
    /** 退出覆盖或媒介消失时仅撤销该来源。 */
    public static void remove(LivingEntity target,UUID source){var ledger=TARGETS.get(target);if(ledger!=null){ledger.remove(source);if(ledger.isEmpty())TARGETS.remove(target);}}
    /** 诊断读取实际账本，不触发能力或重新扫描覆盖。 */
    public static Map<UUID,GhostSuppressionLedger.Relation> relations(LivingEntity target){var ledger=TARGETS.get(target);return ledger==null?Map.of():ledger.snapshot();}
    /** 查询持续能力关系。 */
    public static GhostSuppressionState state(LivingEntity target,ResourceLocation id,double strength){var ledger=TARGETS.get(target);return ledger==null?GhostSuppressionState.NONE:ledger.state(id,strength);}
    /** 按诅咒自身强度评估所有身体压制，不能借用宿主模块的强度。 */
    public static GhostSuppressionState curseState(LivingEntity target,UUID instance,ResourceLocation id,double strength){
        var result=GhostSuppressionState.NONE;
        var ledger=TARGETS.get(target);
        if(ledger!=null)for(var relation:ledger.snapshot().entrySet())if(!relation.getKey().equals(instance))result=result.merge(of(relation.getValue().suppression(),strength));
        for(var relation:GhostSoundSuppression.relations(target.getUUID()).values())result=result.merge(of(relation.suppression(),strength));
        for(var relation:GhostAttachments.suppressionSources(target))result=result.merge(of(relation.suppression(),strength));
        var pulse=target.getPersistentData().getCompound("QisPlan2SuppressionPulse");
        if(pulse.getLong("until")>target.level().getGameTime())result=result.merge(of(new GhostSuppression(GhostSuppression.Kind.NORMAL,pulse.getDouble("strength")),strength));
        return result;
    }
    /** 将一条声明换算成该目标的权限状态。 */
    private static GhostSuppressionState of(GhostSuppression offer,double strength){return new GhostSuppressionState(offer.level(strength),GhostSuppression.Level.NONE,offer.kind()==GhostSuppression.Kind.ABSOLUTE?Double.POSITIVE_INFINITY:offer.strength());}
    /** 普通短时压制覆盖身体及诅咒，重复命中只刷新一秒，不叠加。 */
    public static boolean pulse(LivingEntity target,double strength){
        if(target.level().isClientSide()||!Double.isFinite(strength)||strength<=0)return false;
        if(!carriesSupernaturalContent(target))return false;
        var resolved=GhostInfluenceExecution.claimResolvedAttack(target.getUUID());
        if(resolved==null)resolved=CurseManager.resolveSuppression(target,strength,null);
        if(resolved.isBlocked()||resolved.remainingIntensity()<=0)return false;
        strength=resolved.remainingIntensity();
        var tag=new CompoundTag();tag.putLong("until",target.level().getGameTime()+20);tag.putDouble("strength",strength);target.getPersistentData().put("QisPlan2SuppressionPulse",tag);
        for(var host:GhostLivingHosts.collect(target))GhostModuleCombatState.suppressForOneSecond(host,strength);return true;
    }
    /** 持续压制也必须经过目标身上持续保护的对抗。 */
    public static GhostSuppressionState protect(LivingEntity target,GhostSuppressionState state){
        if(state.pressure()<=0 || !Double.isFinite(state.pressure()) || target.getServer()==null){
            PROTECTION.remove(target);return state;
        }
        var report=protectionReport(state,CurseManager.resolveSuppressionDetailed(target,state.pressure(),null));
        PROTECTION.put(target,report);
        return report.effective();
    }
    /** 抵挡改变最终许可，但保留原始压制状态和实际保护来源供诊断解释。 */
    public static ProtectionReport protectionReport(GhostSuppressionState raw,CurseManager.SuppressionResult result){
        return new ProtectionReport(raw,result.event().isBlocked()?GhostSuppressionState.NONE:raw,result.blockers());
    }
    /** 服务端退出后清空临时世界关系。 */
    public static void clear(){TARGETS.clear();PROTECTION.clear();}
}
