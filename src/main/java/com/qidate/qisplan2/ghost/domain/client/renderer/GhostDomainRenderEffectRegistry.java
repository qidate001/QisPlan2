package com.qidate.qisplan2.ghost.domain.client.renderer;

import java.util.ArrayList;
import java.util.List;

public final class GhostDomainRenderEffectRegistry {

    private static final List<GhostDomainRenderEffect> EFFECTS =
            new ArrayList<>();

    private GhostDomainRenderEffectRegistry() {
    }

    /**
     * 注册一个鬼域渲染效果。
     */
    public static void register(
            GhostDomainRenderEffect effect
    ) {

        if (!EFFECTS.contains(effect)) {
            EFFECTS.add(effect);
        }
    }

    /**
     * 获取所有已经注册的鬼域渲染效果。
     */
    public static List<GhostDomainRenderEffect> getEffects() {

        return List.copyOf(
                EFFECTS
        );
    }
}