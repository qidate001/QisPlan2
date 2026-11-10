package com.qidate.qisplan2.ghost.module.implementation.calling;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** 每只喊人鬼的全部专属数据保存在模块 customData，读取副本后显式写回。 */
public final class CallingGhostState {
    /**
     * 读取喊人鬼模块的 customData 副本；修改副本后需要显式写回。
     */
    public static CompoundTag read(ModularGhostEntity host) { return host.getModuleCustomData(CallingGhostModule.ID); }

    /**
     * 写回喊人鬼模块的专属数据，不覆盖模块通用强度或复苏值。
     */
    public static void write(ModularGhostEntity host, CompoundTag data) {
        host.setModuleCustomData(CallingGhostModule.ID, data);
    }

    /**
     * 补齐喊人鬼缺失的计时字段，并恢复存档前借用的物理状态。
     * 清除旧的转身观察基线，避免读档后误判一次转身。
     */
    public static void initialize(ModularGhostEntity host) {
        var data = read(host);
        for (String key : new String[]{"callCooldown", "callCount", "attackTargetCooldown"}) {
            if (!data.contains(key)) data.putInt(key, 0);
        }
        if (!data.contains("lastPlayerYRot")) data.putFloat("lastPlayerYRot", 0);
        if (!data.contains("hasLastPlayerRotation")) data.putBoolean("hasLastPlayerRotation", false);

        // 存档可能保存于悬浮跟随期间；先还原当时借用的物理状态，再由行为重新取得控制。
        if (data.getBoolean("physicsOwned")) {
            host.noPhysics = data.getBoolean("previousNoPhysics");
            host.setNoGravity(data.getBoolean("previousNoGravity"));
            data.putBoolean("physicsOwned", false);
        }
        data.putBoolean("hasLastPlayerRotation", false);
        write(host, data);
    }

    /**
     * 绑定新的追踪玩家，重置喊名次数和冷却，并建立转身观察基线。
     * 目标 UUID 与计时均保存到本模块 customData。
     */
    public static void bind(ModularGhostEntity host, ServerPlayer player) {
        var data = read(host);
        data.putUUID("target", player.getUUID());
        data.putInt("callCount", 0);
        data.putInt("callCooldown", 100);
        data.putFloat("lastPlayerYRot", player.getYRot());
        data.putBoolean("hasLastPlayerRotation", true);
        write(host, data);
    }

    /**
     * 清除喊人鬼当前目标与朝向观察基线，结束本轮跟随。
     */
    public static void clear(ModularGhostEntity host) {
        var data = read(host);
        data.remove("target");
        data.putBoolean("hasLastPlayerRotation", false);
        write(host, data);
    }

    /**
     * 记录刚袭击的目标并设置避让冷却，然后清除当前追踪关系。
     * 用于结束本轮遭遇，防止立即重复选择同一目标。
     */
    public static void finishAttack(ModularGhostEntity host, ServerPlayer player) {
        if (!host.hasGhostModule(CallingGhostModule.ID)) return;
        var data = read(host);
        data.putUUID("recentlyAttacked", player.getUUID());
        data.putInt("attackTargetCooldown", 600);
        data.remove("target");
        data.putBoolean("hasLastPlayerRotation", false);
        write(host, data);
    }

    /** 目标避让计时不占用移动资源，即使让位给其他模块也继续推进。 */
    public static void tickTargetCooldown(ModularGhostEntity host) {
        var data = read(host);
        int remaining = Math.max(0, data.getInt("attackTargetCooldown"));
        if (remaining == 0) return;
        data.putInt("attackTargetCooldown", remaining - 1);
        if (remaining == 1) data.remove("recentlyAttacked");
        write(host, data);
    }
}
