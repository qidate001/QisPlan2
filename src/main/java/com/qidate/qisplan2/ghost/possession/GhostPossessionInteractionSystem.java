package com.qidate.qisplan2.ghost.possession;

import com.qidate.qisplan2.core.ModItems;
import com.qidate.qisplan2.entity.AbstractGhostEntity;
import com.qidate.qisplan2.ghost.possession.manager.GhostPossessionManager;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import com.qidate.qisplan2.ghost.possession.target.EntityGhostPossessionTarget;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class GhostPossessionInteractionSystem {

    private GhostPossessionInteractionSystem() {}

    public static InteractionResult handle(
            AbstractGhostEntity ghost,
            Player player,
            InteractionHand hand
    ) {

        /*
         * 只允许主手。
         */
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        /*
         * Shift：棺材钉交互。
         */
        if (player.isShiftKeyDown()) {

            if (ghost.isCoffinNailed()) {

                if (!ghost.level().isClientSide()
                        && player instanceof ServerPlayer serverPlayer) {

                    ghost.setCoffinNailed(false);
                    ghost.setSupernaturalStunTicks(0);

                    ItemStack nail =
                            new ItemStack(ModItems.COFFIN_NAIL.get());

                    if (!serverPlayer.isCreative()) {

                        if (!serverPlayer.getInventory().add(nail)) {
                            serverPlayer.drop(nail, false);
                        }
                    }

                    return InteractionResult.CONSUME;
                }

                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        }

        /*
         * 必须空手。
         */
        if (!player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }

        /*
         * 必须死机。
         */
        if (!ghost.isSupernaturallyStunned()) {
            return InteractionResult.PASS;
        }

        /*
         * 服务端开始驾驭。
         */
        if (!ghost.level().isClientSide()
                && player instanceof ServerPlayer serverPlayer) {

            if (PossessionHandler.hasGhost(
                    serverPlayer,
                    ghost.getGhostId()
            )) {

                serverPlayer.displayClientMessage(
                        Component.literal("已经驾驭了这只鬼。"),
                        true
                );

                return InteractionResult.CONSUME;
            }

            boolean started =
                    GhostPossessionManager.start(
                            serverPlayer,
                            new EntityGhostPossessionTarget(ghost)
                    );

            if (started) {
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.sidedSuccess(
                ghost.level().isClientSide()
        );
    }
}