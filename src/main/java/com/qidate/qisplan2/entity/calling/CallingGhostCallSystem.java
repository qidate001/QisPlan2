package com.qidate.qisplan2.entity.calling;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * 管理喊人鬼的喊名行为。
 *
 * <p>
 * 负责：
 * <ul>
 *     <li>喊名冷却</li>
 *     <li>喊名次数</li>
 *     <li>达到最大次数后的放弃</li>
 *     <li>播放玩家对应的喊名声音</li>
 *     <li>保存与读取喊名状态</li>
 * </ul>
 *
 * <p>
 * 这里不负责目标选择、移动以及回头后的灵异攻击。
 */
public final class CallingGhostCallSystem {

    /**
     * 两次喊名之间的最小冷却。
     */
    private static final int CALL_COOLDOWN_TICKS = 100;

    /**
     * 喊名次数上限。
     */
    private static final int MAX_CALL_COUNT = 7;

    private static final String NBT_CALL_COOLDOWN =
            "QisPlan2CallingGhostCallCooldown";

    private static final String NBT_CALL_COUNT =
            "QisPlan2CallingGhostCallCount";

    private CallingGhostCallSystem() {
    }

    /**
     * 每 tick 更新喊名状态。
     *
     * <p>
     * 冷却结束后喊一次目标玩家的名字。
     * 达到七次后放弃当前目标。
     */
    public static void tick(
            CallingGhost ghost,
            ServerPlayer player
    ) {
        if (ghost.callCooldown > 0) {
            ghost.callCooldown--;
            return;
        }

        if (ghost.callCount >= MAX_CALL_COUNT) {
            ghost.clearTarget();
            return;
        }

        callPlayerName(ghost, player);
    }

    /**
     * 初始化新的喊名状态。
     */
    public static void startCalling(
            CallingGhost ghost
    ) {
        ghost.callCount = 0;
        ghost.callCooldown = CALL_COOLDOWN_TICKS;
    }

    /**
     * 播放目标玩家对应的喊名声音。
     */
    private static void callPlayerName(
            CallingGhost ghost,
            ServerPlayer player
    ) {
        SoundEvent sound =
                CallingGhostSounds.getSound(player);

        if (sound == null) {
            QisPlan2.LOGGER.warn(
                    "[QisPlan2] 喊人鬼无法找到玩家 {} 对应的声音",
                    player.getGameProfile().getName()
            );
            ghost.callCooldown = CALL_COOLDOWN_TICKS;
            return;
        }

        ghost.callCount++;
        ghost.callCooldown = CALL_COOLDOWN_TICKS;

        QisPlan2.LOGGER.info(
                "[QisPlan2] 喊人鬼喊了玩家 {}，第 {} 次",
                player.getGameProfile().getName(),
                ghost.callCount
        );

        ghost.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                sound,
                SoundSource.HOSTILE,
                1.0F,
                1.0F
        );
    }

    /**
     * 保存喊名状态。
     */
    public static void save(
            CallingGhost ghost,
            CompoundTag tag
    ) {
        tag.putInt(
                NBT_CALL_COOLDOWN,
                ghost.callCooldown
        );

        tag.putInt(
                NBT_CALL_COUNT,
                ghost.callCount
        );
    }

    /**
     * 读取喊名状态。
     */
    public static void load(
            CallingGhost ghost,
            CompoundTag tag
    ) {
        ghost.callCooldown =
                Math.max(
                        0,
                        tag.getInt(NBT_CALL_COOLDOWN)
                );

        ghost.callCount =
                Math.max(
                        0,
                        tag.getInt(NBT_CALL_COUNT)
                );
    }
}