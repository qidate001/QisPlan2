package com.qidate.qisplan2.ghost.module.implementation.mist;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import net.minecraft.nbt.CompoundTag;

/**
 * 鬼雾的鬼域成长数据。模块 customData 是持久化来源，
 * 运行时鬼域 UUID、中心、维度由宿主所在世界重建，不复制到其他宿主。
 */
public final class GhostMistState {
    private GhostMistState() {}
    public static CompoundTag read(ModularGhostEntity host) {
        var data = host.getModuleCustomData(GhostMistModule.ID);
        data.putDouble("domainStrength", value(data, "domainStrength", 5));
        data.putDouble("domainRadius", value(data, "domainRadius", 80));
        data.putInt("domainLayer", data.contains("domainLayer") ? Math.clamp(data.getInt("domainLayer"), 1, 10) : 1);
        return data;
    }
    private static double value(CompoundTag data, String key, double fallback) {
        double value = data.contains(key) ? data.getDouble(key) : fallback;
        return Double.isFinite(value) && value >= 0 ? value : fallback;
    }
    public static void initialize(ModularGhostEntity host) {
        host.setModuleCustomData(GhostMistModule.ID, read(host));
    }
    public static void save(ModularGhostEntity host, double strength, double radius, int layer) {
        var data = read(host);
        data.putDouble("domainStrength", strength);
        data.putDouble("domainRadius", radius);
        data.putInt("domainLayer", layer);
        host.setModuleCustomData(GhostMistModule.ID, data);
    }
}
