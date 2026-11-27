package com.qidate.qisplan2.ghost.curse;

import com.qidate.qisplan2.ghost.module.influence.GhostInfluence;
import com.qidate.qisplan2.ghost.module.suppression.GhostSuppressionLedger;
import net.minecraft.network.chat.Component;
import com.qidate.qisplan2.ghost.module.GhostModuleDataModifier;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;

import com.qidate.qisplan2.ghost.module.event.SupernaturalSuppressionEvent;
import com.qidate.qisplan2.ghost.module.host.GhostPersistentSuppression;
import com.qidate.qisplan2.ghost.module.event.SupernaturalAttackEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostAttackContext;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import java.util.function.Predicate;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.network.curse.CurseNetwork;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 诅咒实例管理器。
 *
 * 负责：
 *
 * 1. 注册诅咒
 * 2. 注销诅咒
 * 3. 查询诅咒
 * 4. 每 Tick 执行诅咒
 * 5. 自动清理失效诅咒
 */
public final class CurseManager {
    private static final Map<UUID, Curse.Status> LAST_STATUS=new HashMap<>();
    /** 目标索引使事件只查询该对象身上的诅咒，不再扫描全服诅咒。 */
    private static final Map<UUID, Map<UUID, Curse>> BY_TARGET = new HashMap<>();
    /** 将加载后的实例纳入两个索引；相同实例 ID 替换时先删除旧目标映射。 */
    private static void index(Curse curse) {
        var old=CURSES.put(curse.getId(),curse);
        if(old!=null){var entries=BY_TARGET.get(old.getTarget());if(entries!=null){entries.remove(old.getId());if(entries.isEmpty())BY_TARGET.remove(old.getTarget());}}
        BY_TARGET.computeIfAbsent(curse.getTarget(),k->new LinkedHashMap<>()).put(curse.getId(),curse);
    }
    /** 查找已加载的目标，允许非玩家生物承受诅咒；不强制加载区块。 */
    public static LivingEntity target(MinecraftServer server,UUID id) {
        if(server==null)return null;
        for(var level:server.getAllLevels()){var entity=level.getEntity(id);if(entity instanceof LivingEntity living)return living;}
        return null;
    }
    /** 按该诅咒自己的强度评估压制，任何已成立的压制使持续效果沉寂。 */
    public static Curse.Status status(MinecraftServer server,Curse curse){
        var target=target(server,curse.getTarget());if(target==null || !target.isAlive())return Curse.Status.DORMANT;
        return GhostPersistentSuppression.curseState(target,curse.getId(),curse.getType(),curse.getStrength()).canAct()?Curse.Status.RUNNING:Curse.Status.DORMANT;
    }
    /** 常态复苏倍率来自有效声明，多个同类效果不重复相乘。 */
    public static double revivalMultiplier(LivingEntity target){
        double multiplier=1;
        for(var curse:getByTarget(target.getUUID())){var type=CurseRegistry.get(curse.getType());if(type!=null&&curse.isValid(target.getServer())&&curse.status(target.getServer())==Curse.Status.RUNNING)multiplier=Math.max(multiplier,type.revivalMultiplier());}
        return multiplier;
    }
    /** 持续保护以压制事实触发能力，绝对压制不能用有限强度抵消。 */
    public static boolean blocksSuppression(LivingEntity target,double strength){
        if(!Double.isFinite(strength)||strength<=0||target.getServer()==null)return false;
        return resolveSuppression(target,strength,null).isBlocked();
    }
    /** 压制与袭击分别产生语义事实，但使用相同强度对抗上下文和同源死机回调。 */
    public static SupernaturalAttackEvent resolveSuppression(LivingEntity target,double strength,
            GhostInfluence source){
        return resolveSuppressionDetailed(target,strength,source).event();
    }
    /** 记录真正使本次压制失效的诅咒实例，展示时不再猜测原因或重放能力。 */
    public record SuppressionBlocker(UUID instance,ResourceLocation type,double strength) {}
    public record SuppressionResult(SupernaturalAttackEvent event,List<SuppressionBlocker> blockers) {
        public SuppressionResult { blockers=List.copyOf(blockers); }
    }
    /** 与真实压制结算共用的带解释结果；没有保护时不会制造抵挡记录。 */
    public static SuppressionResult resolveSuppressionDetailed(LivingEntity target,double strength,GhostInfluence source){
        var blockers=new ArrayList<SuppressionBlocker>();
        var incoming=new SupernaturalAttackEvent(target.getUUID(),strength);
        var attack=new GhostAttackContext(incoming,source==null?null:source.moduleId(),()->{
            if(source!=null && source.host() instanceof GhostModuleDataModifier modifier)
                modifier.setDeadlocked(source.moduleId(),true);
        },()->strength);
        for(var curse:getByTarget(target.getUUID())) {
            if(!curse.isValid(target.getServer())||curse.status(target.getServer())!=Curse.Status.RUNNING)continue;
            var type=CurseRegistry.get(curse.getType());if(type==null)continue;
            var event=new SupernaturalSuppressionEvent(target.getUUID(),strength);
            type.program().dispatch(event,new CurseAbilityContext(curse.getId(),curse.getTarget(),curse.getStrength(),event,attack,()->remove(target.getServer(),curse.getId()),null));
            if(incoming.isBlocked()){
                blockers.add(new SuppressionBlocker(curse.getId(),curse.getType(),curse.getStrength()));
                break;
            }
        }
        return new SuppressionResult(incoming,blockers);
    }
    /** 直接调用持续诅咒的能力也检查存活、压制状态和能力媒介，不允许旁路沉寂。 */
    public static boolean execute(MinecraftServer server,UUID instance,ResourceLocation ability,GhostEvent event){
        var curse=get(instance);if(curse==null||!curse.isValid(server)||curse.status(server)!=Curse.Status.RUNNING)return false;
        var type=CurseRegistry.get(curse.getType());if(type==null)return false;
        return type.program().execute(ability,new CurseAbilityContext(instance,curse.getTarget(),curse.getStrength(),event,null,()->remove(server,instance),()->curse.tick(server)));
    }
    /**
     * 在目标死亡已确认后，按各诅咒声明清理；使用目标索引的副本，允许安全删除。
     * 统一 remove 负责释放持续效果、压制关系、更新存档并同步客户端。
     * 保留实例不更换 ID，也不重置其强度、剩余寿命或独有状态。
     */
    public static void onTargetDeath(MinecraftServer server, UUID target) {
        for (Curse curse : getByTarget(target)) {
            CurseType type = CurseRegistry.get(curse.getType());
            if (type == null || !type.persistsAfterDeath()) remove(server, curse.getId());
            else curse.suspend(server);
        }
    }

    /** 按目标与类型移除全部匹配实例，所有删除都走同一释放、存档和同步入口。 */
    public static int removeTarget(MinecraftServer server,UUID target,ResourceLocation type){
        int removed=0;for(var curse:getByTarget(target))if(type==null||type.equals(curse.getType())){remove(server,curse.getId());removed++;}return removed;
    }


    /*
     * 所有正在运行中的诅咒。
     *
     * Key：
     *     Curse 实例 UUID
     *
     * Value：
     *     Curse 实例
     */
    private static final Map<UUID, Curse> CURSES =
            new HashMap<>();

    private CurseManager() {
    }

    /**
     * 注册一个诅咒。
     *
     * 如果已经存在相同 ID，
     * 则覆盖原来的实例。
     */
    public static void add(
            MinecraftServer server,
            Curse curse
    ) {
        if(CURSES.containsKey(curse.getId()))remove(server,curse.getId());
        index(curse);

        QisPlan2.LOGGER.info(
                "[诅咒系统] 注册诅咒：{}，目标：{}",
                curse.getId(),
                curse.getTarget()
        );

        /*
         * 诅咒列表发生变化，
         * 通知持久化系统。
         */
        CurseSavedData.markDirty();

        /*
         * 诅咒列表发生变化，
         * 同步目标玩家的客户端状态。
         */
        syncTarget(
                server,
                curse.getTarget()
        );
    }

    /**
     * 注销一个诅咒，同时释放世界效果与持续压制关系。
     */
    public static void remove(
            MinecraftServer server,
            UUID curseId
    ) {

        Curse removed =
                CURSES.remove(curseId);

        /*
         * 只有真的删除了一个诅咒，
         * 才需要更新 SavedData。
         */
        if (removed == null) {
            return;
        }

        LAST_STATUS.remove(curseId);
        removed.suspend(server);
        var body=target(server,removed.getTarget());
        if(body!=null)GhostPersistentSuppression.remove(body,removed.getId());
        var indexed=BY_TARGET.get(removed.getTarget());
        if(indexed!=null){indexed.remove(curseId);if(indexed.isEmpty())BY_TARGET.remove(removed.getTarget());}
        QisPlan2.LOGGER.info(
                "[诅咒系统] 注销诅咒：{}",
                curseId
        );

        /*
         * 诅咒列表发生变化，
         * 通知持久化系统。
         */
        CurseSavedData.markDirty();

        /*
         * 同步原诅咒目标玩家的客户端状态。
         */
        syncTarget(
                server,
                removed.getTarget()
        );
    }

    /**
     * 通过来源来注销一个诅咒。
     */
    public static void removeBySource(
            MinecraftServer server,
            Predicate<Curse> predicate
    ) {
        List<UUID> removeIds = new ArrayList<>();

        for (Curse curse : CURSES.values()) {
            if (predicate.test(curse)) {
                removeIds.add(curse.getId());
            }
        }

        for (UUID id : removeIds) {
            remove(
                    server,
                    id
            );
        }
    }

    /**
     * 将诅咒加入运行时管理器。
     *
     * 仅用于从 SavedData 恢复诅咒。
     *
     * 不触发 SavedData 更新，
     * 因为当前正在读取 SavedData。
     */
    public static void restore(
            Curse curse
    ) {

        index(curse);
    }

    /** 强度或寿命被刷新后同时更新存档与目标展示，避免 HUD 使用旧到期时间。 */
    public static void changed(MinecraftServer server,Curse curse){
        if(CURSES.get(curse.getId())!=curse)return;
        markDirty();syncTarget(server,curse.getTarget());
    }

    /**
     * 标记诅咒数据需要保存。
     */
    public static void markDirty() {
        CurseSavedData.markDirty();
    }

    /**
     * 同步某个玩家当前受到的全部诅咒。
     *
     * <p>
     * 如果目标玩家当前不在线，则不进行同步。
     * </p>
     */
    private static void syncTarget(
            MinecraftServer server,
            UUID target
    ) {

        var player =
                server.getPlayerList()
                        .getPlayer(target);

        if (player == null) {
            return;
        }

        CurseNetwork.sync(player);
    }

    /**
     * 根据 ID 获取诅咒。
     */
    public static Curse get(
            UUID curseId
    ) {

        return CURSES.get(curseId);
    }

    /**
     * 获取当前所有诅咒。
     *
     * 返回副本，
     * 防止外部直接修改 Manager 内部数据。
     */
    public static Collection<Curse> getAll() {

        return List.copyOf(
                CURSES.values()
        );
    }

    /**
     * 获取某个玩家当前受到的所有诅咒。
     */
    public static List<Curse> getByTarget(
            UUID target
    ) {

        var entries=BY_TARGET.get(target);
        return entries==null?List.of():List.copyOf(entries.values());
    }

    /**
     * 每个服务器 Tick 调用一次。
     */
    public static void tick(
            MinecraftServer server
    ) {

        /*
         * 使用副本遍历。
         *
         * 因为 tick() 或清理过程中，
         * 可能会修改 CURSES。
         */
        List<Curse> curses =
                new ArrayList<>(
                        CURSES.values()
                );

        for (Curse curse : curses) {

            /*
             * 诅咒已经失效。
             */
            if (!curse.isValid(server)) {

                remove(
                        server,
                        curse.getId()
                );

                continue;
            }

            /*
             * 执行诅咒逻辑。
             */
            if(CURSES.get(curse.getId())!=curse)continue;
            var body=target(server,curse.getTarget());
            var declaration=CurseRegistry.get(curse.getType());
            // 旁观者死亡等路径可能跳过掉落阶段；在服务端 tick 中对已死亡目标兜底。
            if(body!=null && !body.isAlive() && (declaration==null || !declaration.persistsAfterDeath())) {
                remove(server,curse.getId());
                continue;
            }
            if(body!=null){
                var offer=declaration==null||curse.status(server)!=Curse.Status.RUNNING?null:declaration.suppression().offer(1,curse.getStrength());
                if(offer==null)GhostPersistentSuppression.remove(body,curse.getId());
                else GhostPersistentSuppression.put(body,curse.getId(),offer,
                        new GhostSuppressionLedger.Source("curse",CurseRegistry.owner(curse.getType()),
                                Component.translatable("curse."+curse.getType().getNamespace()+"."+curse.getType().getPath())));
            }
            var currentStatus=curse.status(server);
            if(LAST_STATUS.put(curse.getId(),currentStatus)!=currentStatus)syncTarget(server,curse.getTarget());
            curse.tickDormant();
            if(curse.status(server)==Curse.Status.DORMANT){curse.suspend(server);}
            else {
                var type=CurseRegistry.get(curse.getType());
                var event=new ModuleTickEvent();
                if(type!=null)type.program().dispatch(event,new CurseAbilityContext(curse.getId(),curse.getTarget(),curse.getStrength(),event,null,()->remove(server,curse.getId()),()->curse.tick(server)));
            }
            if(!curse.isValid(server))remove(server,curse.getId());
            if(curse.getRemainingTicks()>=0)markDirty();
        }
    }

    /**
     * 清空所有诅咒。
     *
     * 主要用于服务器关闭或重新初始化时。
     *
     * 这里不能调用 markDirty()。
     *
     * 因为服务器关闭、重新加载存档时，
     * clear() 只是清理运行时状态，
     * 不能把 SavedData 改成“空诅咒列表”。
     */
    public static void clear() {

        CURSES.clear();
        BY_TARGET.clear();
        LAST_STATUS.clear();
    }
}
