package com.qidate.qisplan2.ghost.reboot;

import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record GhostRebootSource(

        /**
         * 来源类别
         */
        GhostRebootSourceType type,

        /**
         * 来源对应的 UUID。
         *
         * 可以是：
         * 玩家 UUID
         * 鬼实体 UUID
         * 鬼域来源 UUID
         *
         * 某些来源可能没有 UUID，例如世界事件。
         */
        UUID sourceUUID,

        /**
         * 来源的具体类型。
         *
         * 例如：
         * qisplan2:ghost_eye
         * qisplan2:ghost_lake
         */
        ResourceLocation sourceType

) {
}