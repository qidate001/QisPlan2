package com.qidate.qisplan2.worldgen;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.util.StructureUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.Random;

public class GhostTempleGeneration {

    /**
     * 一个生成区域 = 32 × 32 个区块
     *
     * 也就是 512 × 512 方块。
     */
    private static final int REGION_SIZE = 32;

    /**
     * 每个区域生成鬼庙的概率。
     *
     * 1 = 100%
     * 4 = 25%
     * 20 = 5%
     */
    private static final int TEMPLE_CHANCE = 4;

    /**
     * 鬼庙结构尺寸。
     */
    private static final int STRUCTURE_WIDTH = 48;
    private static final int STRUCTURE_DEPTH = 48;

    /**
     * 允许的最大地形高度差。
     *
     * 例如：
     * 0 = 必须完全平
     * 3 = 48×48 范围最高点和最低点最多相差 3 格
     */
    private static final int MAX_HEIGHT_DIFFERENCE = 3;


    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {

        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        /*
         * 只允许主世界。
         */
        if (level.dimension() != ServerLevel.OVERWORLD) {
            return;
        }


        /*
         * 当前区块坐标
         */
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;


        /*
         * ========================================
         * 1. 确定所属 512×512 区域
         * ========================================
         */

        int regionX =
                Math.floorDiv(chunkX, REGION_SIZE);

        int regionZ =
                Math.floorDiv(chunkZ, REGION_SIZE);

        int localX =
                Math.floorMod(chunkX, REGION_SIZE);

        int localZ =
                Math.floorMod(chunkZ, REGION_SIZE);


        /*
         * 每个区域只有一个区块负责进行判定。
         */
        if (localX != 0 || localZ != 0) {
            return;
        }


        /*
         * ========================================
         * 2. 检查这个区域是否已经处理
         * ========================================
         */

        Boolean checked =
                chunk.getData(
                        QisPlan2.GHOST_TEMPLE_GENERATED
                );

        if (Boolean.TRUE.equals(checked)) {
            return;
        }


        /*
         * ========================================
         * 3. 根据世界种子生成固定随机数
         * ========================================
         */

        long worldSeed =
                level.getSeed();

        long regionSeed =
                worldSeed
                        + regionX * 341873128712L
                        + regionZ * 132897987541L;

        Random random =
                new Random(regionSeed);


        /*
         * ========================================
         * 4. 判断这个区域是否生成鬼庙
         * ========================================
         */

        if (random.nextInt(TEMPLE_CHANCE) != 0) {

            // 这个区域确定没有鬼庙。
            chunk.setData(
                    QisPlan2.GHOST_TEMPLE_GENERATED,
                    true
            );

            return;
        }


        /*
         * ========================================
         * 5. 随机决定候选位置
         * ========================================
         */

        int targetChunkX =
                regionX * REGION_SIZE
                        + random.nextInt(REGION_SIZE);

        int targetChunkZ =
                regionZ * REGION_SIZE
                        + random.nextInt(REGION_SIZE);

        int x =
                targetChunkX * 16
                        + random.nextInt(16);

        int z =
                targetChunkZ * 16
                        + random.nextInt(16);


        /*
         * ========================================
         * 6. 找到地面
         * ========================================
         */

        int y =
                level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        x,
                        z
                );

        BlockPos pos =
                new BlockPos(x, y, z);


        QisPlan2.LOGGER.info(
                "[QisPlan2] 鬼庙候选位置: {} {} {}",
                x,
                y,
                z
        );


        /*
         * ========================================
         * 7. 地形检查
         * ========================================
         */

        if (!isValidTerrain(level, pos)) {

            QisPlan2.LOGGER.info(
                    "[QisPlan2] 鬼庙位置地形不符合要求，取消生成: {}",
                    pos
            );

            /*
             * 目前：
             *
             * 这个区域已经完成判定。
             */
            chunk.setData(
                    QisPlan2.GHOST_TEMPLE_GENERATED,
                    true
            );

            return;
        }


        /*
         * ========================================
         * 8. 生成鬼庙
         * ========================================
         */

        boolean success =
                StructureUtil.placeStructure(
                        level,
                        pos,
                        "qisplan2:ghost_temple"
                );


        /*
         * ========================================
         * 9. 记录结果
         * ========================================
         */

        if (success) {

            QisPlan2.LOGGER.info(
                    "[QisPlan2] 鬼庙生成成功！位置: {}",
                    pos
            );

            chunk.setData(
                    QisPlan2.GHOST_TEMPLE_GENERATED,
                    true
            );

        } else {

            QisPlan2.LOGGER.error(
                    "[QisPlan2] 鬼庙生成失败！位置: {}",
                    pos
            );

            /*
             * 生成失败暂时不标记。
             *
             * 这样以后还有机会重新尝试。
             */
        }
    }


    /**
     * 检查鬼庙是否适合生成在这个位置。
     */
    private static boolean isValidTerrain(
            ServerLevel level,
            BlockPos pos
    ) {

        final int width = 48;
        final int depth = 48;

        int startX = pos.getX();
        int startZ = pos.getZ();

        int endX = startX + width - 1;
        int endZ = startZ + depth - 1;


        /*
         * ========================================
         * 1. 检查鬼庙涉及的所有区块是否已经加载
         * ========================================
         */

        int minChunkX = startX >> 4;
        int maxChunkX = endX >> 4;

        int minChunkZ = startZ >> 4;
        int maxChunkZ = endZ >> 4;

        for (int chunkX = minChunkX;
             chunkX <= maxChunkX;
             chunkX++) {

            for (int chunkZ = minChunkZ;
                 chunkZ <= maxChunkZ;
                 chunkZ++) {

                if (!level.hasChunk(
                        chunkX,
                        chunkZ
                )) {

                    QisPlan2.LOGGER.info(
                            "[QisPlan2] 鬼庙范围存在未加载区块: {}, {}",
                            chunkX,
                            chunkZ
                    );

                    return false;
                }
            }
        }


        /*
         * ========================================
         * 2. 只采样少量位置
         * ========================================
         *
         * 不再扫描 48×48 = 2304 个方块。
         *
         * 检查：
         *
         * 四个角
         * 四条边的中点
         * 中心
         */

        int centerX = startX + width / 2;
        int centerZ = startZ + depth / 2;

        int[][] samples = {

                // 四角
                {startX, startZ},
                {endX, startZ},
                {startX, endZ},
                {endX, endZ},

                // 四边中点
                {centerX, startZ},
                {centerX, endZ},
                {startX, centerZ},
                {endX, centerZ},

                // 中心
                {centerX, centerZ}
        };


        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;


        /*
         * ========================================
         * 3. 检查采样点
         * ========================================
         */

        for (int[] sample : samples) {

            int x = sample[0];
            int z = sample[1];

            /*
             * 因为我们已经确认区块加载，
             * 所以这里不会主动生成远处新区块。
             */

            int y = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    x,
                    z
            );


            /*
             * 如果高度已经接近世界底部，
             * 基本可以认为这里没有正常地面。
             */

            if (y <= level.getMinBuildHeight() + 1) {

                QisPlan2.LOGGER.info(
                        "[QisPlan2] 发现异常高度: {}",
                        y
                );

                return false;
            }


            /*
             * 记录高度
             */

            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);


            /*
             * ====================================
             * 检查地面液体
             * ====================================
             */

            BlockPos groundPos =
                    new BlockPos(
                            x,
                            y - 1,
                            z
                    );

            if (!level.getFluidState(
                    groundPos
            ).isEmpty()) {

                QisPlan2.LOGGER.info(
                        "[QisPlan2] 地面存在液体: {}",
                        groundPos
                );

                return false;
            }


            /*
             * ====================================
             * 检查地面上方
             * ====================================
             */

            BlockPos above =
                    new BlockPos(
                            x,
                            y,
                            z
                    );

            if (!level.getFluidState(
                    above
            ).isEmpty()) {

                QisPlan2.LOGGER.info(
                        "[QisPlan2] 地面上方存在液体"
                );

                return false;
            }
        }


        /*
         * ========================================
         * 4. 检查高度差
         * ========================================
         */

        int heightDifference =
                maxY - minY;

        if (heightDifference > 3) {

            QisPlan2.LOGGER.info(
                    "[QisPlan2] 地形太陡，高度差: {}",
                    heightDifference
            );

            return false;
        }


        /*
         * ========================================
         * 5. 检查通过
         * ========================================
         */

        QisPlan2.LOGGER.info(
                "[QisPlan2] 地形检查通过，高度: {} ~ {}",
                minY,
                maxY
        );

        return true;
    }
}