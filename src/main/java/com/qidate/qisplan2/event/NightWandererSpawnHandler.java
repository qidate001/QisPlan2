package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.entity.NightWanderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

@EventBusSubscriber(modid = QisPlan2.MODID)
public class NightWandererSpawnHandler {

    /**
     * 玩家周围最小生成距离。
     *
     * 要求大于 32 格。
     */
    private static final double MIN_DISTANCE = 40.0D;

    /**
     * 最大生成距离。
     */
    private static final double MAX_DISTANCE = 64.0D;

    /**
     * 每隔多少 tick 检查一次。
     *
     * 20 tick = 1 秒
     */
    private static final int CHECK_INTERVAL = 20;

    /**
     * 每个玩家附近最多存在多少只夜游鬼。
     */
    private static final int MAX_NEARBY = 1;

    /**
     * 一次寻找生成位置时最多尝试多少次。
     */
    private static final int MAX_ATTEMPTS = 32;


    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {

        /*
         * 每秒检查一次。
         */
        if (event.getServer().getTickCount() % CHECK_INTERVAL != 0) {
            return;
        }

        /*
         * 遍历所有服务器玩家。
         */
        for (ServerPlayer player :
                event.getServer().getPlayerList().getPlayers()) {

            ServerLevel level = player.serverLevel();

            /*
             * 只在主世界生成。
             */
            if (level.dimension() != ServerLevel.OVERWORLD) {
                continue;
            }

            /*
             * 白天不生成。
             */
            if (level.isDay()) {
                continue;
            }

            /*
             * 当前玩家附近已经存在的夜游鬼。
             */
            List<NightWanderer> nearby =
                    level.getEntitiesOfClass(
                            NightWanderer.class,
                            player.getBoundingBox()
                                    .inflate(MAX_DISTANCE),
                            entity -> entity.isAlive()
                    );

            /*
             * 达到数量上限。
             */
            if (nearby.size() >= MAX_NEARBY) {
                continue;
            }

            /*
             * 还缺多少只。
             */
            int amountToSpawn =
                    MAX_NEARBY - nearby.size();

            /*
             * 尝试补满。
             */
            for (int i = 0;
                 i < amountToSpawn;
                 i++) {

                BlockPos spawnPos =
                        findSpawnPosition(
                                level,
                                player
                        );

                if (spawnPos == null) {
                    break;
                }

                spawnNightWanderer(
                        level,
                        spawnPos
                );
            }
        }
    }


    /**
     * 寻找一个适合生成的位置。
     */
    private static BlockPos findSpawnPosition(
            ServerLevel level,
            ServerPlayer player
    ) {

        Vec3 playerPos =
                player.position();

        for (int attempt = 0;
             attempt < MAX_ATTEMPTS;
             attempt++) {

            /*
             * 随机角度。
             */
            double angle =
                    level.random.nextDouble()
                            * Math.PI * 2.0D;

            /*
             * 40 ~ 64 格。
             */
            double distance =
                    MIN_DISTANCE
                            + level.random.nextDouble()
                            * (MAX_DISTANCE - MIN_DISTANCE);

            int x =
                    (int) Math.floor(
                            playerPos.x
                                    + Math.cos(angle)
                                    * distance
                    );

            int z =
                    (int) Math.floor(
                            playerPos.z
                                    + Math.sin(angle)
                                    * distance
                    );

            /*
             * 找地表。
             */
            int y =
                    level.getHeight(
                            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            x,
                            z
                    );

            if (y <= level.getMinBuildHeight()) {
                continue;
            }

            BlockPos pos =
                    new BlockPos(x, y, z);

            /*
             * 检查位置。
             */
            if (!isValidSpawnPosition(
                    level,
                    pos
            )) {
                continue;
            }

            /*
             * 最终确认距离。
             */
            double distanceSqr =
                    pos.distSqr(
                            player.blockPosition()
                    );

            if (distanceSqr <
                    MIN_DISTANCE * MIN_DISTANCE) {
                continue;
            }

            if (distanceSqr >
                    MAX_DISTANCE * MAX_DISTANCE) {
                continue;
            }

            return pos;
        }

        return null;
    }


    /**
     * 判断位置能不能生成夜游鬼。
     */
    private static boolean isValidSpawnPosition(
            ServerLevel level,
            BlockPos pos
    ) {

        /*
         * 地面必须稳固。
         */
        BlockPos groundPos =
                pos.below();

        BlockState ground =
                level.getBlockState(
                        groundPos
                );

        if (!ground.isFaceSturdy(
                level,
                groundPos,
                Direction.UP
        )) {
            return false;
        }

        /*
         * 身体位置必须为空气。
         */
        if (!level.getBlockState(pos).isAir()) {
            return false;
        }

        /*
         * 头部也必须为空气。
         */
        if (!level.getBlockState(
                pos.above()
        ).isAir()) {
            return false;
        }

        /*
         * 夜游鬼怕光。
         *
         * 方块光 > 3 就不生成。
         */
        int blockLight =
                level.getBrightness(
                        LightLayer.BLOCK,
                        pos
                );

        if (blockLight > 3) {
            return false;
        }

        return true;
    }


    /**
     * 创建并生成夜游鬼。
     */
    private static void spawnNightWanderer(
            ServerLevel level,
            BlockPos pos
    ) {

        NightWanderer entity =
                QisPlan2.NIGHT_WANDERER
                        .get()
                        .create(level);

        if (entity == null) {
            return;
        }

        entity.moveTo(
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F,
                0.0F
        );

        level.addFreshEntity(entity);

        QisPlan2.LOGGER.info(
                "[QisPlan2] 夜游鬼生成：{} {} {}",
                pos.getX(),
                pos.getY(),
                pos.getZ()
        );
    }
}