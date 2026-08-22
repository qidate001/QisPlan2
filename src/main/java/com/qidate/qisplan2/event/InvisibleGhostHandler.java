package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.entity.InvisibleGhost;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

@EventBusSubscriber(modid = QisPlan2.MODID)
public final class InvisibleGhostHandler {

    /**
     * 灵异攻击间隔。
     *
     * 20 tick = 1 秒。
     */
    private static final int ATTACK_INTERVAL = 20;

    /**
     * 视野检测范围。
     */
    private static final double DETECTION_RANGE = 64.0D;

    private static final double DETECTION_RANGE_SQR =
            DETECTION_RANGE * DETECTION_RANGE;

    /**
     * 视线判定阈值。
     *
     * 数值越低，允许的视野范围越宽。
     */
    private static final double LOOK_THRESHOLD = 0.85D;

    /**
     * 每秒受到的灵异攻击强度。
     */
    private static final double SUPERNATURAL_ATTACK_STRENGTH =
            5.0D;

    private InvisibleGhostHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        /*
         * 每秒检查一次。
         */
        if (event.getServer().getTickCount()
                % ATTACK_INTERVAL != 0) {

            return;
        }

        for (ServerLevel level :
                event.getServer().getAllLevels()) {

            for (ServerPlayer player :
                    level.players()) {

                checkPlayer(
                        level,
                        player
                );
            }
        }
    }

    /**
     * 检查玩家附近是否有被玩家看见的不可视之鬼。
     */
    private static void checkPlayer(
            ServerLevel level,
            ServerPlayer player
    ) {

        List<InvisibleGhost> ghosts =
                level.getEntitiesOfClass(
                        InvisibleGhost.class,
                        player.getBoundingBox()
                                .inflate(
                                        DETECTION_RANGE
                                )
                );

        for (InvisibleGhost ghost :
                ghosts) {

            /*
             * 鬼已经死亡。
             */
            if (!ghost.isAlive()) {
                continue;
            }

            /*
             * 鬼处于死机状态。
             */
            if (ghost.isSupernaturallyStunned()) {
                continue;
            }

            /*
             * 玩家没有看到它。
             */
            if (!canSee(
                    player,
                    ghost
            )) {
                continue;
            }

            /*
             * 看到不可视之鬼：
             *
             * 每秒受到一次 5 强度灵异攻击。
             */
            SupernaturalDeathHandler.tryKill(
                    player,
                    ModDamageTypes.invisibleGhost(
                            ghost
                    ),
                    SUPERNATURAL_ATTACK_STRENGTH
            );

            /*
             * 一个玩家每秒最多受到一次
             * 不可视之鬼的攻击。
             */
            return;
        }
    }

    /**
     * 判断玩家是否真正看见不可视之鬼。
     */
    private static boolean canSee(
            ServerPlayer player,
            InvisibleGhost ghost
    ) {

        /*
         * ========================================
         * 距离
         * ========================================
         */
        double distanceSqr =
                player.distanceToSqr(
                        ghost
                );

        if (distanceSqr
                > DETECTION_RANGE_SQR) {

            return false;
        }

        /*
         * ========================================
         * Minecraft 自带视线遮挡
         * ========================================
         *
         * 墙体等会阻挡。
         */
        if (!player.hasLineOfSight(
                ghost
        )) {

            return false;
        }

        /*
         * ========================================
         * 玩家视线方向
         * ========================================
         */
        Vec3 view =
                player.getViewVector(
                        1.0F
                ).normalize();

        /*
         * 玩家眼睛 → 鬼身体中心。
         */
        Vec3 toGhost =
                ghost.position()
                        .add(
                                0.0D,
                                ghost.getBbHeight()
                                        * 0.5D,
                                0.0D
                        )
                        .subtract(
                                player.getEyePosition()
                        )
                        .normalize();

        /*
         * dot 越接近 1，
         * 表示越正对着鬼。
         */
        double dot =
                view.dot(
                        toGhost
                );

        return dot >= LOOK_THRESHOLD;
    }
}