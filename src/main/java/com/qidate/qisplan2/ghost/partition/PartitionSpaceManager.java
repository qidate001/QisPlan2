package com.qidate.qisplan2.ghost.partition;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;

/**
 * 划分维度区域管理。
 *
 * 每个逻辑区域：
 *
 * 200 × 200 chunk
 * = 3200 × 3200 block
 */
public final class PartitionSpaceManager {

    /**
     * 一个区域的宽度：
     *
     * 200 chunk。
     */
    public static final int REGION_CHUNKS = 200;

    /**
     * 一个 chunk = 16 block。
     */
    public static final int REGION_BLOCKS =
            REGION_CHUNKS * 16;

    /**
     * 区域内部初始盒子的中心 Y。
     */
    public static final int REGION_Y = 65;


    /*
     * ============================================================
     * 初始石盒
     * ============================================================
     */

    /**
     * 初始空间盒子尺寸。
     */
    public static final int STARTER_BOX_SIZE = 10;

    /**
     * 初始空间盒子的方块。
     */
    private static final net.minecraft.world.level.block.Block
            STARTER_BOX_BLOCK =
            QisPlan2.GHOST_LEATHER_WALL.get();


    private PartitionSpaceManager() {
    }


    /*
     * ============================================================
     * 区域坐标
     * ============================================================
     */

    /**
     * 区域 ID → 区域 X。
     */
    public static int getRegionX(
            long regionId
    ) {

        int gridSize = 1000;

        return Math.floorMod(
                Math.toIntExact(
                        regionId
                ),
                gridSize
        );
    }


    /**
     * 区域 ID → 区域 Z。
     */
    public static int getRegionZ(
            long regionId
    ) {

        int gridSize = 1000;

        return Math.floorDiv(
                Math.toIntExact(
                        regionId
                ),
                gridSize
        );
    }


    /**
     * 区域中心 X。
     */
    public static double getCenterX(
            long regionId
    ) {

        return getRegionX(
                regionId
        ) * REGION_BLOCKS
                + REGION_BLOCKS / 2.0D;
    }


    /**
     * 区域中心 Z。
     */
    public static double getCenterZ(
            long regionId
    ) {

        return getRegionZ(
                regionId
        ) * REGION_BLOCKS
                + REGION_BLOCKS / 2.0D;
    }


    /**
     * 区域中心 Y。
     */
    public static double getCenterY() {

        return REGION_Y;
    }


    /**
     * 获取区域中心方块位置。
     */
    public static BlockPos getCenterBlock(
            long regionId
    ) {

        return BlockPos.containing(
                getCenterX(regionId),
                getCenterY(),
                getCenterZ(regionId)
        );
    }


    /*
     * ============================================================
     * 初始空间生成
     * ============================================================
     */

    /**
     * 确保指定区域已经生成初始石盒。
     *
     * 一个区域只初始化一次。
     */
    public static void ensureRegionInitialized(
            ServerLevel partitionLevel,
            long regionId
    ) {

        MinecraftServer server =
                partitionLevel.getServer();


        PartitionSpaceSavedData data =
                PartitionSpaceSavedData.get(
                        server
                );


        /*
         * 已经初始化过。
         */
        if (data.isRegionInitialized(
                regionId
        )) {

            return;
        }


        /*
         * ========================================================
         * 计算石盒中心
         * ========================================================
         */

        int centerX =
                Mth.floor(
                        getCenterX(
                                regionId
                        )
                );

        int centerY =
                (int) getCenterY();

        int centerZ =
                Mth.floor(
                        getCenterZ(
                                regionId
                        )
                );


        /*
         * ========================================================
         * 10×10×10 空心石盒
         * ========================================================
         *
         * 外部尺寸：
         *
         * 10 × 10 × 10
         *
         * 内部：
         *
         * 8 × 8 × 8
         *
         * 全部石头。
         *
         * 玩家会出现在内部中心。
         * ========================================================
         */

        int minX =
                centerX
                        - STARTER_BOX_SIZE / 2;

        int minY =
                centerY
                        - STARTER_BOX_SIZE / 2;

        int minZ =
                centerZ
                        - STARTER_BOX_SIZE / 2;


        int maxX =
                minX
                        + STARTER_BOX_SIZE
                        - 1;

        int maxY =
                minY
                        + STARTER_BOX_SIZE
                        - 1;

        int maxZ =
                minZ
                        + STARTER_BOX_SIZE
                        - 1;


        for (int x = minX;
             x <= maxX;
             x++) {

            for (int y = minY;
                 y <= maxY;
                 y++) {

                for (int z = minZ;
                     z <= maxZ;
                     z++) {

                    /*
                     * 判断是否在盒子外壳。
                     *
                     * 六个面任意一个贴边，
                     * 就放石头。
                     */
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


                    partitionLevel.setBlock(
                            new BlockPos(
                                    x,
                                    y,
                                    z
                            ),
                            STARTER_BOX_BLOCK
                                    .defaultBlockState(),
                            3
                    );
                }
            }
        }

        /*
         * ========================================================
         * 中央出口
         * ========================================================
         */

        partitionLevel.setBlock(
                new BlockPos(
                        centerX,
                        minY + 1,
                        centerZ
                ),
                QisPlan2.PARTITION_EXIT.get()
                        .defaultBlockState(),
                3
        );


        /*
         * ========================================================
         * 标记为已经初始化
         * ========================================================
         */

        data.markRegionInitialized(
                regionId
        );
    }
}