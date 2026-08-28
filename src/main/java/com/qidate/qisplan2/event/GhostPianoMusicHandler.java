package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModBlocks;
import com.qidate.qisplan2.data.GhostPianoSavedData;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.network.StartGhostPianoMusicPayload;
import com.qidate.qisplan2.network.StopGhostPianoMusicPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

/**
 * 鬼钢琴鬼音乐管理器。
 *
 * 规则：
 *
 * 1. 玩家进入鬼钢琴的听觉范围 → 开始播放
 * 2. 玩家离开听觉范围 / 被鬼石砖、关闭的鬼门阻挡 → 停止
 * 3. 如果玩家原本正在听音乐，但现在一首都听不到了
 *    → 受到 50 强度的灵异攻击
 *
 * 多架钢琴同时存在时：
 *
 * 只要还有至少一架钢琴能被听见，
 * 就不会触发“音乐中断”。
 */
@EventBusSubscriber(modid = QisPlan2.MODID)
public final class GhostPianoMusicHandler {

    /*
     * ==============================
     * 配置
     * ==============================
     */

    /**
     * 鬼钢琴最大听觉范围。
     */
    private static final double HEARING_RANGE = 32.0D;

    private static final double HEARING_RANGE_SQR =
            HEARING_RANGE * HEARING_RANGE;

    /**
     * 多久检查一次玩家的听觉状态。
     *
     * 5 tick = 0.25 秒。
     *
     * 不需要每 tick 检查，
     * 否则玩家数量多时没有必要。
     */
    private static final int CHECK_INTERVAL = 5;

    /**
     * 灵异袭击强度。
     */
    private static final double INTERRUPTION_ATTACK_STRENGTH =
            50.0D;


    /*
     * ==============================
     * 活跃鬼钢琴
     * ==============================
     *
     * 每个维度单独维护。
     */
    private static final Map<String, Set<BlockPos>>
            ACTIVE_PIANOS =
            new HashMap<>();


    /*
     * ==============================
     * 玩家当前正在听的钢琴
     * ==============================
     *
     * UUID → 当前能听见的钢琴位置
     */
    private static final Map<UUID, Set<BlockPos>>
            HEARD_PIANOS =
            new HashMap<>();


    private GhostPianoMusicHandler() {
    }


    /**
     * 注册一架鬼钢琴。
     *
     * 在 GhostPianoBlock 放置完成后调用。
     */
    public static void registerPiano(
            ServerLevel level,
            BlockPos pos
    ) {
        GhostPianoSavedData data =
                GhostPianoSavedData.get(level);

        if (data.add(pos)) {

            QisPlan2.LOGGER.debug(
                    "[QisPlan2] 注册鬼钢琴：{} @ {}",
                    level.dimension().location(),
                    pos
            );
        }
    }


    /**
     * 注销一架鬼钢琴。
     *
     * 在 GhostPianoBlock 被破坏时调用。
     */
    public static void unregisterPiano(
            ServerLevel level,
            BlockPos pos
    ) {
        GhostPianoSavedData data =
                GhostPianoSavedData.get(level);

        data.remove(pos);

        QisPlan2.LOGGER.debug(
                "[QisPlan2] 注销鬼钢琴：{} @ {}",
                level.dimension().location(),
                pos
        );
    }


    /**
     * 服务端 Tick。
     */
    @SubscribeEvent
    public static void onServerTick(
            ServerTickEvent.Post event
    ) {

        /*
         * 每 CHECK_INTERVAL tick 检查一次。
         */
        if (event.getServer()
                .getTickCount()
                % CHECK_INTERVAL != 0) {

            return;
        }

        for (ServerLevel level :
                event.getServer().getAllLevels()) {

            for (ServerPlayer player :
                    level.players()) {

                updatePlayer(
                        level,
                        player
                );
            }
        }
    }


    /**
     * 更新一个玩家当前能听见的所有鬼钢琴。
     */
    private static void updatePlayer(
            ServerLevel level,
            ServerPlayer player
    ) {

        UUID uuid =
                player.getUUID();

        /*
         * 上一次听见的钢琴。
         */
        Set<BlockPos> previous =
                HEARD_PIANOS.computeIfAbsent(
                        uuid,
                        ignored -> new HashSet<>()
                );

        /*
         * 本次实际能听见的钢琴。
         */
        Set<BlockPos> current =
                new HashSet<>();


        /*
         * 获取当前维度的钢琴。
         */
        GhostPianoSavedData data =
                GhostPianoSavedData.get(level);

        Set<BlockPos> pianos =
                data.getPositions();

        if (!pianos.isEmpty()) {

            /*
             * 遍历这个维度里记录的全部钢琴。
             *
             * 以后如果钢琴数量特别多，
             * 可以再优化成按 Chunk 索引。
             */
            for (BlockPos pianoPos :
                    pianos) {

                /*
                 * 钢琴已经不存在了。
                 *
                 * 清理失效记录。
                 */
                if (!level.getBlockState(
                        pianoPos
                ).is(ModBlocks.GHOST_PIANO_BLOCK.get())) {

                    GhostPianoSavedData.get(level)
                            .remove(pianoPos);

                    continue;
                }

                /*
                 * 距离检查。
                 */
                if (player.distanceToSqr(
                        pianoPos.getX() + 0.5D,
                        pianoPos.getY() + 0.5D,
                        pianoPos.getZ() + 0.5D
                ) > HEARING_RANGE_SQR) {

                    continue;
                }

                /*
                 * 判断声音是否被阻挡。
                 */
                if (!canHearPiano(
                        level,
                        pianoPos,
                        player
                )) {

                    continue;
                }

                current.add(
                        pianoPos
                );
            }
        }


        /*
         * ==============================
         * 开始听新的钢琴
         * ==============================
         */

        for (BlockPos pianoPos :
                current) {

            if (previous.contains(pianoPos)) {
                continue;
            }

            PacketDistributor.sendToPlayer(
                    player,
                    new StartGhostPianoMusicPayload(
                            pianoPos
                    )
            );
        }


        /*
         * ==============================
         * 停止已经听不到的钢琴
         * ==============================
         */

        for (BlockPos pianoPos :
                previous) {

            if (current.contains(pianoPos)) {
                continue;
            }

            PacketDistributor.sendToPlayer(
                    player,
                    new StopGhostPianoMusicPayload(
                            pianoPos
                    )
            );
        }


        /*
         * ==============================
         * 音乐是否真正中断？
         * ==============================
         *
         * previous 非空：
         *     玩家上一轮正在听音乐
         *
         * current 为空：
         *     现在一架也听不到了
         *
         * → 音乐中断
         */
        if (!previous.isEmpty()
                && current.isEmpty()) {

            onMusicInterrupted(
                    level,
                    player
            );
        }


        /*
         * 更新状态。
         */
        previous.clear();

        previous.addAll(
                current
        );
    }


    /**
     * 鬼音乐突然中断。
     */
    private static void onMusicInterrupted(
            ServerLevel level,
            ServerPlayer player
    ) {

        /*
         * 直接发动 50 强度灵异攻击。
         *
         * 鬼寿衣等防御手段会在
         * SupernaturalDeathHandler 中统一处理。
         */
        SupernaturalDeathHandler.tryKill(
                player,
                ModDamageTypes.ghostPiano(player),
                INTERRUPTION_ATTACK_STRENGTH
        );
    }


    /**
     * 判断玩家与钢琴之间有没有允许音乐通过的路径。
     *
     * 鬼石砖：
     *     阻挡
     *
     * 关闭鬼门：
     *     阻挡
     *
     * 打开的鬼门：
     *     不阻挡
     *
     * 其他方块：
     *     不作为鬼音乐的阻挡物
     */
    private static boolean canHearPiano(
            ServerLevel level,
            BlockPos pianoPos,
            ServerPlayer player
    ) {
        Vec3 start =
                new Vec3(
                        pianoPos.getX() + 0.5D,
                        pianoPos.getY() + 0.7D,
                        pianoPos.getZ() + 0.5D
                );

        Vec3 end =
                player.getEyePosition();

        double distance =
                start.distanceTo(end);

        /*
         * 每约 0.5 格检查一次。
         */
        int steps =
                Math.max(
                        1,
                        (int) Math.ceil(
                                distance / 0.5D
                        )
                );

        for (int i = 1; i < steps; i++) {

            double t =
                    (double) i / steps;

            double x =
                    Mth.lerp(
                            t,
                            start.x,
                            end.x
                    );

            double y =
                    Mth.lerp(
                            t,
                            start.y,
                            end.y
                    );

            double z =
                    Mth.lerp(
                            t,
                            start.z,
                            end.z
                    );

            BlockPos checkPos =
                    BlockPos.containing(
                            x,
                            y,
                            z
                    );

            BlockState state =
                    level.getBlockState(
                            checkPos
                    );

            if (isGhostMusicBlocker(state)) {
                return false;
            }
        }

        return true;
    }


    /**
     * 判断方块是否阻挡鬼音乐。
     */
    private static boolean isGhostMusicBlocker(
            BlockState state
    ) {
        /*
         * 鬼石砖
         */
        if (state.is(
                ModBlocks.GHOST_STONE_BRICKS.get()
        )) {
            return true;
        }

        /*
         * 鬼门
         *
         * 关闭：阻挡
         * 打开：不阻挡
         */
        if (state.is(
                ModBlocks.GHOST_DOOR.get()
        )) {
            return !state.getValue(
                    DoorBlock.OPEN
            );
        }

        return false;
    }


    /**
     * 清理一个玩家的听觉状态。
     *
     * 玩家退出服务器时可以调用。
     */
    public static void removePlayer(
            ServerPlayer player
    ) {

        UUID uuid =
                player.getUUID();

        Set<BlockPos> heard =
                HEARD_PIANOS.remove(
                        uuid
                );

        if (heard == null) {
            return;
        }

        for (BlockPos pianoPos :
                heard) {

            PacketDistributor.sendToPlayer(
                    player,
                    new StopGhostPianoMusicPayload(
                            pianoPos
                    )
            );
        }
    }
}