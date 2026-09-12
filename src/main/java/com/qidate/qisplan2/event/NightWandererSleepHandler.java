package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.entity.NightWanderer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = QisPlan2.MODID)
public final class NightWandererSleepHandler {

    private static final int KILL_INTERVAL = 30 * 20;

    private NightWandererSleepHandler() {
    }

    @SubscribeEvent
    public static void onSleepFinished(
            SleepFinishedTimeEvent event
    ) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        /*
         * 在当前维度的玩家附近寻找夜游鬼。
         *
         * 这里使用玩家位置而不是整个世界的 WorldBorder，
         * 因为睡眠事件本身就是由玩家触发的。
         */
        List<NightWanderer> ghosts = new ArrayList<>();

        for (var player : level.players()) {

            List<NightWanderer> nearbyGhosts =
                    level.getEntitiesOfClass(
                            NightWanderer.class,
                            player.getBoundingBox().inflate(128.0D)
                    );

            for (NightWanderer ghost : nearbyGhosts) {
                if (!ghosts.contains(ghost)) {
                    ghosts.add(ghost);
                }
            }
        }

        if (ghosts.isEmpty()) {
            return;
        }

        /*
         * SleepFinishedTimeEvent#getNewTime()
         * 是本次睡眠跳过的时间，而不是世界绝对时间。
         */
        long skippedTime = event.getNewTime();

        /*
         * 每 30 秒睡眠时间，
         * 相当于夜游鬼额外猎杀一个实体。
         */
        int kills =
                (int) (skippedTime / KILL_INTERVAL);

        if (kills <= 0) {
            QisPlan2.LOGGER.info(
                    "[夜游鬼睡眠] 睡眠时间不足 {} tick，本次不增加击杀数",
                    KILL_INTERVAL
            );

            return;
        }

        /*
         * 目前按照当前维度找到的第一只夜游鬼处理。
         */
        NightWanderer ghost = ghosts.getFirst();

        /*
         * 夜游鬼已经死亡。
         */
        if (!ghost.isAlive()) {
            QisPlan2.LOGGER.info(
                    "[夜游鬼睡眠] 夜游鬼 {} 已死亡，本次不增加击杀数",
                    ghost.getUUID()
            );

            return;
        }

        /*
         * 夜游鬼已经被棺材钉钉住。
         */
        if (ghost.isCoffinNailed()) {
            QisPlan2.LOGGER.info(
                    "[夜游鬼睡眠] 夜游鬼 {} 已被棺材钉钉住，本次不增加击杀数",
                    ghost.getUUID()
            );

            return;
        }

        /*
         * 夜游鬼已经被灵异压制，无法继续活动。
         */
        if (ghost.isSupernaturallyStunned()) {
            QisPlan2.LOGGER.info(
                    "[夜游鬼睡眠] 夜游鬼 {} 已被灵异压制，本次不增加击杀数",
                    ghost.getUUID()
            );

            return;
        }

        int oldKillCount =
                ghost.getKillCount();

        double oldStrength =
                ghost.getSupernaturalStrength();

        /*
         * 根据睡眠时间增加夜游鬼的“猎杀记录”。
         */
        ghost.addKillCount(kills);

        QisPlan2.LOGGER.info(
                "[夜游鬼睡眠] 睡眠 {} tick，"
                        + "夜游鬼击杀数 {} -> {}，"
                        + "灵异强度 {} -> {}",
                skippedTime,
                oldKillCount,
                ghost.getKillCount(),
                oldStrength,
                ghost.getSupernaturalStrength()
        );
    }
}