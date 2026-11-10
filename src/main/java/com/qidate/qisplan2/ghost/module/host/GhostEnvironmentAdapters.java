package com.qidate.qisplan2.ghost.module.host;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.environment.*;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import com.qidate.qisplan2.ghost.module.sound.GhostSoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 物理载体差异只允许出现在适配层，模块拿到的接口无法反向获得这些对象。 */
public final class GhostEnvironmentAdapters {
    /**
     * 禁止外部实例化此工具类；功能统一通过静态入口调用。
     */
    private GhostEnvironmentAdapters() {}

    /**
     * 仅在适配层解析承担世界位置与动作的生命实体。
     * 物品使用其携带者；没有生命实体来源时返回 null，不向模块暴露此对象。
     */
    public static LivingEntity bearer(GhostModuleHost host) {
        if (host instanceof LivingEntity entity) return entity;
        if (host instanceof PlayerGhostModuleHost player) return player.player();
        if (host instanceof GhostItemModuleHost item) return item.carrier();
        return null;
    }

    /**
     * 为指定载体与模块创建环境适配接口。
     * 接口实时读取载体状态；物品手持判定、燃烧压力、耐久和燃料更新均留在此边界。
     */
    public static GhostEnvironment bind(GhostModuleHost host, ResourceLocation moduleId) {
        return new GhostEnvironment() {
            /**
             * 判断当前模块是否满足暴露在世界中的条件。
             * 具体的手持、存活或方块有效性由载体适配器解释。
             */
            public boolean exposed() {
                if (host instanceof GhostItemModuleHost item) {
                    var player = item.carrier();
                    return player != null && player.isAlive() && !item.stack().isEmpty()
                            && (player.getMainHandItem() == item.stack() || player.getOffhandItem() == item.stack());
                }
                var entity = bearer(host);
                if (entity != null) return entity.isAlive() && !entity.isRemoved();
                return host instanceof BlockEntity block && !block.isRemoved() && block.hasLevel();
            }

            /**
             * 获取当前环境造成的基础燃烧压力，单位为每 tick 的消耗量。
             * 此值不包含声音屏蔽额外消耗。
             */
            public double burnPressure() {
                var entity = bearer(host);
                if (entity == null) return 0.05D;
                double pressure = 0.05D;
                for (var ghost : entity.level().getEntitiesOfClass(ModularGhostEntity.class,
                        entity.getBoundingBox().inflate(16), g -> g != entity && g.isAlive())) {
                    double proximity = 1 + Math.max(0, 1 - entity.distanceTo(ghost) / 16D) * 4;
                    pressure = Math.max(pressure, 0.05D * proximity * (1 + Math.max(0, ghost.getSupernaturalStrength()) / 10));
                }
                return pressure;
            }

            /**
             * 读取被屏蔽的灵异声音强度，供模块计算额外燃烧代价。
             * 没有可计入的声音压力时返回零。
             */
            public double blockedSoundIntensity() {
                return exposed() && bearer(host) instanceof ServerPlayer player
                        ? GhostSoundManager.getBlockedSoundIntensity(player) : 0;
            }

            /**
             * 发布本次能力产生的环境效果。
             * 世界系统读取效果时仍需检查模块状态和暴露条件。
             */
            public void emit(GhostEnvironmentalEffect effect) {
                if (!(host instanceof GhostModuleCustomDataModifier data)) return;
                var custom = data.getModuleCustomData(moduleId);
                custom.putString("activeEffect", effect.name());
                data.setModuleCustomData(moduleId, custom);
            }

            /**
             * 扣除本次能力消耗的燃料。
             * 适配器负责把连续消耗量转换为耐久或持久燃料，模块不处理物品存储细节。
             */
            public void consume(double amount) {
                if (!Double.isFinite(amount) || amount <= 0 || !(host instanceof GhostModuleCustomDataModifier data)) return;
                if (host instanceof GhostItemModuleHost item) {
                    if (!item.stack().isDamageableItem()) {
                        consumeStoredFuel(data, moduleId, amount);
                        return;
                    }
                    // 用累计小数保存精确消耗，不把整数物品耐久暴露给模块。
                    var custom = data.getModuleCustomData(moduleId);
                    double accumulated = custom.getDouble("burnRemainder") + amount;
                    int whole = (int)Math.min(Integer.MAX_VALUE, Math.floor(accumulated));
                    custom.putDouble("burnRemainder", accumulated - whole);
                    data.setModuleCustomData(moduleId, custom);
                    if (whole == 0) return;
                    if ((long)item.stack().getDamageValue() + whole >= item.stack().getMaxDamage()) {
                        item.stack().shrink(1);
                    } else item.stack().setDamageValue(item.stack().getDamageValue() + whole);
                } else consumeStoredFuel(data, moduleId, amount);
            }
        };
    }

    /**
     * 从模块 customData 扣除连续燃料；未初始化时使用默认燃料量。
     * 耗尽时熄灭并清除环境效果，保留其他模块专属字段。
     */
    private static void consumeStoredFuel(GhostModuleCustomDataModifier data, ResourceLocation id, double amount) {
        var custom = data.getModuleCustomData(id);
        double remaining = custom.contains("remainingFuel") ? custom.getDouble("remainingFuel") : 1200;
        remaining = Math.max(0, remaining - amount);
        custom.putDouble("remainingFuel", remaining);
        if (remaining == 0) {
            custom.putBoolean("lit", false);
            custom.remove("activeEffect");
        }
        data.setModuleCustomData(id, custom);
    }
}
