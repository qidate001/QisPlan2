package com.qidate.qisplan2.ghost;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class GhostAbilityContext {

    private final ServerPlayer player;

    private final ResourceLocation ghost;

    private PossessedGhostState state;

    private final LivingEntity target;

    public GhostAbilityContext(
            ServerPlayer player,
            ResourceLocation ghost,
            PossessedGhostState state
    ) {
        this(
                player,
                ghost,
                state,
                null
        );
    }

    public GhostAbilityContext(
            ServerPlayer player,
            ResourceLocation ghost,
            PossessedGhostState state,
            LivingEntity target
    ) {
        this.player = player;
        this.ghost = ghost;
        this.state = state;
        this.target = target;
    }

    public ServerPlayer player() {
        return player;
    }

    public ResourceLocation ghost() {
        return ghost;
    }

    public PossessedGhostState state() {
        return state;
    }

    public LivingEntity target() {
        return target;
    }

    /**
     * 修改当前这只鬼在本次操作中的状态。
     */
    public void setState(
            PossessedGhostState state
    ) {
        this.state = state;
    }
}