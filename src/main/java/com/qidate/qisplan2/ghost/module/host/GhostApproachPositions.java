package com.qidate.qisplan2.ghost.module.host;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
/** 只负责世界碰撞与候选位置，不包含门鬼技能、复苏或伤害结算。 */
public final class GhostApproachPositions {
    private GhostApproachPositions() {}

    /** 在目标周围寻找可站立位置，交由调用方决定是否移动。 */
    public static BlockPos findApproachPosition(
            ServerLevel level,
            LivingEntity target
    ) {

        BlockPos center = target.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;


        /*
         * 四个水平面。
         */
        for (Direction direction :
                Direction.Plane.HORIZONTAL) {

            BlockPos candidate = center.relative(direction);


            /*
             * ====================================================
             * 必须能站进去。
             * ====================================================
             */

            if (!level.getBlockState(candidate).getCollisionShape(level, candidate).isEmpty()) {
                continue;
            }


            /*
             * ====================================================
             * 脚下必须有支撑。
             * ====================================================
             */

            BlockPos below = candidate.below();

            if (level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                continue;
            }


            /*
             * ====================================================
             * 选距离目标最近的位置。
             * ====================================================
             */

            double distance =
                    target.distanceToSqr(
                            candidate.getX() + 0.5D,
                            candidate.getY(),
                            candidate.getZ() + 0.5D
                    );

            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }


        /*
         * ========================================================
         * 找不到合适站位
         *
         * 直接用目标方块位置作为兜底。
         * ========================================================
         */

        if (best == null) {
            return center;
        }

        return best;
    }


}
