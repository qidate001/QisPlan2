package com.qidate.qisplan2.item.candle;

import com.qidate.qisplan2.ghost.GhostAvoidanceSystem;
import net.minecraft.server.level.ServerPlayer;

/** 旧接口兼容入口，判定统一委托给手持鬼烛模块策略。 */
public final class WhiteGhostCandleSystem {
    private WhiteGhostCandleSystem() {}
    public static double absorbAttack(ServerPlayer player, double intensity) {
        return GhostAvoidanceSystem.hasWhiteGhostCandle(player) ? 0 : Math.max(0, intensity);
    }
}
