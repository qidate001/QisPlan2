package com.qidate.qisplan2.ghost.partition;

import com.qidate.qisplan2.QisPlan2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PartitionSpaceManager {

    /**
     * 一个独立区域之间的间距。
     *
     * 200 × 200 chunk。
     */
    public static final int REGION_CHUNKS = 200;

    public static final int REGION_BLOCKS =
            REGION_CHUNKS * 16;

    /**
     * 房间尺寸。
     */
    public static final int ROOM_SIZE = 11;

    /**
     * 相邻房间共用一面墙。
     *
     * 因此中心间距为：
     *
     * 11 - 1 = 10
     */
    public static final int ROOM_STEP =
            ROOM_SIZE - 1;

    /**
     * 房间中心高度。
     */
    public static final int ROOM_CENTER_Y = 65;

    private PartitionSpaceManager() {
    }

    /*
     * ============================================================
     * 区域
     * ============================================================
     */

    public static int getRegionX(
            long regionId
    ) {

        return Math.floorMod(
                Math.toIntExact(regionId),
                1000
        );
    }

    public static int getRegionZ(
            long regionId
    ) {

        return Math.floorDiv(
                Math.toIntExact(regionId),
                1000
        );
    }

    public static double getRegionCenterX(
            long regionId
    ) {

        return getRegionX(regionId)
                * REGION_BLOCKS
                + REGION_BLOCKS / 2.0D;
    }

    public static double getRegionCenterZ(
            long regionId
    ) {

        return getRegionZ(regionId)
                * REGION_BLOCKS
                + REGION_BLOCKS / 2.0D;
    }

    /*
     * ============================================================
     * 房间
     * ============================================================
     */

    public static BlockPos getRoomCenter(
            long regionId,
            PartitionRoomPos room
    ) {

        int regionCenterX =
                Mth.floor(
                        getRegionCenterX(
                                regionId
                        )
                );

        int regionCenterZ =
                Mth.floor(
                        getRegionCenterZ(
                                regionId
                        )
                );

        return new BlockPos(
                regionCenterX
                        + room.x()
                        * ROOM_STEP,

                ROOM_CENTER_Y
                        + room.y()
                        * ROOM_STEP,

                regionCenterZ
                        + room.z()
                        * ROOM_STEP
        );
    }

    /**
     * 获取房间外壳最小坐标。
     */
    public static BlockPos getRoomMin(
            long regionId,
            PartitionRoomPos room
    ) {

        BlockPos center =
                getRoomCenter(
                        regionId,
                        room
                );

        int half =
                ROOM_SIZE / 2;

        return center.offset(
                -half,
                -half,
                -half
        );
    }

    /*
     * ============================================================
     * 初始房间
     * ============================================================
     */

    public static void ensureInitialRoom(
            ServerLevel level,
            long regionId
    ) {

        MinecraftServer server =
                level.getServer();

        PartitionSpaceSavedData data =
                PartitionSpaceSavedData.get(
                        server
                );

        PartitionRoomPos initialRoom =
                new PartitionRoomPos(
                        0,
                        0,
                        0
                );

        /*
         * ========================================================
         * 如果房间还不存在：
         *
         * 先创建房间。
         * ========================================================
         */

        if (!data.hasRoom(
                regionId,
                initialRoom
        )) {

            data.addRoom(
                    regionId,
                    initialRoom
            );
        }

        rebuildSpaceGeometry(
                level,
                regionId
        );

        /*
         * ========================================================
         * 无论房间是不是旧房间，
         * 都检查中央出口。
         *
         * 这样可以自动修复以前生成的旧空间。
         * ========================================================
         */

        BlockPos center =
                getRoomCenter(
                        regionId,
                        initialRoom
                );

        BlockState state =
                level.getBlockState(
                        center
                );

        if (!state.is(
                QisPlan2.PARTITION_EXIT.get()
        )) {

            level.setBlock(
                    center,
                    QisPlan2.PARTITION_EXIT
                            .get()
                            .defaultBlockState(),
                    3
            );

            QisPlan2.LOGGER.info(
                    "[QisPlan2] 补充划分空间出口：regionId={}，pos={}",
                    regionId,
                    center
            );
        }
    }

    /*
     * ============================================================
     * 扩展房间
     * ============================================================
     */

    public static boolean expandRoom(
            ServerLevel level,
            long regionId,
            PartitionRoomPos sourceRoom,
            Direction direction
    ) {

        MinecraftServer server =
                level.getServer();

        PartitionSpaceSavedData data =
                PartitionSpaceSavedData.get(
                        server
                );

        /*
         * ========================================================
         * 目标房间
         * ========================================================
         */

        PartitionRoomPos targetRoom =
                sourceRoom.relative(
                        direction
                );

        /*
         * 已经存在。
         */
        if (data.hasRoom(
                regionId,
                targetRoom
        )) {

            return false;
        }

        /*
         * ========================================================
         * 记录新房间。
         *
         * 注意：
         *
         * 不再单独 generateRoom。
         *
         * 后面由 rebuildSpaceGeometry()
         * 根据整个空间统一生成。
         * ========================================================
         */

        data.addRoom(
                regionId,
                targetRoom
        );

        /*
         * ========================================================
         * 根据整个 Space 重新计算墙壁。
         * ========================================================
         */

        rebuildSpaceGeometry(
                level,
                regionId
        );

        return true;
    }

    /*
     * ============================================================
     * 生成房间
     * ============================================================
     */

    private static void rebuildSpaceGeometry(
            ServerLevel level,
            long regionId
    ) {

        PartitionSpaceSavedData data =
                PartitionSpaceSavedData.get(
                        level.getServer()
                );

        Set<PartitionRoomPos> rooms =
                data.getRooms(
                        regionId
                );

        if (rooms.isEmpty()) {
            return;
        }

        /*
         * ========================================================
         * 第一阶段：
         *
         * 收集所有房间占据的方块。
         *
         * 每个房间：
         *
         * 11 × 11 × 11
         *
         * 相邻房间：
         *
         * 中心间距 10
         *
         * 所以相邻房间会共享一层方块。
         * ========================================================
         */

        Set<Long> occupied =
                new HashSet<>();

        for (PartitionRoomPos room :
                rooms) {

            BlockPos center =
                    getRoomCenter(
                            regionId,
                            room
                    );

            int half =
                    ROOM_SIZE / 2;

            for (int x = -half;
                 x <= half;
                 x++) {

                for (int y = -half;
                     y <= half;
                     y++) {

                    for (int z = -half;
                         z <= half;
                         z++) {

                        BlockPos pos =
                                center.offset(
                                        x,
                                        y,
                                        z
                                );

                        occupied.add(
                                pos.asLong()
                        );
                    }
                }
            }
        }

        /*
         * ========================================================
         * 第二阶段：
         *
         * 根据“房间并集”重新计算边界。
         *
         * 一个方块：
         *
         * 只要它的六个方向中有一个方向
         * 不属于房间并集，
         *
         * 它就是外墙。
         *
         * 否则：
         *
         * 它就是内部空气。
         * ========================================================
         */

        for (PartitionRoomPos room :
                rooms) {

            BlockPos center =
                    getRoomCenter(
                            regionId,
                            room
                    );

            int half =
                    ROOM_SIZE / 2;

            for (int x = -half;
                 x <= half;
                 x++) {

                for (int y = -half;
                     y <= half;
                     y++) {

                    for (int z = -half;
                         z <= half;
                         z++) {

                        BlockPos pos =
                                center.offset(
                                        x,
                                        y,
                                        z
                                );

                        long packed =
                                pos.asLong();

                        /*
                         * 六个方向。
                         */
                        boolean outside =
                                !occupied.contains(
                                        pos.relative(
                                                Direction.NORTH
                                        ).asLong()
                                )
                                        || !occupied.contains(
                                        pos.relative(
                                                Direction.SOUTH
                                        ).asLong()
                                )
                                        || !occupied.contains(
                                        pos.relative(
                                                Direction.EAST
                                        ).asLong()
                                )
                                        || !occupied.contains(
                                        pos.relative(
                                                Direction.WEST
                                        ).asLong()
                                )
                                        || !occupied.contains(
                                        pos.relative(
                                                Direction.UP
                                        ).asLong()
                                )
                                        || !occupied.contains(
                                        pos.relative(
                                                Direction.DOWN
                                        ).asLong()
                                );

                        BlockState current =
                                level.getBlockState(
                                        pos
                                );

                        /*
                         * ====================================================
                         * 外墙
                         * ====================================================
                         */

                        if (outside) {

                            /*
                             * 不覆盖特殊方块。
                             *
                             * 比如出口。
                             */
                            if (current.isAir()
                                    || current.is(
                                    QisPlan2.GHOST_LEATHER_WALL.get()
                            )) {

                                level.setBlock(
                                        pos,
                                        QisPlan2
                                                .GHOST_LEATHER_WALL
                                                .get()
                                                .defaultBlockState(),
                                        3
                                );
                            }

                            continue;
                        }

                        /*
                         * ====================================================
                         * 内部空间：
                         *
                         * 如果当前是鬼皮墙，
                         * 就把它拆掉。
                         *
                         * 其他特殊方块保留。
                         * ====================================================
                         */

                        if (current.is(
                                QisPlan2.GHOST_LEATHER_WALL.get()
                        )) {

                            level.removeBlock(
                                    pos,
                                    false
                            );
                        }
                    }
                }
            }
        }
    }

    public static long findRegionId(
            BlockPos pos
    ) {

        int regionX =
                Math.floorDiv(
                        pos.getX(),
                        REGION_BLOCKS
                );

        int regionZ =
                Math.floorDiv(
                        pos.getZ(),
                        REGION_BLOCKS
                );

        return (long) regionZ * 1000L
                + regionX;
    }

    public static PartitionRoomPos findRoomContaining(
            ServerLevel level,
            BlockPos pos
    ) {

        /*
         * ========================================================
         * 根据坐标粗略估算所在房间。
         *
         * 因为相邻房间共享一面墙，
         * 所以不能简单使用 round()。
         *
         * 我们直接检查附近几个候选房间，
         * 哪个房间真正包含这个方块，就使用哪个。
         * ========================================================
         */

        long regionId =
                findRegionId(pos);

        PartitionSpaceSavedData data =
                PartitionSpaceSavedData.get(
                        level.getServer()
                );

        /*
         * ========================================================
         * 先根据相对位置得到一个大致中心。
         * ========================================================
         */

        int regionCenterX =
                Mth.floor(
                        getRegionCenterX(
                                regionId
                        )
                );

        int regionCenterZ =
                Mth.floor(
                        getRegionCenterZ(
                                regionId
                        )
                );

        int relativeX =
                pos.getX()
                        - regionCenterX;

        int relativeY =
                pos.getY()
                        - ROOM_CENTER_Y;

        int relativeZ =
                pos.getZ()
                        - regionCenterZ;

        int estimatedX =
                Math.floorDiv(
                        relativeX + ROOM_STEP / 2,
                        ROOM_STEP
                );

        int estimatedY =
                Math.floorDiv(
                        relativeY + ROOM_STEP / 2,
                        ROOM_STEP
                );

        int estimatedZ =
                Math.floorDiv(
                        relativeZ + ROOM_STEP / 2,
                        ROOM_STEP
                );

        /*
         * ========================================================
         * 检查附近候选房间。
         *
         * 墙面可能属于边界，
         * 所以检查 ±1。
         * ========================================================
         */

        for (int x = estimatedX - 1;
             x <= estimatedX + 1;
             x++) {

            for (int y = estimatedY - 1;
                 y <= estimatedY + 1;
                 y++) {

                for (int z = estimatedZ - 1;
                     z <= estimatedZ + 1;
                     z++) {

                    PartitionRoomPos room =
                            new PartitionRoomPos(
                                    x,
                                    y,
                                    z
                            );

                    /*
                     * 这个房间必须真的存在。
                     */
                    if (!data.hasRoom(
                            regionId,
                            room
                    )) {
                        continue;
                    }

                    /*
                     * ====================================================
                     * 计算这个房间的完整边界。
                     * ====================================================
                     */

                    BlockPos min =
                            getRoomMin(
                                    regionId,
                                    room
                            );

                    BlockPos max =
                            min.offset(
                                    ROOM_SIZE - 1,
                                    ROOM_SIZE - 1,
                                    ROOM_SIZE - 1
                            );

                    /*
                     * ====================================================
                     * 判断方块是否位于房间内部。
                     *
                     * 注意这里允许边界：
                     *
                     * <= max
                     * ====================================================
                     */

                    if (pos.getX() >= min.getX()
                            && pos.getX() <= max.getX()
                            && pos.getY() >= min.getY()
                            && pos.getY() <= max.getY()
                            && pos.getZ() >= min.getZ()
                            && pos.getZ() <= max.getZ()) {

                        return room;
                    }
                }
            }
        }

        return null;
    }

    /**
     * 兼容旧代码：
     * 获取区域中心 X。
     */
    public static double getCenterX(
            long regionId
    ) {

        return getRegionCenterX(
                regionId
        );
    }


    /**
     * 兼容旧代码：
     * 获取区域中心 Y。
     */
    public static double getCenterY() {

        return ROOM_CENTER_Y;
    }


    /**
     * 兼容旧代码：
     * 获取区域中心 Z。
     */
    public static double getCenterZ(
            long regionId
    ) {

        return getRegionCenterZ(
                regionId
        );
    }
}