package com.qidate.qisplan2.ghost.module.implementation.calling;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** 目标查询与鬼石砖、关闭鬼门的阻隔检测；查询不改变模块状态。 */
final class CallingGhostTargets {
    private static final double BLOCK_CHECK_STEP = 0.5D;
    static ServerPlayer current(ModularGhostEntity host) {
        var data = CallingGhostState.read(host);
        if (!(host.level() instanceof ServerLevel level) || !data.hasUUID("target")) return null;
        var player = level.getPlayerByUUID(data.getUUID("target"));
        return player instanceof ServerPlayer serverPlayer && valid(host, serverPlayer) ? serverPlayer : null;
    }
    static boolean valid(ModularGhostEntity host, ServerPlayer player) {
        return player.level() == host.level() && player.isAlive() && !player.isCreative()
                && !player.isSpectator() && host.canDetectPlayer(player);
    }
    static ServerPlayer available(ModularGhostEntity host) {
        ServerPlayer current = current(host);
        // 被阻隔时保留原目标，但不占用移动控制。
        if (current != null) return canTrackPlayer(host, current) ? current : null;
        if (!(host.level() instanceof ServerLevel level)) return null;
        var data = CallingGhostState.read(host);
        ServerPlayer best = null;
        int priority = Integer.MIN_VALUE;
        double distance = Double.MAX_VALUE;
        for (var player : level.players()) {
            double squared = host.distanceToSqr(player);
            if (squared > 64 * 64 || !valid(host, player) || !canTrackPlayer(host, player)) continue;
            if (data.getInt("attackTargetCooldown") > 0 && data.hasUUID("recentlyAttacked")
                    && data.getUUID("recentlyAttacked").equals(player.getUUID())) continue;
            int candidatePriority = host.getPlayerTargetPriority(player);
            if (candidatePriority > priority || (candidatePriority == priority && squared < distance)) {
                best = player;
                priority = candidatePriority;
                distance = squared;
            }
        }
        return best;
    }
    public static boolean canTrackPlayer(
            ModularGhostEntity ghost,
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

}
