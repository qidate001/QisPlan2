package com.qidate.qisplan2.ghost.module.implementation.nightwanderer;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import net.minecraft.nbt.CompoundTag;

/**
 * 夜游鬼模块专属状态的访问入口。所有值都保存在该模块实例的 customData 内，
 * 不在宿主实体或全局模块定义中另存一份；因此存档和模块转移使用同一份状态。
 * 缺失字段按零读取，兼容新创建的空 customData。
 */
public final class NightWandererState {
    public static final String KILLS = "killCount";
    public static final String ATTACK_COOLDOWN = "attackCooldown";
    public static final String NO_TARGET = "huntNoTargetTicks";
    public static final String TELEPORT_COOLDOWN = "huntTeleportCooldown";
    public static final String TARGET = "target";
    private NightWandererState() {}

    /** 新实例补齐计数字段；已有存档值保留，未知扩展字段也不删除。 */
    public static void initialize(ModularGhostEntity host) {
        CompoundTag data = host.getModuleCustomData(NightWandererModule.ID);
        for (String key : new String[]{KILLS, ATTACK_COOLDOWN, NO_TARGET, TELEPORT_COOLDOWN}) {
            if (!data.contains(key)) data.putInt(key, 0);
        }
        host.setModuleCustomData(NightWandererModule.ID, data);
    }

    public static int get(ModularGhostEntity host, String key) {
        return Math.max(0, host.getModuleCustomData(NightWandererModule.ID).getInt(key));
    }
    public static void set(ModularGhostEntity host, String key, int value) {
        CompoundTag data = host.getModuleCustomData(NightWandererModule.ID);
        data.putInt(key, Math.max(0, value));
        host.setModuleCustomData(NightWandererModule.ID, data);
    }
    public static void decrement(ModularGhostEntity host, String key) {
        int value = get(host, key);
        if (value > 0) set(host, key, value - 1);
    }
    /** 击杀数保存到 customData，强度只更新模块通用属性，避免两份强度相互覆盖。 */
    public static void addKills(ModularGhostEntity host, int amount) {
        if (amount <= 0 || !host.hasGhostModule(NightWandererModule.ID)) return;
        int old = get(host, KILLS);
        int next = (int)Math.min(Integer.MAX_VALUE, (long)old + amount);
        set(host, KILLS, next);
        var context = new GhostModuleContext(host, NightWandererModule.ID);
        host.setIntensity(NightWandererModule.ID, context.intensity() + (next - old) * 0.1D);
    }
    public static void startAttackCooldown(ModularGhostEntity host) {
        set(host, ATTACK_COOLDOWN, Math.max(1, 200 - get(host, KILLS) / 5));
    }
}
