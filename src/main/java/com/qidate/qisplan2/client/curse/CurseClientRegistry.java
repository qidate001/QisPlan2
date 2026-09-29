package com.qidate.qisplan2.client.curse;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 客户端诅咒注册表。
 *
 * <p>
 * 负责保存诅咒在客户端上的表现信息，
 * 不参与诅咒实际逻辑。
 * </p>
 */
public final class CurseClientRegistry {

    private static final Map<
            ResourceLocation,
            CurseClientType
            > CURSES =
            new HashMap<>();

    private CurseClientRegistry() {
    }

    /**
     * 客户端诅咒显示定义。
     *
     * @param icon 诅咒图标
     * @param canDetect 玩家自身是否能够察觉该诅咒
     */
    public record CurseClientType(
            ResourceLocation icon,
            boolean canDetect
    ) {
    }

    /**
     * 注册客户端诅咒类型。
     */
    public static void register(
            ResourceLocation id,
            CurseClientType type
    ) {

        CURSES.put(
                id,
                type
        );
    }

    /**
     * 获取客户端诅咒类型。
     */
    public static CurseClientType get(
            ResourceLocation id
    ) {

        return CURSES.get(id);
    }

    /**
     * 判断客户端是否注册了该诅咒。
     */
    public static boolean contains(
            ResourceLocation id
    ) {

        return CURSES.containsKey(id);
    }

    /**
     * 获取全部客户端诅咒类型。
     */
    public static Map<
            ResourceLocation,
            CurseClientType
            > getAll() {

        return Collections.unmodifiableMap(
                CURSES
        );
    }
}