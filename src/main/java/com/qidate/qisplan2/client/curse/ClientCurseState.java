package com.qidate.qisplan2.client.curse;

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
     * @param id 诅咒 UUID
     * @param type 诅咒类型
     * @param strength 当前诅咒强度
     * @param remainingTicks 剩余持续时间
     */
    public record CurseData(
            UUID id,
            ResourceLocation type,
            int strength,
            int remainingTicks
    ) {
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
}