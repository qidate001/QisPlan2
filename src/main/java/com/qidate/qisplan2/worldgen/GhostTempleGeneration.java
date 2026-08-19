package com.qidate.qisplan2.worldgen;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.util.StructureUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.Random;

public class GhostTempleGeneration {

    /**
     * 一个生成区域的大小。
     *
     * 512 方块 = 32 个区块
     */
    private static final int REGION_SIZE = 32;

    /**
     * 生成概率。
     *
     * 4 = 1 / 4
     * 也就是 25%
     */
    private static final int TEMPLE_CHANCE = 4;


    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {

        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        // 目前只在主世界生成
        if (level.dimension() != ServerLevel.OVERWORLD) {
            return;
        }

        /*
         * 当前区块坐标
         */
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;

        /*
         * 把世界划分成 32×32 区块的大区域。
         */
        int regionX =
                Math.floorDiv(chunkX, REGION_SIZE);

        int regionZ =
                Math.floorDiv(chunkZ, REGION_SIZE);

        /*
         * 只有这个区域的“候选区块”负责触发生成。
         *
         * 这样一个区域只会检查一次，而不是
         * 里面 1024 个区块全部检查。
         */
        int localX =
                Math.floorMod(chunkX, REGION_SIZE);

        int localZ =
                Math.floorMod(chunkZ, REGION_SIZE);

        if (localX != 0 || localZ != 0) {
            return;
        }

        /*
         * 根据：
         *
         * 世界种子
         * +
         * 区域坐标
         *
         * 创建稳定随机数。
         */
        long seed =
                level.getSeed();

        long regionSeed =
                seed
                        + regionX * 341873128712L
                        + regionZ * 132897987541L;

        Random random =
                new Random(regionSeed);

        /*
         * 决定这个区域有没有鬼庙。
         */
        if (random.nextInt(TEMPLE_CHANCE) != 0) {
            return;
        }

        /*
         * 在这个 512×512 区域中选择一个固定位置。
         *
         * 注意：这是由世界种子决定的，
         * 所以每次都会得到完全一样的位置。
         */
        int targetChunkX =
                regionX * REGION_SIZE
                        + random.nextInt(REGION_SIZE);

        int targetChunkZ =
                regionZ * REGION_SIZE
                        + random.nextInt(REGION_SIZE);

        /*
         * 转换成方块坐标。
         */
        int x =
                targetChunkX * 16
                        + random.nextInt(16);

        int z =
                targetChunkZ * 16
                        + random.nextInt(16);

        /*
         * 找到地面高度。
         */
        int y =
                level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
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
         * 生成鬼庙。
         */
        boolean success =
                StructureUtil.placeStructure(
                        level,
                        pos,
                        "qisplan2:ghost_temple"
                );

        QisPlan2.LOGGER.info(
                "[QisPlan2] 鬼庙生成结果: {}",
                success
        );
    }
}