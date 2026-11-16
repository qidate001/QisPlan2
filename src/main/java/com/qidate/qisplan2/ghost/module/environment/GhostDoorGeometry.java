package com.qidate.qisplan2.ghost.module.environment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.function.Predicate;

/** 门坐标系几何计算。只处理方向、空间与阻断事实，不读取任何载体。 */
public final class GhostDoorGeometry {
    /** 几何工具无需实例化。 */
    private GhostDoorGeometry() {}
    /** 两扇门的正面相连：沿来源门法线进入，沿目的门相反法线离开。 */
    public static float rotation(Direction origin, Direction destination) {
        return Mth.wrapDegrees(destination.toYRot() - origin.toYRot() + 180);
    }
    /** 按 Minecraft 水平偏航角旋转向量，保留竖直分量。 */
    public static Vec3 rotate(Vec3 vector, float yaw) {
        double radians = Math.toRadians(yaw), cos = Math.cos(radians), sin = Math.sin(radians);
        return new Vec3(vector.x*cos-vector.z*sin, vector.y, vector.x*sin+vector.z*cos);
    }
    /** 根据实际入门运动判定出口侧；静止释放时使用视线判定，不强制所有人从同侧离开。 */
    public static Vec3 exitPosition(BlockPos destination, Direction originFacing, Direction destinationFacing,
            Vec3 movement, Vec3 look) {
        Vec3 normal = Vec3.atLowerCornerOf(originFacing.getNormal());
        double entry = movement.dot(normal);
        if (Math.abs(entry) < 1.0E-5) entry = look.dot(normal);
        double exitSide = entry < 0 ? 1 : -1;
        return Vec3.atBottomCenterOf(destination).add(Vec3.atLowerCornerOf(destinationFacing.getNormal()).scale(exitSide*.85));
    }
    /**
     * 沿门法线形成一格宽、两格高的直线体积，检测实体碰撞箱是否与之相交。
     * 门前门后独立截断；门所在的第零格不参与隔绝检测，避免关闭的鬼门阻断自身能力。
     */
    public static AABB attackBounds(BlockPos door, Direction facing, double range, Predicate<BlockPos> blocked) {
        if (!Double.isFinite(range) || range <= 0 || range > 128) throw new IllegalArgumentException("Invalid door range");
        double positive = reach(door, facing, range, blocked);
        double negative = reach(door, facing.getOpposite(), range, blocked);
        Vec3 center = door.getCenter();
        if (facing.getAxis() == Direction.Axis.X) {
            double low = facing.getStepX() > 0 ? negative : positive;
            double high = facing.getStepX() > 0 ? positive : negative;
            return new AABB(center.x-low, door.getY(), door.getZ(), center.x+high, door.getY()+2, door.getZ()+1);
        }
        double low = facing.getStepZ() > 0 ? negative : positive;
        double high = facing.getStepZ() > 0 ? positive : negative;
        return new AABB(door.getX(), door.getY(), center.z-low, door.getX()+1, door.getY()+2, center.z+high);
    }
    /** 门洞高度内任一格隔绝即截断该方向，终点停在隔绝方块靠近门的一面。 */
    private static double reach(BlockPos door, Direction direction, double range, Predicate<BlockPos> blocked) {
        for (int step = 1; step-.5 < range; step++) {
            var pos = door.relative(direction, step);
            if (blocked.test(pos) || blocked.test(pos.above())) return Math.min(range, step-.5);
        }
        return range;
    }
}
