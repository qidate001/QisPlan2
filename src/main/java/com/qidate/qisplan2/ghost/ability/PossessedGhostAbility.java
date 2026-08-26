package com.qidate.qisplan2.ghost.ability;

import com.qidate.qisplan2.ghost.GhostAbilityContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public interface PossessedGhostAbility {

    /**
     * 这个能力对应的鬼。
     */
    ResourceLocation id();


    /*
     * ============================================================
     * 厉鬼强度
     * ============================================================
     */

    /**
     * 初始本质强度。
     *
     * 玩家刚刚驾驭这只鬼时，
     * intrinsicStrength 会使用这个值。
     *
     * 例如：
     *
     * 10.0
     * 20.0
     * 100.0
     */
    default double initialIntrinsicStrength() {

        return 1.0D;
    }


    /**
     * 复苏值为 0 时，
     * 这只鬼能够发挥自身本质强度的多少。
     *
     * 默认：
     *
     * 1 / 3
     */
    default double minimumStrengthRatio() {

        return 1.0D / 3.0D;
    }


    /*
     * ============================================================
     * 基础信息
     * ============================================================
     */

    /**
     * 玩家看到的鬼名称。
     *
     * 默认使用语言文件。
     */
    default Component displayName() {

        return Component.translatable(
                "ghost."
                        + id().getNamespace()
                        + "."
                        + id().getPath()
        );
    }


    /*
     * ============================================================
     * 驾驭生命周期
     * ============================================================
     */

    /**
     * 玩家刚刚驾驭这只鬼。
     */
    default void onPossess(
            GhostAbilityContext context
    ) {
    }


    /**
     * 玩家每 tick 驾驭这只鬼时调用。
     */
    default void tick(
            GhostAbilityContext context
    ) {
    }


    /*
     * ============================================================
     * 主动能力
     * ============================================================
     */

    /**
     * 使用主动能力。
     */
    default boolean use(
            GhostAbilityContext context
    ) {

        return false;
    }


    /**
     * 对方块使用主动能力。
     */
    default boolean useOnBlock(
            GhostAbilityContext context,
            net.minecraft.core.BlockPos pos
    ) {

        return false;
    }


    /*
     * ============================================================
     * 解除驾驭
     * ============================================================
     */

    /**
     * 玩家解除驾驭时调用。
     */
    default void onRelease(
            GhostAbilityContext context
    ) {
    }
}