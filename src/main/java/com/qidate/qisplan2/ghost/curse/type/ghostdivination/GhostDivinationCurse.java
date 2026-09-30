package com.qidate.qisplan2.ghost.curse.type.ghostdivination;

import com.qidate.qisplan2.ghost.curse.AbstractCurse;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public class GhostDivinationCurse
        extends AbstractCurse {

    /**
     * 生签诅咒类型。
     */
    public static final ResourceLocation TYPE =
            GhostDivinationCurseType.ID;

    /**
     * 生签默认强度。
     */
    public static final int DEFAULT_STRENGTH = 50;

    /**
     * 灵异冲突式死机状态下的生签强度。
     */
    public static final int STUN_STRENGTH = 20;

    /**
     * 生签持续时间。
     */
    public static final int DEFAULT_DURATION =
            5 * 60 * 20;

    /**
     * 创建新的生签诅咒。
     */
    public GhostDivinationCurse(
            UUID target,
            GhostDivinationCurseSource source,
            int strength,
            int remainingTicks
    ) {
        this(
                UUID.randomUUID(),
                target,
                source,
                strength,
                remainingTicks
        );
    }

    /**
     * 从保存数据恢复生签诅咒。
     */
    public GhostDivinationCurse(
            UUID id,
            UUID target,
            GhostDivinationCurseSource source,
            int strength,
            int remainingTicks
    ) {
        super(
                id,
                GhostDivinationCurseType.ID,
                target,
                source,
                strength,
                remainingTicks
        );
    }

    /**
     * 增加剩余持续时间。
     *
     * @param ticks 增加的 Tick 数
     */
    public void addTime(
            int ticks
    ) {
        if (ticks <= 0) {
            return;
        }

        setRemainingTicks(
                getRemainingTicks() + ticks
        );
    }

    @Override
    public void tick(
            MinecraftServer server
    ) {
        tickDuration();
    }
}