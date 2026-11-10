package com.qidate.qisplan2.ghost.module.implementation.calling;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.GhostAvoidanceSystem;
import com.qidate.qisplan2.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;

/** 目标查询与鬼石砖、关闭鬼门的阻隔检测；查询不改变模块状态。 */
public final class CallingGhostTargets {
    private static final double BLOCK_CHECK_STEP = 0.5D;

    /**
     * 查询当前模块记录的有效运行时对象，不存在时返回 null。
     */
    public static ServerPlayer current(ModularGhostEntity host) {
        var data = CallingGhostState.read(host);
        if (!(host.level() instanceof ServerLevel level) || !data.hasUUID("target")) return null;
        var player = level.getPlayerByUUID(data.getUUID("target"));
        return player instanceof ServerPlayer serverPlayer && valid(host, serverPlayer) ? serverPlayer : null;
    }

    /**
     * 检查候选玩家是否存活、同维度且可以被当前厉鬼探测。
     */
    public static boolean valid(ModularGhostEntity host, ServerPlayer player) {
        return player.level() == host.level() && player.isAlive() && !player.isCreative()
                && !player.isSpectator() && host.canDetectPlayer(player);
    }

    /**
     * 寻找可追踪目标，优先考虑红烛吸引，再沿用当前目标或筛选其他玩家。
     * 同时考虑阻隔和最近袭击后的目标避让冷却。
     */
    public static ServerPlayer available(ModularGhostEntity host) {
        var preferred = GhostAvoidanceSystem.preferredTarget(host, 32,
                p -> p instanceof ServerPlayer server && valid(host, server) && canTrackPlayer(host, server));
        if (preferred instanceof ServerPlayer serverPlayer) return serverPlayer;
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

    /**
     * 把实体来源的位置转换成通用追踪检测输入，复用阻隔查询。
     */
    public static boolean canTrackPlayer(
            ModularGhostEntity ghost,
            ServerPlayer player
    ) {
        return canTrack(ghost.level(), ghost.position(), player);
    }

    /**
     * 校验目标资格与空间阻隔，判断是否允许继续追踪。
     */
    public static boolean canTrack(Level level, Vec3 origin,
                                   LivingEntity player) {
        Vec3 start =
                origin.add(
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
                    level.getBlockState(
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
