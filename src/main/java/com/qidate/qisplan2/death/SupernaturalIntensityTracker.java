package com.qidate.qisplan2.death;

import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SupernaturalIntensityTracker {

    private SupernaturalIntensityTracker() {
    }

    /**
     * 统计窗口：
     *
     * 20 tick = 1 秒
     */
    private static final long WINDOW_TICKS = 20L;

    private record Entry(
            long gameTime,
            double intensity
    ) {
    }

    /**
     * 每个实体最近一秒受到的灵异袭击。
     */
    private static final Map<
            UUID,
            ArrayDeque<Entry>
            > HISTORY = new HashMap<>();

    /**
     * 记录一次灵异袭击，
     * 返回最近一秒累计灵异强度。
     */
    public static double addAndGetRecentIntensity(
            LivingEntity entity,
            double intensity
    ) {

        long now =
                entity.level().getGameTime();

        UUID uuid =
                entity.getUUID();

        ArrayDeque<Entry> deque =
                HISTORY.computeIfAbsent(
                        uuid,
                        ignored -> new ArrayDeque<>()
                );

        /*
         * 删除超过一秒的数据。
         */
        while (!deque.isEmpty()
                && now - deque.peekFirst().gameTime() >= WINDOW_TICKS) {

            deque.removeFirst();
        }

        /*
         * 加入本次袭击。
         */
        deque.addLast(
                new Entry(
                        now,
                        intensity
                )
        );

        /*
         * 求最近一秒累计强度。
         */
        double total = 0.0D;

        for (Entry entry : deque) {
            total += entry.intensity();
        }

        return total;
    }

    /**
     * 查询最近一秒累计强度，
     * 不新增记录。
     */
    public static double getRecentIntensity(
            LivingEntity entity
    ) {

        long now =
                entity.level().getGameTime();

        ArrayDeque<Entry> deque =
                HISTORY.get(
                        entity.getUUID()
                );

        if (deque == null) {
            return 0.0D;
        }

        while (!deque.isEmpty()
                && now - deque.peekFirst().gameTime() >= WINDOW_TICKS) {

            deque.removeFirst();
        }

        double total = 0.0D;

        for (Entry entry : deque) {
            total += entry.intensity();
        }

        return total;
    }

    /**
     * 清除某实体记录。
     *
     * 可在玩家死亡、
     * 实体移除时调用。
     */
    public static void clear(
            LivingEntity entity
    ) {
        HISTORY.remove(
                entity.getUUID()
        );
    }
}