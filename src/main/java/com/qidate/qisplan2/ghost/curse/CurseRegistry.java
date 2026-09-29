package com.qidate.qisplan2.ghost.curse;

import com.qidate.qisplan2.ghost.curse.type.ghostdivination.GhostDivinationCurseType;
import com.qidate.qisplan2.ghost.curse.type.ghosttombstone.GhostTombstoneCurseType;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class CurseRegistry {

    private static final Map<
            ResourceLocation,
            CurseType
            > CURSES =
            new HashMap<>();

    private static boolean initialized = false;

    private CurseRegistry() {
    }

    /**
     * 注册一个诅咒类型。
     */
    public static void register(
            CurseType curse
    ) {

        CURSES.put(
                curse.id(),
                curse
        );
    }

    /**
     * 获取诅咒类型。
     */
    public static CurseType get(
            ResourceLocation id
    ) {

        return CURSES.get(id);
    }

    /**
     * 判断诅咒类型是否存在。
     */
    public static boolean contains(
            ResourceLocation id
    ) {

        return CURSES.containsKey(id);
    }

    /**
     * 获取所有诅咒类型 ID。
     *
     * 用于命令自动补全。
     */
    public static Set<ResourceLocation> ids() {

        return Collections.unmodifiableSet(
                CURSES.keySet()
        );
    }

    /**
     * 注册所有诅咒类型。
     */
    public static void bootstrap() {

        if (initialized) {
            return;
        }

        initialized = true;

        // 鬼墓碑
        register(new GhostTombstoneCurseType());

        // 鬼签（生签）
        register(new GhostDivinationCurseType());
    }
}