package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = QisPlan2.MODID)
public class GhostCarpetHandler {

    /**
     * 15秒 = 300 ticks
     */
    private static final long TRIGGER_TICKS = 15L * 20L;

    /**
     * 鬼地毯诅咒的数据标签。
     */
    public static final String GHOST_CARPET_CURSE_COUNT =
            "ghost_carpet_curse_count";

    /**
     * 每个实体已经累计踩了多少 tick。
     */
    private static final Map<UUID, Long> CARPET_TICKS =
            new HashMap<>();

    /**
     * 本次是否已经触发。
     */
    private static final Map<UUID, Boolean> TRIGGERED =
            new HashMap<>();

    /**
     * 日志节流。
     */
//    private static final Map<UUID, Long> LAST_LOG_TIME =
//            new HashMap<>();


    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {

        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        if (entity.level().isClientSide()) {
            return;
        }

        UUID uuid = entity.getUUID();

        /*
         * =========================
         * 死亡 → 清空鬼地毯进度
         * =========================
         */
        if (!entity.isAlive()) {
            reset(entity);
            return;
        }

        /*
         * =========================
         * 检查鬼地毯
         * =========================
         */

        BlockPos entityPos = entity.blockPosition();

        BlockState currentState =
                entity.level().getBlockState(entityPos);

        BlockState belowState =
                entity.level().getBlockState(entityPos.below());

        boolean onGhostCarpet =
                currentState.is(QisPlan2.GHOST_CARPET.get())
                        || belowState.is(QisPlan2.GHOST_CARPET.get());


        /*
         * =========================
         * 已经触发
         * =========================
         */

        if (TRIGGERED.getOrDefault(uuid, false)) {
            return;
        }


        /*
         * =========================
         * 没踩鬼地毯，暂停。
         * =========================
         */

        if (!onGhostCarpet) {
            return;
        }


        /*
         * =========================
         * 正在踩鬼地毯
         *
         * 每 tick +1
         * =========================
         */

        long ticks =
                CARPET_TICKS.getOrDefault(uuid, 0L);

        ticks++;

        CARPET_TICKS.put(
                uuid,
                ticks
        );


        /*
         * =========================
         * 日志
         * =========================
         */

//        long lastLog =
//                LAST_LOG_TIME.getOrDefault(
//                        uuid,
//                        -20L
//                );

//        long gameTime =
//                entity.level().getGameTime();

//        if (gameTime - lastLog >= 20) {
//
//            LAST_LOG_TIME.put(
//                    uuid,
//                    gameTime
//            );
//
//            long seconds =
//                    ticks / 20;
//
//            System.out.println(
//                    "[QisPlan2][GhostCarpet] "
//                            + entity.getName().getString()
//                            + " 鬼地毯累计踩踏："
//                            + seconds
//                            + "/15 秒"
//            );
//        }


        /*
         * =========================
         * 达到15秒
         * =========================
         */

        if (ticks >= TRIGGER_TICKS) {

//            System.out.println(
//                    "[QisPlan2][GhostCarpet] "
//                            + "！！！15秒累计踩踏完成！！！"
//                            + " 实体="
//                            + entity.getName().getString()
//            );

            applyGhostCarpetCurse(entity);

            TRIGGERED.put(
                    uuid,
                    true
            );
        }
    }


    /**
     * 增加一层鬼地毯诅咒。
     */
    private static void applyGhostCarpetCurse(
            LivingEntity entity
    ) {

        /*
         * ========================================
         * 鬼地毯诅咒触发
         * ========================================
         */

//        System.out.println(
//                "[QisPlan2][GhostCarpet] "
//                        + "鬼地毯诅咒触发！"
//                        + " 实体="
//                        + entity.getName().getString()
//        );


        /*
         * ========================================
         * 粒子
         * ========================================
         */

        if (entity.level() instanceof ServerLevel serverLevel) {

            serverLevel.sendParticles(
                    ParticleTypes.SOUL,
                    entity.getX(),
                    entity.getY() + 0.5,
                    entity.getZ(),
                    20,
                    0.4,
                    0.5,
                    0.4,
                    0.05
            );

            serverLevel.playSound(
                    null,
                    entity.blockPosition(),
                    SoundEvents.SOUL_ESCAPE.value(),
                    SoundSource.BLOCKS,
                    1.0F,
                    0.8F
            );
        }


        /*
         * ========================================
         * 请求灵异死亡
         * ========================================
         */

        boolean killed =
                SupernaturalDeathHandler.tryKill(
                        entity,
                        ModDamageTypes.ghostCarpet(entity)
                );


        /*
         * ========================================
         * 日志
         * ========================================
         */

//        if (killed) {
//
//            System.out.println(
//                    "[QisPlan2][GhostCarpet] "
//                            + "鬼地毯死亡成功："
//                            + entity.getName().getString()
//            );
//
//        } else {
//
//            System.out.println(
//                    "[QisPlan2][GhostCarpet] "
//                            + "鬼地毯死亡被抵消："
//                            + entity.getName().getString()
//            );
//        }
    }


    /**
     * 离开鬼地毯：
     *
     * 计时清零
     * 触发状态清零
     * 日志计时清零
     */
    private static void reset(
            LivingEntity entity
    ) {

        UUID uuid =
                entity.getUUID();

        CARPET_TICKS.remove(uuid);
        TRIGGERED.remove(uuid);
//        LAST_LOG_TIME.remove(uuid);
    }
}