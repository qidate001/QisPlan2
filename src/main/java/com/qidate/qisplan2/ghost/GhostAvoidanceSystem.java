package com.qidate.qisplan2.ghost;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.module.host.GhostEnvironmentalEffects;
import com.qidate.qisplan2.ghost.module.environment.GhostEnvironmentalEffect;
import java.util.Comparator;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 厉鬼共用的目标策略。查询环境能力的输出，不区分能力由哪一种载体提供。
 * 白烛优先于红烛；不满足暴露媒介时不提供效果。
 */
public final class GhostAvoidanceSystem {
    public static final double ATTRACTION_RANGE = 32;

    /**
     * 禁止外部实例化此工具类；功能统一通过静态入口调用。
     */
    private GhostAvoidanceSystem() {}

    /**
     * 根据环境屏蔽效果判断是否允许探测玩家。
     */
    public static boolean canDetect(ModularGhostEntity ghost, Player player) { return !hasWhiteGhostCandle(player); }

    /**
     * 根据环境屏蔽效果判断是否允许袭击玩家。
     */
    public static boolean canAttack(ModularGhostEntity ghost, Player player) { return !hasWhiteGhostCandle(player); }

    /**
     * 计算目标优先级：白烛屏蔽优先排除目标，红烛吸引提高目标优先级。
     */
    public static int getTargetPriority(ModularGhostEntity ghost, Player player) {
        return hasWhiteGhostCandle(player) ? Integer.MIN_VALUE : hasRedGhostCandle(player) ? 100 : 0;
    }

    /**
     * 查询目标是否受到屏蔽环境效果影响，不判断具体白烛物品类型。
     */
    public static boolean hasWhiteGhostCandle(LivingEntity player) { return GhostEnvironmentalEffects.on(player, GhostEnvironmentalEffect.CONCEALMENT); }

    /**
     * 查询目标是否受到吸引环境效果影响，不判断具体红烛物品类型。
     */
    public static boolean hasRedGhostCandle(LivingEntity player) { return GhostEnvironmentalEffects.on(player, GhostEnvironmentalEffect.ATTRACTION); }

    /** 使用方可叠加自己的追踪媒介，红烛不会绕过墙体、维度或模块能力前提。 */
    public static LivingEntity preferredTarget(LivingEntity ghost, double range, Predicate<LivingEntity> eligible) {
        return ghost.level().getEntitiesOfClass(LivingEntity.class, ghost.getBoundingBox().inflate(range),
                p -> p.isAlive() && !(p instanceof Player player && (player.isCreative() || player.isSpectator())) && p != ghost
                        && ghost.distanceToSqr(p) <= range * range
                        && hasRedGhostCandle(p) && !hasWhiteGhostCandle(p) && eligible.test(p))
                .stream().min(Comparator.comparingDouble(ghost::distanceToSqr)).orElse(null);
    }
}
