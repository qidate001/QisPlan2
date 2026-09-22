package com.qidate.qisplan2.ghost.reboot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class GhostRebootSources {

    private GhostRebootSources() {
    }

    /**
     * 玩家主动发动重启。
     */
    public static GhostRebootSource player(
            ServerPlayer player
    ) {
        return new GhostRebootSource(
                GhostRebootSourceType.PLAYER,
                player.getUUID(),
                null
        );
    }

    /**
     * 某只厉鬼发动重启。
     *
     * sourceUUID：
     * 发动这次重启的鬼 / 驭鬼者 UUID
     *
     * sourceType：
     * 具体厉鬼类型
     */
    public static GhostRebootSource ghost(
            UUID sourceUUID,
            ResourceLocation sourceType
    ) {
        return new GhostRebootSource(
                GhostRebootSourceType.GHOST,
                sourceUUID,
                sourceType
        );
    }

    /**
     * 某个鬼域发动重启。
     *
     * sourceUUID：
     * 鬼域来源 UUID
     *
     * sourceType：
     * 鬼域类型
     */
    public static GhostRebootSource domain(
            UUID sourceUUID,
            ResourceLocation sourceType
    ) {
        return new GhostRebootSource(
                GhostRebootSourceType.DOMAIN,
                sourceUUID,
                sourceType
        );
    }

    /**
     * 世界 / 特殊事件发动重启。
     *
     * 没有具体实体来源时，sourceUUID 可以为 null。
     */
    public static GhostRebootSource event(
            ResourceLocation sourceType
    ) {
        return new GhostRebootSource(
                GhostRebootSourceType.EVENT,
                null,
                sourceType
        );
    }
}