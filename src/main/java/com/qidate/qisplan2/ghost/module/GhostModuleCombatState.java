package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.host.GhostEnvironmentAdapters;
import com.qidate.qisplan2.ghost.curse.CurseManager;

import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.suppression.GhostSuppression;
import com.qidate.qisplan2.ghost.module.suppression.GhostSuppressionState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

/**
 * 通用模块的压制入睡、沉睡恢复与短时袭击状态。
 * 数据跟随模块迁移，逻辑只依赖宿主接口，不识别实体、玩家、方块或物品。
 * 沉睡深度使用剩余自然苏醒 tick 表示，不占用玩家原有的失控复苏值。
 */
public final class GhostModuleCombatState {
    public static final String KEY = "qisplan2:combat";
    private GhostModuleCombatState() {}

    /** 自主规则和行为只由模块沉睡及裁决死机决定，旧临时死机不再参与。 */
    public static boolean available(GhostModuleData.Entry entry) {
        return !entry.sleeping() && !entry.deadlocked();
    }
    /** 返回持久化沉睡深度，供命令和兼容视图展示。 */
    public static int sleepTicks(GhostModuleData.Entry entry) {
        if (!entry.sleeping()) return 0;
        var data = entry.customData().getCompound(KEY);
        return (int)Math.min(Integer.MAX_VALUE, Math.ceil(Math.max(0,
                data.contains("sleepTicks") ? data.getDouble("sleepTicks") : 1200)));
    }
    /** 基准 1.5 倍约五分钟；平方加速不设倍率上限，无解压制用独立的二十倍速度。 */
    public static double sleepRate(double pressure, double intensity) {
        if (pressure == Double.POSITIVE_INFINITY) return 20;
        double ratio = Math.max(1, pressure / Math.max(0.001, intensity) / 1.5);
        return Math.min(1e100, ratio * ratio);
    }
    /** 基础十分钟上限随倍率平方增长；无解压制最多累积二十四小时。 */
    public static double sleepCap(double pressure, double intensity) {
        return pressure == Double.POSITIVE_INFINITY ? 24D * 60 * 60 * 20 : 12000D * sleepRate(pressure, intensity);
    }
    /** 独立读取模块的深度积累声明；不能让无解压制的入睡加速倍率污染积累速度。 */
    private static double accumulationRate(ResourceLocation moduleId) {
        var module = GhostModuleRegistry.get(moduleId);
        double value = module == null ? 0.5D : module.suppressionSleepAccumulationRate();
        if (!Double.isFinite(value) || value < 0) throw new IllegalStateException("Invalid sleep accumulation rate: " + moduleId);
        return value;
    }
    /** 初始化沉睡，供命令、破体及旧存档迁移使用；不修改裁决死机。 */
    public static void sleep(GhostModuleHost host, GhostModuleData.Entry entry, double ticks) {
        if (!(host instanceof GhostModuleDataModifier state) || !(host instanceof GhostModuleCustomDataModifier data)) return;
        var custom = data.getModuleCustomData(entry.id());
        var combat = custom.getCompound(KEY).copy();
        combat.putDouble("sleepTicks", Math.max(0, ticks));
        combat.putDouble("pressureProgress", 0);
        combat.remove("stunTicks"); combat.remove("permanentStun");
        custom.put(KEY, combat); data.setModuleCustomData(entry.id(), custom);
        state.setSleeping(entry.id(), ticks > 0);
    }
    /** 显式的一秒普通压制；纯袭击入口不得调用。每次命中替换强度并刷新到期，不累加持续时间。 */
    public static void suppressForOneSecond(GhostModuleHost host, double strength) {
        if (!(host instanceof GhostModuleCustomDataModifier data) || !Double.isFinite(strength) || strength <= 0) return;
        for (var entry : List.copyOf(host.getModules())) {
            var custom = data.getModuleCustomData(entry.id());
            var combat = custom.getCompound(KEY).copy();
            combat.putDouble("attackStrength", strength);
            combat.putLong("attackUntil", host.moduleWorld(entry.id()).gameTime() + 20);
            custom.put(KEY, combat); data.setModuleCustomData(entry.id(), custom);
        }
    }
    /** 查询短时袭击权限；时间到即失效，不依赖下一轮清理 tick。 */
    public static GhostSuppressionState attackSuppression(GhostModuleHost host, ResourceLocation id) {
        var entry = host.getModules().stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);
        if (entry == null) return GhostSuppressionState.NONE;
        var data = entry.customData().getCompound(KEY);
        if (!data.contains("attackUntil") || data.getLong("attackUntil") <= host.moduleWorld(id).gameTime()) return GhostSuppressionState.NONE;
        double strength = data.getDouble("attackStrength");
        if (!Double.isFinite(strength) || strength <= 0) return GhostSuppressionState.NONE;
        return new GhostSuppressionState(new GhostSuppression(GhostSuppression.Kind.NORMAL, strength).level(entry.intensity()),
                GhostSuppression.Level.NONE, strength);
    }
    /**
     * 在宿主原有 ModuleTick 中推进，不扫描世界或重新遍历压制来源。
     * 同一游戏 tick 重复派发只结算一次；卸载时间不补算，模块迁移不继承旧宿主时钟。
     */
    public static void tick(GhostModuleHost host) {
        if (!(host instanceof GhostModuleDataModifier state) || !(host instanceof GhostModuleCustomDataModifier data)) return;
        for (var entry : List.copyOf(host.getModules())) {
            var custom = data.getModuleCustomData(entry.id());
            var combat = custom.getCompound(KEY).copy();
            boolean sleeping = entry.sleeping();
            // 旧状态只迁移一次：有限停滞变为沉睡，永久停滞统一进入模块裁决死机。
            if (combat.contains("stunTicks") || combat.contains("permanentStun")) {
                double legacy = Math.max(0, combat.getInt("stunTicks"));
                if (legacy > 0) { combat.putDouble("sleepTicks", Math.max(legacy, combat.getDouble("sleepTicks"))); sleeping = true; }
                if (combat.getBoolean("permanentStun")) state.setDeadlocked(entry.id(), true);
                combat.remove("stunTicks"); combat.remove("permanentStun");
            }
            var suppression = host.suppression(entry.id());
            boolean complete = suppression.external() == GhostSuppression.Level.COMPLETE
                    || suppression.internal() == GhostSuppression.Level.COMPLETE;
            if (!sleeping && !complete && combat.getDouble("pressureProgress") == 0 && !combat.contains("attackUntil")
                    && combat.equals(custom.getCompound(KEY))) continue;
            // 已到稳定上限且没有待清理短时关系时，不反复修改 NBT 或触发同步。
            if (sleeping && complete && !combat.contains("attackUntil")
                    && combat.getDouble("sleepTicks") == sleepCap(suppression.pressure(), entry.intensity())
                    && combat.equals(custom.getCompound(KEY))) continue;
            long now = host.moduleWorld(entry.id()).gameTime();
            if (combat.contains("lastTick") && combat.getLong("lastTick") == now) continue;
            combat.putLong("lastTick", now);
            if (combat.contains("attackUntil") && combat.getLong("attackUntil") <= now) {
                combat.remove("attackUntil"); combat.remove("attackStrength");
            }
            double rate = sleepRate(suppression.pressure(), entry.intensity());
            if (!sleeping) {
                double progress = complete ? combat.getDouble("pressureProgress") + rate : 0;
                if (progress >= 6000) {
                    sleeping = true; progress = 0; combat.putDouble("sleepTicks", 1200);
                }
                combat.putDouble("pressureProgress", progress);
            } else {
                double remaining = combat.contains("sleepTicks") ? combat.getDouble("sleepTicks") : 1200;
                if (complete) {
                    double cap = sleepCap(suppression.pressure(), entry.intensity());
                    // 较弱来源接管不会突然截断已经积累的深度；超过新上限时继续自然恢复。
                    remaining = remaining > cap ? Math.max(cap, remaining - 1) : Math.min(cap, remaining + accumulationRate(entry.id()));
                } else {
                    var body=GhostEnvironmentAdapters.bearer(host);
                    double recovery=body==null?1:CurseManager.revivalMultiplier(body);
                    remaining = Math.max(0, remaining - recovery);
                }
                combat.putDouble("sleepTicks", remaining);
                sleeping = remaining > 0;
            }
            custom.put(KEY, combat);
            if (!custom.equals(data.getModuleCustomData(entry.id()))) data.setModuleCustomData(entry.id(), custom);
            if (sleeping != entry.sleeping()) state.setSleeping(entry.id(), sleeping);
        }
    }
    /**
     * 沉睡规则被触犯只削减深度，不执行规则效果；每秒最多结算一次，避免接触采样数量放大速度。
     * 每次削减 5×sqrt(自身强度) 秒，强度越高越容易被惊醒；当前事件即使唤醒也不反击。
     */
    public static void disturbed(GhostModuleHost host, ResourceLocation id) {
        if (!(host instanceof GhostModuleCustomDataModifier data) || !(host instanceof GhostModuleDataModifier state)) return;
        var entry = host.getModules().stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);
        if (entry == null || !entry.sleeping()) return;
        var custom = data.getModuleCustomData(id);
        var combat = custom.getCompound(KEY).copy();
        long now = host.moduleWorld(id).gameTime();
        if (combat.contains("lastDisturbed") && now >= combat.getLong("lastDisturbed") && now - combat.getLong("lastDisturbed") < 20) return;
        double remaining = combat.contains("sleepTicks") ? combat.getDouble("sleepTicks") : 1200;
        remaining = Math.max(0, remaining - 100 * Math.sqrt(Math.max(1, entry.intensity())));
        combat.putDouble("sleepTicks", remaining); combat.putLong("lastDisturbed", now);
        custom.put(KEY, combat); data.setModuleCustomData(id, custom);
        if (remaining == 0) state.setSleeping(id, false);
    }
}
