package com.qidate.qisplan2.client.curse;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 客户端当前玩家的诅咒状态。
 *
 * <p>
 * 这里只保存服务端同步过来的诅咒展示数据，
 * 不参与任何诅咒实际逻辑。
 * </p>
 */
public final class ClientCurseState {

    /*
     * ========================================================
     * 当前诅咒
     * ========================================================
     *
     * Key：
     *      Curse 的 UUID
     *
     * Value：
     *      客户端需要显示的数据
     */
    private static final Map<
            UUID,
            CurseData
            > CURSES =
            new LinkedHashMap<>();


    private ClientCurseState() {
    }


    /*
     * ========================================================
     * Curse 数据
     * ========================================================
     */

    /**
     * 客户端用于显示的诅咒数据。
     *
     * <p>
     * 与服务端不同，客户端需要自行计算持续时间，
     * 因此 remainingTicks 必须允许在本地 Tick 中变化。
     * </p>
     */
    public static final class CurseData {

        /**
         * 诅咒 UUID。
         */
        private final UUID id;

        /**
         * 诅咒类型。
         */
        private final ResourceLocation type;

        /**
         * 当前诅咒强度。
         */
        private int strength;

        /**
         * 客户端当前预测的剩余持续时间。
         *
         * <p>
         * -1 表示无限持续。
         * </p>
         */
        private int remainingTicks;

        public CurseData(
                UUID id,
                ResourceLocation type,
                int strength,
                int remainingTicks
        ) {
            this.id = id;
            this.type = type;
            this.strength = strength;
            this.remainingTicks = remainingTicks;
        }

        public UUID id() {
            return id;
        }

        public ResourceLocation type() {
            return type;
        }

        public int strength() {
            return strength;
        }

        public int remainingTicks() {
            return remainingTicks;
        }

        /**
         * 设置诅咒强度。
         */
        public void setStrength(
                int strength
        ) {
            this.strength = strength;
        }

        /**
         * 设置剩余持续时间。
         */
        public void setRemainingTicks(
                int remainingTicks
        ) {
            this.remainingTicks = remainingTicks;
        }

        /**
         * 客户端本地 Tick。
         *
         * <p>
         * 客户端只负责预测持续时间，
         * 服务端仍然是最终权威。
         * </p>
         */
        public void tick() {

            /*
             * 只有有限持续时间才进行倒计时。
             *
             * -1 = 无限持续
             *  0 = 已经结束
             * >0 = 剩余 Tick
             */
            if (remainingTicks > 0) {
                remainingTicks--;
            }
        }
    }


    /*
     * ========================================================
     * 查询
     * ========================================================
     */

    /**
     * 获取当前所有诅咒。
     */
    public static Collection<CurseData> getAll() {
        return CURSES.values();
    }

    /**
     * 获取指定诅咒。
     */
    public static CurseData get(
            UUID id
    ) {
        return CURSES.get(id);
    }

    /**
     * 判断当前是否存在指定诅咒。
     */
    public static boolean contains(
            UUID id
    ) {
        return CURSES.containsKey(id);
    }

    /**
     * 获取当前诅咒数量。
     */
    public static int size() {
        return CURSES.size();
    }

    /**
     * 判断当前是否没有诅咒。
     */
    public static boolean isEmpty() {
        return CURSES.isEmpty();
    }


    /*
     * ========================================================
     * 修改
     * ========================================================
     */

    /**
     * 添加或更新一个诅咒。
     */
    public static void put(
            CurseData curse
    ) {
        CURSES.put(
                curse.id(),
                curse
        );
    }

    /**
     * 移除指定诅咒。
     */
    public static void remove(
            UUID id
    ) {
        CURSES.remove(id);
    }

    /**
     * 清空所有客户端诅咒。
     */
    public static void clear() {
        CURSES.clear();
    }

    /**
     * 客户端所有诅咒进行一次 Tick。
     *
     * <p>
     * 持续时间由客户端本地计算，
     * 不需要服务器每 Tick 发送数据包。
     * </p>
     */
    public static void tick() {

        for (CurseData curse :
                CURSES.values()
        ) {
            curse.tick();
        }
    }
}