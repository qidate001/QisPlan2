package com.qidate.qisplan2.ghost;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public final class ItemGhostPossessionTarget
        implements GhostPossessionTarget {

    private final ItemStack stack;

    private final ResourceLocation abilityId;

    private final Consumer<ServerPlayer> failureAction;

    public ItemGhostPossessionTarget(
            ItemStack stack,
            ResourceLocation abilityId,
            Consumer<ServerPlayer> failureAction
    ) {
        this.stack = stack;
        this.abilityId = abilityId;
        this.failureAction = failureAction;
    }

    @Override
    public boolean onSuccess(
            ServerPlayer player
    ) {

        boolean possessed =
                PossessionHandler.possess(
                        player,
                        abilityId
                );

        if (!possessed) {
            return false;
        }

        stack.shrink(1);

        return true;
    }

    @Override
    public void onFailure(
            ServerPlayer player
    ) {

        failureAction.accept(
                player
        );
    }
}