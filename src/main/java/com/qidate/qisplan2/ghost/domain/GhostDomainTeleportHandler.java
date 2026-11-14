package com.qidate.qisplan2.ghost.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class GhostDomainTeleportHandler {

    private static final double MAX_DISTANCE = 128.0D;

    private GhostDomainTeleportHandler() {
    }

    /** 保留内部通用调用，自动选择当前生效的己方鬼域。 */
    public static boolean teleport(ServerPlayer player) {
        return teleport(player, GhostDomainManager.get(player.serverLevel()).getOwnEffectiveDomain(player));
    }

    /** 模块技能指定其自身鬼域，避免误借用另一模块的鬼域能力。 */
    public static boolean teleport(ServerPlayer player, GhostDomain domain) {
        ServerLevel level = player.serverLevel();
        GhostDomainManager manager = GhostDomainManager.get(level);
        if (domain == null) {
            return false;
        }

        if (!domain.getBehavior().canTeleport(
                level,
                domain,
                player
        )) {
            return false;
        }

        Vec3 target =
                findTargetPosition(player);

        if (target == null) {
            return false;
        }

        if (!domain.contains(
                target.x,
                target.y,
                target.z
        )) {
            return false;
        }

        boolean success =
                domain.getBehavior().teleport(
                        level,
                        domain,
                        player,
                        target.x,
                        target.y,
                        target.z
                );

        if (!success) {
            return false;
        }

        manager.updatePositionImmediate(
                domain.getId(),
                player.getX(),
                player.getY(),
                player.getZ()
        );

        return true;
    }

    private static Vec3 findTargetPosition(
            ServerPlayer player
    ) {

        HitResult hit =
                player.pick(
                        MAX_DISTANCE,
                        1.0F,
                        false
                );

        if (hit.getType()
                != HitResult.Type.BLOCK) {

            return null;
        }

        BlockHitResult blockHit =
                (BlockHitResult) hit;

        BlockPos blockPos =
                blockHit.getBlockPos();

        BlockPos targetPos =
                blockPos.relative(
                        blockHit.getDirection()
                );

        return new Vec3(
                targetPos.getX() + 0.5D,
                targetPos.getY(),
                targetPos.getZ() + 0.5D
        );
    }
}