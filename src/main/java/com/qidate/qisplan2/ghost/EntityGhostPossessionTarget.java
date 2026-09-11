package com.qidate.qisplan2.ghost;

import com.qidate.qisplan2.core.ModItems;
import com.qidate.qisplan2.death.SupernaturalEntity;
import com.qidate.qisplan2.entity.AbstractGhostEntity;
import com.qidate.qisplan2.network.QisNetwork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;

public final class EntityGhostPossessionTarget
        implements GhostPossessionTarget {

    private final Entity ghost;

    public EntityGhostPossessionTarget(
            Entity ghost
    ) {
        this.ghost = ghost;
    }

    public Entity ghost() {
        return ghost;
    }

    @Override
    public boolean onSuccess(
            ServerPlayer player
    ) {

        /*
         * 获取实体类型。
         */
        var typeKey =
                BuiltInRegistries.ENTITY_TYPE.getKey(
                        ghost.getType()
                );

        if (typeKey == null) {
            return false;
        }

        boolean possessed =
                PossessionHandler.possess(
                        player,
                        typeKey
                );

        if (!possessed) {
            return false;
        }

        /*
         * ====================================================
         * 棺材钉
         * ====================================================
         */

        if (ghost instanceof AbstractGhostEntity abstractGhost
                && abstractGhost.isCoffinNailed()) {

            abstractGhost.setCoffinNailed(
                    false
            );

            ItemStack nail =
                    new ItemStack(
                            ModItems.COFFIN_NAIL.get()
                    );

            if (!player.isCreative()) {

                if (!player.getInventory().add(
                        nail
                )) {

                    player.drop(
                            nail,
                            false
                    );
                }
            }
        }

        /*
         * 鬼消失。
         */
        ghost.discard();

        return true;
    }


    @Override
    public void onFailure(
            ServerPlayer player
    ) {

        if (ghost instanceof SupernaturalEntity supernatural) {

            supernatural.clearSupernaturalStun();
        }
    }
}