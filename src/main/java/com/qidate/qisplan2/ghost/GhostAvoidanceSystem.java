package com.qidate.qisplan2.ghost;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.module.implementation.candle.GhostCandleModule;
import com.qidate.qisplan2.ghost.module.implementation.candle.RedGhostCandleModule;
import com.qidate.qisplan2.ghost.module.implementation.candle.WhiteGhostCandleModule;
import java.util.Comparator;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 厉鬼共用的目标策略。效果来自手持物品上的模块，不依赖物品 Java 类型。
 * 白烛优先于红烛；背包中点燃的鬼烛继续燃烧，但不提供手持效果。
 */
public final class GhostAvoidanceSystem {
    public static final double ATTRACTION_RANGE = 32;
    private GhostAvoidanceSystem() {}
    public static boolean canDetect(ModularGhostEntity ghost, Player player) { return !hasWhiteGhostCandle(player); }
    public static boolean canAttack(ModularGhostEntity ghost, Player player) { return !hasWhiteGhostCandle(player); }
    public static int getTargetPriority(ModularGhostEntity ghost, Player player) {
        return hasWhiteGhostCandle(player) ? Integer.MIN_VALUE : hasRedGhostCandle(player) ? 100 : 0;
    }
    public static boolean hasWhiteGhostCandle(Player player) { return holdsLit(player, WhiteGhostCandleModule.ID); }
    public static boolean hasRedGhostCandle(Player player) { return holdsLit(player, RedGhostCandleModule.ID); }
    private static boolean holdsLit(Player player, ResourceLocation id) {
        return GhostCandleModule.isLit(player.getMainHandItem(), id)
                || GhostCandleModule.isLit(player.getOffhandItem(), id);
    }

    /** 使用方可叠加自己的追踪媒介，红烛不会绕过墙体、维度或模块能力前提。 */
    public static Player preferredTarget(LivingEntity ghost, double range, Predicate<Player> eligible) {
        return ghost.level().getEntitiesOfClass(Player.class, ghost.getBoundingBox().inflate(range),
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator() && p != ghost
                        && ghost.distanceToSqr(p) <= range * range
                        && hasRedGhostCandle(p) && !hasWhiteGhostCandle(p) && eligible.test(p))
                .stream().min(Comparator.comparingDouble(ghost::distanceToSqr)).orElse(null);
    }
}
