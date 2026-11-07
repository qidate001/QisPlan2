package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

/**
 * 模块实例的外来灵异攻击状态。
 * 通用攻击状态放在 customData 的保留子节点，避免与具体厉鬼的专属字段冲突。
 * 与裁决 deadlocked 分开保存，清除普通死机不能解除永久死机或裁决僵持。
 */
public final class GhostModuleCombatState {
    private static final String KEY = "qisplan2:combat";
    private GhostModuleCombatState() {}
    public static int ticks(GhostModuleData.Entry entry) {
        return Math.max(0, entry.customData().getCompound(KEY).getInt("stunTicks"));
    }
    public static boolean permanent(GhostModuleData.Entry entry) {
        return entry.customData().getCompound(KEY).getBoolean("permanentStun");
    }
    public static boolean available(GhostModuleData.Entry entry) {
        return !entry.sleeping() && !entry.deadlocked() && !permanent(entry) && ticks(entry) == 0;
    }
    public static void set(GhostModuleHost host, GhostModuleData.Entry entry, int ticks, boolean permanent) {
        if (!(host instanceof GhostModuleCustomDataModifier modifier)) return;
        CompoundTag data = modifier.getModuleCustomData(entry.id());
        CompoundTag combat = data.getCompound(KEY).copy();
        combat.putInt("stunTicks", permanent ? 0 : Math.max(0, ticks));
        combat.putBoolean("permanentStun", permanent);
        data.put(KEY, combat);
        modifier.setModuleCustomData(entry.id(), data);
    }
    public static void tick(GhostModuleHost host) {
        for (var entry : List.copyOf(host.getModules())) {
            if (ticks(entry) > 0 && !permanent(entry)) set(host, entry, ticks(entry) - 1, false);
        }
    }
}
