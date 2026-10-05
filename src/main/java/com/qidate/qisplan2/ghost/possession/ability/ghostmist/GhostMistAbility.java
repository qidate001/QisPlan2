package com.qidate.qisplan2.ghost.possession.ability.ghostmist;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.domain.type.mist.GhostMistDomainController;
import com.qidate.qisplan2.ghost.possession.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.possession.ability.PossessedGhostAbility;
import net.minecraft.resources.ResourceLocation;

/**
 * ========================================
 * 鬼雾驾驭能力
 * ========================================
 *
 * 当前阶段只负责：
 *
 * - 注册鬼雾 Ability
 * - 驱动鬼雾鬼域生命周期
 *
 * 鬼雾具体的驭鬼数据，
 * 后续在驭鬼数据结构重构时统一接入。
 */
public final class GhostMistAbility
        implements PossessedGhostAbility {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "ghost_mist"
            );

    @Override
    public ResourceLocation id() {
        return ID;
    }

    /**
     * 基础强度。
     */
    @Override
    public double initialStrength() {
        return 100.0D;
    }

    @Override
    public double minimumStrengthRatio() {
        return 0.50D;
    }

    /**
     * 每 tick 驱动鬼雾鬼域。
     */
    @Override
    public void tick(
            GhostAbilityContext context
    ) {

        GhostMistDomainController.tick(
                context
        );
    }

    /**
     * 解除驾驭时关闭鬼雾鬼域。
     */
    @Override
    public void onRelease(
            GhostAbilityContext context
    ) {

        GhostMistDomainController.close(
                context.player()
        );
    }
}