package com.qidate.qisplan2.entity.calling;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 管理喊人鬼的目标系统。
 *
 * <p>
 * 负责：
 * <ul>
 *     <li>寻找目标玩家</li>
 *     <li>验证目标是否可以被追踪</li>
 *     <li>绑定与清除当前目标</li>
 *     <li>管理最近袭击目标的冷却</li>
 *     <li>检测鬼石砖与关闭鬼门造成的灵异阻隔</li>
 *     <li>保存与读取目标相关的 NBT</li>
 * </ul>
 *
 * <p>
 * 这里不负责喊名、移动或回头后的灵异攻击。
 */
public final class CallingGhostTargetSystem {

    /**
     * 最近一次袭击同一玩家后的目标冷却。
     *
     * <p>
     * 30 秒 = 600 tick。
     */
    private static final int ATTACK_TARGET_COOLDOWN_TICKS =
            20 * 30;

    /**
     * 目标搜索范围。
     */
    private static final double TARGET_SEARCH_RANGE =
            64.0D;

    /**
     * 灵异阻隔检测的采样间隔。
     */
    private static final double BLOCK_CHECK_STEP =
            0.5D;

    private static final String NBT_TARGET_PLAYER =
            "QisPlan2CallingGhostTarget";

    private static final String NBT_ATTACK_TARGET_COOLDOWN =
            "QisPlan2CallingGhostAttackTargetCooldown";

    private static final String NBT_RECENTLY_ATTACKED_PLAYER =
            "QisPlan2CallingGhostRecentlyAttackedPlayer";

    private CallingGhostTargetSystem() {
    }

    /**
     * 每 tick 更新目标系统。
     */
    public static void tick(
            CallingGhost ghost
    ) {
        tickAttackTargetCooldown(ghost);

        ServerPlayer player =
                getTargetPlayer(ghost);

        /*
         * 当前没有有效目标时，
         * 尝试寻找新的玩家。
         */
        if (player == null) {

            ghost.clearTarget();

            player = findNearestPlayer(ghost);

            if (player != null) {
                ghost.setTargetPlayer(player);
            }
        }

        /*
         * 附近没有玩家。
         */
        if (player == null) {
            return;
        }

        /*
         * 玩家死亡。
         */
        if (!player.isAlive()) {
            ghost.clearTarget();
            return;
        }

        /*
         * 鬼石砖或关闭鬼门
         * 会暂时隔绝喊人鬼。
         */
        if (!canTrackPlayer(
                ghost,
                player
        )) {
            return;
        }
    }

    /**
     * 更新最近袭击目标的冷却。
     */
    private static void tickAttackTargetCooldown(
            CallingGhost ghost
    ) {
        if (ghost.attackTargetCooldown <= 0) {
            return;
        }

        ghost.attackTargetCooldown--;

        if (ghost.attackTargetCooldown <= 0) {
            ghost.recentlyAttackedPlayerUUID = null;
        }
    }

    /**
     * 获取当前绑定的目标玩家。
     *
     * <p>
     * 如果玩家已经跨维度，
     * 当前喊人鬼无法继续跟随该玩家。
     */
    public static ServerPlayer getTargetPlayer(
            CallingGhost ghost
    ) {
        if (!(ghost.level()
                instanceof ServerLevel serverLevel)) {
            return null;
        }

        if (ghost.targetPlayerUUID == null) {
            return null;
        }

        Player player =
                serverLevel.getPlayerByUUID(
                        ghost.targetPlayerUUID
                );

        if (player instanceof ServerPlayer serverPlayer) {

            if (serverPlayer.level()
                    != ghost.level()) {
                return null;
            }

            return serverPlayer;
        }

        return null;
    }

    /**
     * 判断玩家是否可以成为新的目标。
     */
    private static boolean canTargetPlayer(
            CallingGhost ghost,
            ServerPlayer player
    ) {
        /*
         * 创造模式不追踪。
         */
        if (player.isCreative()) {
            return false;
        }

        /*
         * 旁观模式不追踪。
         */
        if (player.isSpectator()) {
            return false;
        }

        /*
         * 必须存活。
         */
        if (!player.isAlive()) {
            return false;
        }

        /*
         * 鬼石砖 / 关闭鬼门
         * 可以隔绝喊人鬼。
         */
        if (!canTrackPlayer(
                ghost,
                player
        )) {
            return false;
        }

        /*
         * 最近刚刚袭击过的玩家，
         * 暂时不能再次成为目标。
         */
        if (ghost.recentlyAttackedPlayerUUID != null
                && player.getUUID().equals(
                ghost.recentlyAttackedPlayerUUID
        )) {
            return false;
        }

        return true;
    }

    public static void setTargetPlayer(
            CallingGhost ghost,
            ServerPlayer player
    ) {
        ghost.targetPlayerUUID =
                player.getUUID();
    }

    public static void clearTarget(
            CallingGhost ghost
    ) {
        ghost.targetPlayerUUID = null;
    }

    /**
     * 在附近寻找最近的可追踪玩家。
     */
    private static ServerPlayer findNearestPlayer(
            CallingGhost ghost
    ) {
        if (!(ghost.level()
                instanceof ServerLevel serverLevel)) {
            return null;
        }

        double maxDistanceSqr =
                TARGET_SEARCH_RANGE
                        * TARGET_SEARCH_RANGE;

        ServerPlayer nearestPlayer = null;

        double nearestDistanceSqr =
                Double.MAX_VALUE;

        for (ServerPlayer player
                : serverLevel.players()) {

            double distanceSqr =
                    ghost.distanceToSqr(player);

            if (distanceSqr > maxDistanceSqr) {
                continue;
            }

            if (!canTargetPlayer(
                    ghost,
                    player
            )) {
                continue;
            }

            if (distanceSqr < nearestDistanceSqr) {
                nearestDistanceSqr =
                        distanceSqr;

                nearestPlayer =
                        player;
            }
        }

        if (nearestPlayer != null) {
            QisPlan2.LOGGER.info(
                    "[QisPlan2] 喊人鬼找到新目标：{}",
                    nearestPlayer
                            .getGameProfile()
                            .getName()
            );
        }

        return nearestPlayer;
    }

    /**
     * 记录玩家刚刚被袭击，并开始目标冷却。
     */
    public static void markRecentlyAttacked(
            CallingGhost ghost,
            ServerPlayer player
    ) {
        ghost.recentlyAttackedPlayerUUID =
                player.getUUID();

        ghost.attackTargetCooldown =
                ATTACK_TARGET_COOLDOWN_TICKS;
    }

    /**
     * 判断喊人鬼是否能够继续追踪玩家。
     *
     * <p>
     * 鬼石砖永久阻挡。
     * 关闭鬼门阻挡，打开鬼门不阻挡。
     */
    public static boolean canTrackPlayer(
            CallingGhost ghost,
            ServerPlayer player
    ) {
        Vec3 start =
                ghost.position().add(
                        0.0D,
                        0.8D,
                        0.0D
                );

        Vec3 end =
                player.getEyePosition();

        double distance =
                start.distanceTo(end);

        int steps =
                Math.max(
                        1,
                        (int) Math.ceil(
                                distance
                                        / BLOCK_CHECK_STEP
                        )
                );

        for (int i = 1; i < steps; i++) {

            double t =
                    (double) i / steps;

            double x =
                    Mth.lerp(
                            t,
                            start.x,
                            end.x
                    );

            double y =
                    Mth.lerp(
                            t,
                            start.y,
                            end.y
                    );

            double z =
                    Mth.lerp(
                            t,
                            start.z,
                            end.z
                    );

            BlockPos checkPos =
                    BlockPos.containing(
                            x,
                            y,
                            z
                    );

            BlockState state =
                    ghost.level().getBlockState(
                            checkPos
                    );

            if (isCallingGhostBlocker(state)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 判断方块是否能够阻挡喊人鬼。
     */
    private static boolean isCallingGhostBlocker(
            BlockState state
    ) {
        /*
         * 鬼石砖：
         *
         * 永久阻挡。
         */
        if (state.is(
                ModBlocks.GHOST_STONE_BRICKS.get()
        )) {
            return true;
        }

        /*
         * 鬼门：
         *
         * 关闭 → 阻挡
         * 打开 → 不阻挡
         */
        if (state.is(
                ModBlocks.GHOST_DOOR.get()
        )) {
            return !state.getValue(
                    DoorBlock.OPEN
            );
        }

        return false;
    }

    /**
     * 保存目标系统状态。
     */
    public static void save(
            CallingGhost ghost,
            CompoundTag tag
    ) {
        if (ghost.targetPlayerUUID != null) {
            tag.putUUID(
                    NBT_TARGET_PLAYER,
                    ghost.targetPlayerUUID
            );
        }

        tag.putInt(
                NBT_ATTACK_TARGET_COOLDOWN,
                ghost.attackTargetCooldown
        );

        if (ghost.recentlyAttackedPlayerUUID != null) {
            tag.putUUID(
                    NBT_RECENTLY_ATTACKED_PLAYER,
                    ghost.recentlyAttackedPlayerUUID
            );
        }
    }

    /**
     * 读取目标系统状态。
     */
    public static void load(
            CallingGhost ghost,
            CompoundTag tag
    ) {
        if (tag.hasUUID(
                NBT_TARGET_PLAYER
        )) {
            ghost.targetPlayerUUID =
                    tag.getUUID(
                            NBT_TARGET_PLAYER
                    );
        }

        ghost.attackTargetCooldown =
                Math.max(
                        0,
                        tag.getInt(
                                NBT_ATTACK_TARGET_COOLDOWN
                        )
                );

        if (tag.hasUUID(
                NBT_RECENTLY_ATTACKED_PLAYER
        )) {
            ghost.recentlyAttackedPlayerUUID =
                    tag.getUUID(
                            NBT_RECENTLY_ATTACKED_PLAYER
                    );
        }
    }
}