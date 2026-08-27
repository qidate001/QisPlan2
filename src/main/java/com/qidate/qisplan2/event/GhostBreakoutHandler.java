package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.entity.AbstractGhostEntity;
import com.qidate.qisplan2.ghost.PossessedGhostState;
import com.qidate.qisplan2.ghost.PossessionHandler;

import com.qidate.qisplan2.ghost.ability.GhostAbilityRegistry;
import com.qidate.qisplan2.ghost.ability.PossessedGhostAbility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.Map;

@EventBusSubscriber(
        modid = QisPlan2.MODID
)
public final class GhostBreakoutHandler {

    private GhostBreakoutHandler() {
    }

    /**
     * 复苏值与死机时间换算：
     *
     * 0%   -> 50 秒
     * 80%  -> 10 秒
     * 100% -> 0 秒
     */
    private static final long MAX_STUN_TICKS =
            1000L;



    @SubscribeEvent
    public static void onLivingDeath(
            LivingDeathEvent event
    ) {

        LivingEntity entity =
                event.getEntity();

        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        breakout(
                player
        );
    }

    /**
     * 让玩家当前驾驭的所有厉鬼立即破体。
     *
     * 用于：
     *
     * 1. 正常死亡事件
     * 2. 厉鬼复苏达到 100% 的强制死亡
     */
    public static void breakout(
            ServerPlayer player
    ) {

        if (!(player.level()
                instanceof ServerLevel level)) {

            return;
        }

        Map<
                ResourceLocation,
                PossessedGhostState
                > states =
                PossessionHandler.getAllStates(
                        player
                );

        if (states.isEmpty()) {
            return;
        }

        /*
         * ========================================================
         * 记录死亡位置。
         * ========================================================
         */

        double x =
                player.getX();

        double y =
                player.getY();

        double z =
                player.getZ();

        float yRot =
                player.getYRot();

        float xRot =
                player.getXRot();

        /*
         * ========================================================
         * 逐只破体。
         * ========================================================
         */

        for (Map.Entry<
                ResourceLocation,
                PossessedGhostState
                > entry :
                states.entrySet()) {

            spawnBrokenGhost(
                    level,
                    entry.getKey(),
                    entry.getValue(),
                    x,
                    y,
                    z,
                    yRot,
                    xRot
            );
        }

        /*
         * ========================================================
         * 清空玩家驾驭状态。
         * ========================================================
         */

        PossessionHandler.clearAll(
                player
        );

        QisPlan2.LOGGER.info(
                "[QisPlan2] 玩家 {} 触发厉鬼破体：{} 只",
                player.getName().getString(),
                states.size()
        );
    }


    /**
     * 从玩家身体中生成一只普通厉鬼。
     */
    private static void spawnBrokenGhost(
            ServerLevel level,
            ResourceLocation ghostId,
            PossessedGhostState state,
            double x,
            double y,
            double z,
            float yRot,
            float xRot
    ) {

        PossessedGhostAbility ability =
                GhostAbilityRegistry.get(
                        ghostId
                );

        if (ability == null) {
            QisPlan2.LOGGER.warn(
                    "[QisPlan2] 无法让厉鬼破体：找不到 Ability {}",
                    ghostId
            );
            return;
        }

        EntityType<? extends AbstractGhostEntity>
                entityType =
                ability.entityType();

        if (entityType == null) {
            QisPlan2.LOGGER.warn(
                    "[QisPlan2] 无法让厉鬼破体：Ability {} 没有对应实体类型",
                    ghostId
            );
            return;
        }

        AbstractGhostEntity ghost =
                entityType.create(level);

        /*
         * ========================================================
         * 生成位置
         * ========================================================
         */

        ghost.moveTo(
                x,
                y,
                z,
                yRot,
                xRot
        );


        /*
         * ========================================================
         * 计算死机时间。
         *
         * revival:
         *
         * 0.0 -> 1000 tick
         * 0.8 -> 200 tick
         * 1.0 -> 0 tick
         * ========================================================
         */

        long stunTicks =
                Math.round(
                        (1.0D - state.revival())
                                * MAX_STUN_TICKS
                );

        stunTicks =
                Math.max(
                        0L,
                        stunTicks
                );

        /*
         * ========================================================
         * 设置普通死机。
         * ========================================================
         */

        if (stunTicks > 0L) {

            ghost.setSupernaturalStunTicks(
                    (int) stunTicks
            );
        }

        /*
         * ========================================================
         * 加入世界。
         * ========================================================
         */

        level.addFreshEntity(
                ghost
        );

        QisPlan2.LOGGER.info(
                "[QisPlan2] 厉鬼破体：ghost={}，复苏={}%，死机={} tick ({} 秒)",
                ghostId,
                state.revival() * 100.0D,
                stunTicks,
                stunTicks / 20.0D
        );
    }


    /**
     * Ability ID → 实体类型。
     *
     * 目前先覆盖已经做好的几个鬼。
     */
    private static EntityType<? extends AbstractGhostEntity>
    getEntityType(
            ResourceLocation ghostId
    ) {

        if (ghostId.equals(
                "qisplan2:opening_ghost"
        )) {

            return QisPlan2.OPENING_GHOST.get();
        }

        if (ghostId.equals(
                "qisplan2:closing_ghost"
        )) {

            return QisPlan2.CLOSING_GHOST.get();
        }

        if (ghostId.equals(
                "qisplan2:knocking_ghost"
        )) {

            return QisPlan2.KNOCKING_GHOST.get();
        }

        if (ghostId.equals(
                "qisplan2:night_wanderer"
        )) {

            return QisPlan2.NIGHT_WANDERER.get();
        }

        return null;
    }
}