package com.qidate.qisplan2.ghost.curse;

import com.qidate.qisplan2.ghost.curse.type.TestCurse;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class CurseRegistry {

    private static final Map<
            ResourceLocation,
            CurseFactory
            > CURSES =
            new HashMap<>();

    private static boolean initialized = false;

    private CurseRegistry() {
    }

    public static void register(
            ResourceLocation id,
            CurseFactory factory
    ) {
        CURSES.put(
                id,
                factory
        );
    }

    public static Curse create(
            ResourceLocation id,
            java.util.UUID target
    ) {

        CurseFactory factory =
                CURSES.get(id);

        if (factory == null) {
            return null;
        }

        return factory.create(target);
    }

    public static boolean contains(
            ResourceLocation id
    ) {
        return CURSES.containsKey(id);
    }

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

        register(
                TestCurse.ID,
                TestCurse::new
        );
    }
}