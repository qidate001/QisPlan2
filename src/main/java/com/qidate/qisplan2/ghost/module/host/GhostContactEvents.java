package com.qidate.qisplan2.ghost.module.host;

import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.event.TouchEvent;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;

/** 身体碰撞与鬼域覆盖共用接触采样；一个宿主对一个目标每 tick 最多报告一次。 */
public final class GhostContactEvents {
    private static final Map<Object, Samples> SOURCES = new WeakHashMap<>();
    private GhostContactEvents() {}

    /** 可独立验证的时间采样器；离开后不补算空白时间。 */
    public static final class Samples {
        private final Map<UUID, Long> last = new HashMap<>();
        private long cleanup = Long.MIN_VALUE;

        /** 返回本次阶段；同 tick 重复来源返回 null，避免多个鬼域让15秒变短。 */
        public TouchEvent.Phase sample(UUID target,long tick) {
            if(cleanup != tick && tick % 20 == 0) {
                last.values().removeIf(value -> value < tick - 1);
                cleanup = tick;
            }

            Long previous = last.put(target,tick);
            if(previous != null && previous == tick)
                return null;

            return previous != null && previous == tick - 1 ? TouchEvent.Phase.CONTINUE : TouchEvent.Phase.BEGIN;
        }
    }

    /** 只在适配边界解析稳定宿主身份，瞬时点击事件不经过这个持续采样器。 */
    public static void sample(GhostModuleHost host, LivingEntity target) {
        Object key = host instanceof GhostItemModuleHost item ? item.stack() : GhostEnvironmentAdapters.bearer(host);
        if(key == null) key = host;

        var phase = SOURCES.computeIfAbsent(key,ignored -> new Samples()).sample(target.getUUID(),target.level().getGameTime());

        if(phase != null)
            GhostModuleRuntime.dispatch(host, new TouchEvent(target, phase, 1));
    }
}
