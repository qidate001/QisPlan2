package com.qidate.qisplan2.ghost.partition;

import com.qidate.qisplan2.QisPlan2;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

public final class PartitionReturnManager {

    private PartitionReturnManager() {
    }

    /**
     * 记录玩家进入划分维度之前的位置。
     */
    public static void capture(
            ServerPlayer player
    ) {

        PartitionReturnData data =
                new PartitionReturnData(
                        true,
                        player.level()
                                .dimension(),
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        player.getYRot(),
                        player.getXRot()
                );

        player.setData(
                QisPlan2.PARTITION_RETURN_DATA,
                data
        );
    }

    /**
     * 玩家是否拥有有效的返回位置。
     */
    public static boolean hasReturnPoint(
            ServerPlayer player
    ) {

        PartitionReturnData data =
                player.getData(
                        QisPlan2.PARTITION_RETURN_DATA
                );

        return data.valid();
    }

    /**
     * 从划分维度返回原位置。
     *
     * @return 是否成功返回
     */
    public static boolean returnPlayer(
            ServerPlayer player
    ) {

        PartitionReturnData data =
                player.getData(
                        QisPlan2.PARTITION_RETURN_DATA
                );

        if (!data.valid()) {
            return false;
        }

        /*
         * ========================================================
         * 获取目标维度
         * ========================================================
         */

        ServerLevel targetLevel =
                player.server.getLevel(
                        data.dimension()
                );

        if (targetLevel == null) {

            QisPlan2.LOGGER.error(
                    "[QisPlan2] 无法返回：找不到维度 {}",
                    data.dimension()
            );

            return false;
        }

        /*
         * ========================================================
         * 创建传送
         * ========================================================
         */

        DimensionTransition transition =
                new DimensionTransition(
                        targetLevel,

                        new Vec3(
                                data.x(),
                                data.y(),
                                data.z()
                        ),

                        Vec3.ZERO,

                        data.yaw(),
                        data.pitch(),

                        DimensionTransition.DO_NOTHING
                );

        player.changeDimension(
                transition
        );

        /*
         * ========================================================
         * 返回成功以后清除返回点。
         *
         * 防止这个返回点被重复使用。
         * ========================================================
         */

        player.setData(
                QisPlan2.PARTITION_RETURN_DATA,
                PartitionReturnData.EMPTY
        );

        return true;
    }
}