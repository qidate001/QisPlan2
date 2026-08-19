package com.qidate.qisplan2.worldgen;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.util.StructureUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.Random;

public class GhostTempleGeneration {

    /**
     * 一个生成区域 = 32 × 32 个区块
     *
     * 也就是 512 × 512 方块
     */
    private static final int REGION_SIZE = 32;

    /**
     * 每个区域生成鬼庙的概率。
     *
     * 1 / 4 = 25%
     */
    private static final int TEMPLE_CHANCE = 4;


    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {

        // 只处理 LevelChunk
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        // 只处理服务端
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        // 只在主世界生成
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
         * 1. 判断这个区块是不是候选区块
         * ========================================
         *
         * 每 32×32 区块作为一个区域。
         *
         * 每个区域只让左下角（localX=0, localZ=0）
         * 的区块负责进行一次判断。
         */

        int regionX =
                Math.floorDiv(chunkX, REGION_SIZE);

        int regionZ =
                Math.floorDiv(chunkZ, REGION_SIZE);

        int localX =
                Math.floorMod(chunkX, REGION_SIZE);

        int localZ =
                Math.floorMod(chunkZ, REGION_SIZE);

        if (localX != 0 || localZ != 0) {
            return;
        }


        /*
         * ========================================
         * 2. 检查这个区块是否已经处理过
         * ========================================
         */

        Boolean generated =
                chunk.getData(
                        QisPlan2.GHOST_TEMPLE_GENERATED
                );

        if (Boolean.TRUE.equals(generated)) {

            // 已经处理过，绝对不再生成
            return;
        }


        /*
         * ========================================
         * 3. 标记为“已经检查过”
         * ========================================
         *
         * 注意：
         * 无论最后有没有鬼庙，这个区域都只检查一次。
         */

        chunk.setData(
                QisPlan2.GHOST_TEMPLE_GENERATED,
                true
        );


        /*
         * ========================================
         * 4. 根据世界种子计算固定随机数
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
         * 5. 判断这个区域有没有鬼庙
         * ========================================
         */

        if (random.nextInt(TEMPLE_CHANCE) != 0) {

            QisPlan2.LOGGER.debug(
                    "[QisPlan2] 区域 {}, {} 没有生成鬼庙",
                    regionX,
                    regionZ
            );

            return;
        }


        /*
         * ========================================
         * 6. 确定鬼庙所在区块
         * ========================================
         */

        int targetChunkX =
                regionX * REGION_SIZE
                        + random.nextInt(REGION_SIZE);

        int targetChunkZ =
                regionZ * REGION_SIZE
                        + random.nextInt(REGION_SIZE);


        /*
         * 在目标区块内部确定位置
         */
        int x =
                targetChunkX * 16
                        + random.nextInt(16);

        int z =
                targetChunkZ * 16
                        + random.nextInt(16);


        /*
         * ========================================
         * 7. 找地面
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
                "[QisPlan2] 发现鬼庙生成位置: {} {} {}",
                x,
                y,
                z
        );


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
         * 9. 输出结果
         * ========================================
         */

        if (success) {

            QisPlan2.LOGGER.info(
                    "[QisPlan2] 鬼庙生成成功！位置: {} {} {}",
                    x,
                    y,
                    z
            );

        } else {

            QisPlan2.LOGGER.error(
                    "[QisPlan2] 鬼庙生成失败！位置: {} {} {}",
                    x,
                    y,
                    z
            );
        }
    }
}