package com.qidate.qisplan2.ghost.ability;

import com.qidate.qisplan2.ghost.GhostAbilityContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public interface PossessedGhostAbility {

    /**
     * 这个能力对应的鬼。
     */
    ResourceLocation id();

    /**
     * 玩家刚刚驾驭这只鬼。
     */
    default void onPossess(
            GhostAbilityContext context
    ) {
    }

    /**
     * 每 tick 调用。
     */
    default void tick(
            GhostAbilityContext context
    ) {
    }

    /**
     * 对 LivingEntity 使用主动能力。
     */
    default boolean use(
            GhostAbilityContext context
    ) {
        return false;
    }

    /**
     * 对方块使用主动能力。
     *
     * 例如：
     * Shift + 右键门。
     */
    default boolean useOnBlock(
            GhostAbilityContext context,
            BlockPos pos
    ) {
        return false;
    }

    /**
     * 玩家解除驾驭时调用。
     */
    default void onRelease(
            GhostAbilityContext context
    ) {
    }
}