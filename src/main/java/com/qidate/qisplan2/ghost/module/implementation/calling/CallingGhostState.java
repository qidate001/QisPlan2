package com.qidate.qisplan2.ghost.module.implementation.calling;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** 每只喊人鬼的全部专属数据保存在模块 customData，读取副本后显式写回。 */
final class CallingGhostState {
    static CompoundTag read(ModularGhostEntity host) { return host.getModuleCustomData(CallingGhostModule.ID); }
    static void write(ModularGhostEntity host, CompoundTag data) {
        host.setModuleCustomData(CallingGhostModule.ID, data);
    }
    static void initialize(ModularGhostEntity host) {
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
    static void bind(ModularGhostEntity host, ServerPlayer player) {
        var data = read(host);
        data.putUUID("target", player.getUUID());
        data.putInt("callCount", 0);
        data.putInt("callCooldown", 100);
        data.putFloat("lastPlayerYRot", player.getYRot());
        data.putBoolean("hasLastPlayerRotation", true);
        write(host, data);
    }
    static void clear(ModularGhostEntity host) {
        var data = read(host);
        data.remove("target");
        data.putBoolean("hasLastPlayerRotation", false);
        write(host, data);
    }
    static void finishAttack(ModularGhostEntity host, ServerPlayer player) {
        if (!host.hasGhostModule(CallingGhostModule.ID)) return;
        var data = read(host);
        data.putUUID("recentlyAttacked", player.getUUID());
        data.putInt("attackTargetCooldown", 600);
        data.remove("target");
        data.putBoolean("hasLastPlayerRotation", false);
        write(host, data);
    }
    /** 目标避让计时不占用移动资源，即使让位给其他模块也继续推进。 */
    static void tickTargetCooldown(ModularGhostEntity host) {
        var data = read(host);
        int remaining = Math.max(0, data.getInt("attackTargetCooldown"));
        if (remaining == 0) return;
        data.putInt("attackTargetCooldown", remaining - 1);
        if (remaining == 1) data.remove("recentlyAttacked");
        write(host, data);
    }
}
