package com.qidate.qisplan2.ghost.ability;

import com.qidate.qisplan2.ghost.GhostAbilityContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public interface PossessedGhostAbility {

    /**
     * 这个能力对应的鬼。
     */
    ResourceLocation id();

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

    /**
     * 玩家刚刚驾驭这只鬼。
     */
    default void onPossess(
            GhostAbilityContext context
    ) { }

    /**
     * 玩家每 tick 驾驭这只鬼时调用。
     */
    default void tick(
            GhostAbilityContext context
    ) { }

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

    /**
     * 玩家解除驾驭时调用。
     */
    default void onRelease(
            GhostAbilityContext context
    ) { }
}