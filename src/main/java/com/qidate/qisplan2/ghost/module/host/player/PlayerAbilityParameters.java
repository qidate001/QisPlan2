package com.qidate.qisplan2.ghost.module.host.player;

import com.qidate.qisplan2.ghost.module.ability.GhostAbilityParameters;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

/** 玩家主动调用输入；目标与点击位置是参数，不是已经发生的语义事件。 */
public record PlayerAbilityParameters(LivingEntity target, BlockPos block) implements GhostAbilityParameters {
    public PlayerAbilityParameters {
        if (block != null) block = block.immutable();
    }
}
