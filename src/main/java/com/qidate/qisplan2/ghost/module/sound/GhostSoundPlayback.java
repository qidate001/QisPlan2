package com.qidate.qisplan2.ghost.module.sound;

import net.minecraft.server.level.ServerPlayer;

/**
 * 灵异声音的客户端播放适配器。
 *
 * 通用声音管理器只负责声音传播状态的判断。
 * 具体声音如何播放、如何停止，由声源宿主提供适配器。
 *
 * 例如：
 * - 鬼钢琴：发送钢琴播放与停止数据包。
 * - 其他实体或方块：可以使用完全不同的播放实现。
 *
 * 管理器不直接依赖具体声音的网络实现。
 */
public interface GhostSoundPlayback {

    /**
     * 通知指定玩家开始播放该声音。
     */
    void start(ServerPlayer player);

    /**
     * 通知指定玩家停止播放该声音。
     */
    void stop(ServerPlayer player);

    /**
     * 无操作适配器。
     *
     * 适用于只需要声音事件、不需要客户端播放的声源。
     */
    GhostSoundPlayback NONE = new GhostSoundPlayback() {
        @Override
        public void start(ServerPlayer player) {
        }

        @Override
        public void stop(ServerPlayer player) {
        }
    };
}