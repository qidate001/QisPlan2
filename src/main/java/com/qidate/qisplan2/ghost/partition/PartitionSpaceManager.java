package com.qidate.qisplan2.ghost.partition;

import com.qidate.qisplan2.QisPlan2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

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

            generateRoom(
                    level,
                    regionId,
                    initialRoom
            );

            data.addRoom(
                    regionId,
                    initialRoom
            );
        }

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
         * 先生成新房间。
         */
        generateRoom(
                level,
                regionId,
                targetRoom
        );

        /*
         * 打通连接。
         */
        connectRooms(
                level,
                regionId,
                sourceRoom,
                targetRoom,
                direction
        );

        data.addRoom(
                regionId,
                targetRoom
        );

        return true;
    }

    /*
     * ============================================================
     * 生成房间
     * ============================================================
     */

    private static void generateRoom(
            ServerLevel level,
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

        int minX =
                center.getX()
                        - half;

        int minY =
                center.getY()
                        - half;

        int minZ =
                center.getZ()
                        - half;

        int maxX =
                minX
                        + ROOM_SIZE
                        - 1;

        int maxY =
                minY
                        + ROOM_SIZE
                        - 1;

        int maxZ =
                minZ
                        + ROOM_SIZE
                        - 1;

        Block wallBlock =
                QisPlan2.GHOST_LEATHER_WALL.get();

        /*
         * 只生成外壳。
         */
        for (int x = minX;
             x <= maxX;
             x++) {

            for (int y = minY;
                 y <= maxY;
                 y++) {

                for (int z = minZ;
                     z <= maxZ;
                     z++) {

                    boolean shell =
                            x == minX
                                    || x == maxX
                                    || y == minY
                                    || y == maxY
                                    || z == minZ
                                    || z == maxZ;

                    if (!shell) {
                        continue;
                    }

                    level.setBlock(
                            new BlockPos(
                                    x,
                                    y,
                                    z
                            ),
                            wallBlock.defaultBlockState(),
                            3
                    );
                }
            }
        }
    }

    /*
     * ============================================================
     * 打通房间
     * ============================================================
     */

    private static void connectRooms(
            ServerLevel level,
            long regionId,
            PartitionRoomPos sourceRoom,
            PartitionRoomPos targetRoom,
            Direction direction
    ) {

        BlockPos sourceCenter =
                getRoomCenter(
                        regionId,
                        sourceRoom
                );

        /*
         * 两个房间中心之间，
         * 间距正好 10。
         *
         * 共用的墙面就在中心之间。
         */

        BlockPos wallCenter =
                sourceCenter.relative(
                        direction,
                        ROOM_SIZE / 2
                );

        /*
         * 做一个中央 3×4 门洞。
         *
         * 这里只是第一版，
         * 以后可以做成不同大小。
         */
        int halfWidth = 1;

        int halfHeight = 2;

        Direction.Axis axis =
                direction.getAxis();

        for (int a = -halfWidth;
             a <= halfWidth;
             a++) {

            for (int b = 0;
                 b <= halfHeight;
                 b++) {

                BlockPos pos;

                if (axis == Direction.Axis.X) {

                    pos =
                            wallCenter.offset(
                                    0,
                                    b - 1,
                                    a
                            );

                } else if (axis == Direction.Axis.Z) {

                    pos =
                            wallCenter.offset(
                                    a,
                                    b - 1,
                                    0
                            );

                } else {

                    /*
                     * 上下扩展：
                     *
                     * 暂时使用水平 3×3 洞。
                     */
                    pos =
                            wallCenter.offset(
                                    a,
                                    0,
                                    b - 1
                            );
                }

                level.removeBlock(
                        pos,
                        false
                );
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