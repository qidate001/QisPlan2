package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModEntities;
import com.qidate.qisplan2.entity.NightWanderer;
import com.qidate.qisplan2.ghost.PossessionHandler;
import com.qidate.qisplan2.ghost.ability.nightwanderer.NightWandererAbility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

@EventBusSubscriber(modid = QisPlan2.MODID)
public class NightWandererSpawnHandler {

    /**
     * 最小生成距离。
     */
    private static final double MIN_DISTANCE = 40.0D;

    /**
     * 最大生成距离。
     */
    private static final double MAX_DISTANCE = 64.0D;

    /**
     * 每秒检查一次。
     */
    private static final int CHECK_INTERVAL = 20;

    /**
     * 寻找生成位置时最多尝试次数。
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
         * 主世界。
         */
        ServerLevel level =
                event.getServer().overworld();

        /*
         * 白天不生成。
         */
        if (level.isDay()) {
            return;
        }

        /*
         * ========================================
         * 全世界最多只能有 1 只夜游鬼
         * ========================================
         */
        List<NightWanderer> existing =
                level.getEntitiesOfClass(
                        NightWanderer.class,
                        new AABB(
                                level.getWorldBorder().getMinX(),
                                level.getMinBuildHeight(),
                                level.getWorldBorder().getMinZ(),
                                level.getWorldBorder().getMaxX(),
                                level.getMaxBuildHeight(),
                                level.getWorldBorder().getMaxZ()
                        ),
                        entity -> entity.isAlive()
                );

        if (!existing.isEmpty()) {
            return;
        }

        /*
         * ========================================
         * 找一个适合生成的玩家
         * ========================================
         */
        for (ServerPlayer player :
                event.getServer()
                        .getPlayerList()
                        .getPlayers()) {

            /*
             * 只处理主世界玩家。
             */
            if (player.serverLevel() != level) {
                continue;
            }

            /*
             * 驭鬼夜游鬼的玩家周围不再生成。
             */
            if (PossessionHandler.hasGhost(
                    player,
                    NightWandererAbility.ID
            )) {
                continue;
            }

            /*
             * 尝试寻找生成点。
             */
            BlockPos spawnPos =
                    findSpawnPosition(
                            level,
                            player
                    );

            if (spawnPos == null) {
                continue;
            }

            spawnNightWanderer(
                    level,
                    spawnPos
            );

            /*
             * 已经生成唯一的一只，
             * 本次服务器 Tick 结束。
             */
            return;
        }
    }


    private static BlockPos findSpawnPosition(
            ServerLevel level,
            ServerPlayer player
    ) {

        Vec3 playerPos =
                player.position();

        for (int attempt = 0;
             attempt < MAX_ATTEMPTS;
             attempt++) {

            double angle =
                    level.random.nextDouble()
                            * Math.PI * 2.0D;

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

            if (!isValidSpawnPosition(level, pos)) {
                continue;
            }

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


    private static boolean isValidSpawnPosition(
            ServerLevel level,
            BlockPos pos
    ) {

        BlockPos groundPos =
                pos.below();

        BlockState ground =
                level.getBlockState(groundPos);

        if (!ground.isFaceSturdy(
                level,
                groundPos,
                Direction.UP
        )) {
            return false;
        }

        if (!level.getBlockState(pos).isAir()) {
            return false;
        }

        if (!level.getBlockState(
                pos.above()
        ).isAir()) {
            return false;
        }

        /*
         * 光源附近不生成。
         */
        int blockLight =
                level.getBrightness(
                        LightLayer.BLOCK,
                        pos
                );

        return blockLight <= 3;
    }


    private static void spawnNightWanderer(
            ServerLevel level,
            BlockPos pos
    ) {

        NightWanderer entity =
                ModEntities.NIGHT_WANDERER
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