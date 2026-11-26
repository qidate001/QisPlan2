package com.qidate.qisplan2.ghost.module.suppression;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

/** 单个目标的持续关系账本。相同来源更新相同声明不产生状态变化，也不使缓存失效。 */
public final class GhostSuppressionLedger {
    /** excludedModule 仅排除来源自身模块；同一身体的其他模块仍可受影响。 */
    /** 持续影响的所属模块和物理来源；鬼域/诅咒实例 ID 不能替代完整模块身份。 */
    public record Source(String kind, ResourceLocation module, Component description) {}
    public record Relation(GhostSuppression suppression, boolean internal, ResourceLocation excludedModule, Source source) {
        /** 兼容没有额外描述的声音及旧调用，压制计算不依赖展示元数据。 */
        public Relation(GhostSuppression suppression, boolean internal, ResourceLocation excludedModule) {
            this(suppression, internal, excludedModule, null);
        }
    }
    private record Cached(double strength, GhostSuppressionState state) {}
    private final Map<UUID, Relation> relations = new HashMap<>();
    private final Map<ResourceLocation, Cached> cache = new HashMap<>();
    private long revision;

    /** 幂等更新具体来源，不把多台钢琴合并成一个无法撤销的布尔量。 */
    public void put(UUID source, Relation relation) {
        if (relation.equals(relations.get(source))) return;
        relations.put(source, relation); cache.clear(); revision++;
    }
    /** 只撤销指定来源，其他关系保持有效。 */
    public void remove(UUID source) {
        if (relations.remove(source) != null) { cache.clear(); revision++; }
    }
    /** 关系没有变化时只读缓存；目标强度发生改变时仅重算该模块。 */
    public GhostSuppressionState state(ResourceLocation module, double strength) {
        var old = cache.get(module);
        if (old != null && old.strength() == strength) return old.state();
        var external = GhostSuppression.Level.NONE;
        var internal = GhostSuppression.Level.NONE;
        double pressure = 0;
        for (var relation : relations.values()) {
            if (module.equals(relation.excludedModule())) continue;
            var level = relation.suppression().level(strength);
            pressure = Math.max(pressure, relation.suppression().kind() == GhostSuppression.Kind.ABSOLUTE
                    ? Double.POSITIVE_INFINITY : relation.suppression().strength());
            if (relation.internal()) { if (level.ordinal() > internal.ordinal()) internal = level; }
            else if (level.ordinal() > external.ordinal()) external = level;
        }
        var result = new GhostSuppressionState(external, internal, pressure);
        cache.put(module, new Cached(strength, result));
        return result;
    }
    /** 返回不可修改的已成立关系快照；诊断不会更新来源、失效缓存或重算媒介。 */
    public Map<UUID, Relation> snapshot() { return Map.copyOf(relations); }
    /** 无关系时可由索引移除整个账本。 */
    public boolean isEmpty() { return relations.isEmpty(); }
    /** 用于验证稳定关系不会反复更新，不代表游戏时间。 */
    public long revision() { return revision; }
}
