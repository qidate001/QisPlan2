package com.qidate.qisplan2.ghost.module.sound;

import com.qidate.qisplan2.ghost.isolation.GhostIsolationSystem;
import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.event.GhostSoundEvent;
import com.qidate.qisplan2.network.ghostsound.GhostSoundNetwork;
import com.qidate.qisplan2.network.payload.PlayGhostSoundPayload;
import com.qidate.qisplan2.network.payload.StopGhostSoundPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;

/**
 * 通用灵异声音传播管理器。
 *
 * 核心职责：
 * 1. 注册、更新和注销方块及实体声音源。
 * 2. 判断玩家是否处于声音传播范围内。
 * 3. 判断声音传播路径是否被指定方块阻挡。
 * 4. 追踪每名玩家正在听到的具体声音源。
 * 5. 将声音传播事件派发给发声模块宿主。
 * 6. 通过播放适配器通知客户端开始或停止播放。
 *
 * 设计原则：
 * - 管理器不依赖具体的声音网络协议。
 * - 管理器不直接决定攻击、防护等灵异效果。
 * - 灵异效果由宿主的模块规则和能力处理。
 * - 同一 soundType 的多个声源按整体听觉状态聚合。
 *
 * 注意：
 * - 声源的 key 必须保持稳定。
 * - 多格方块必须使用同一个规范位置注册和注销。
 * - 宿主必须实现 GhostModuleHost。
 */
public final class GhostSoundManager {

    /** 声音传播状态的检查间隔，单位为 tick。 */
    private static final int CHECK_INTERVAL = 5;

    /**
     * 当前已注册的声音源。
     *
     * 每个声源使用稳定的 SourceKey 标识。
     * 同一个 key 重复注册时只更新配置，不会直接重置玩家听觉状态。
     */
    private static final Map<SourceKey, SoundSource> SOURCES =
            new HashMap<>();

    /**
     * 每名玩家当前听到的声音源快照。
     *
     * 使用 SoundSource 快照，而不是只保存 SourceKey。
     *
     * 原因：
     * 声源可能在玩家下一次检查之前就被注销。
     * 保存快照后，管理器仍然可以找到原宿主和播放适配器，
     * 正确停止声音，并判断该声音类别是否彻底中断。
     */
    private static final Map<UUID, Map<SourceKey, SoundSource>> HEARD_SOURCES =
            new HashMap<>();

    private GhostSoundManager() {
    }

    // =========================================================
    // 声源注册
    // =========================================================

    /**
     * 注册或更新方块声音源。
     *
     * blockPos 必须是声音源的规范位置。
     * 多格方块应始终使用同一个位置作为声音源标识。
     *
     * @param level 服务端维度
     * @param blockPos 声源规范位置
     * @param host 发声模块宿主
     * @param soundType 声音类别
     * @param range 最大传播距离
     * @param blocker 判断是否阻挡声音的方块规则
     * @param playback 客户端播放适配器
     */
    public static void registerBlockSource(
            ServerLevel level,
            BlockPos blockPos,
            GhostModuleHost host,
            ResourceLocation soundType,
            double range,
            Predicate<BlockState> blocker,
            GhostSoundPlayback playback
    ) {
        if (level == null || blockPos == null || host == null
                || soundType == null || blocker == null
                || !isValidRange(range)) {
            return;
        }

        BlockPos immutablePos = blockPos.immutable();

        SourceKey key = SourceKey.block(
                level.dimension().location(),
                immutablePos
        );

        SoundSource source = new SoundSource(
                key,
                level,
                host,
                null,
                soundType,
                new Vec3(
                        immutablePos.getX() + 0.5D,
                        immutablePos.getY() + 0.7D,
                        immutablePos.getZ() + 0.5D
                ),
                range,
                playback == null ? GhostSoundPlayback.NONE : playback
        );

        SOURCES.put(key, source);
    }

    /**
     * 兼容旧的六参数调用。
     *
     * 这种调用只注册声音传播事件，不执行客户端播放。
     * 需要实际播放声音时，应使用包含 playback 的七参数版本。
     */
    public static void registerBlockSource(
            ServerLevel level,
            BlockPos blockPos,
            GhostModuleHost host,
            ResourceLocation soundType,
            double range,
            Predicate<BlockState> blocker
    ) {
        registerBlockSource(
                level,
                blockPos,
                host,
                soundType,
                range,
                blocker,
                GhostSoundPlayback.NONE
        );
    }

    /**
     * 注册或更新实体声音源。
     *
     * entityId 必须是实体稳定的 UUID。
     * 实体移动时可以重复注册以更新位置，但不得更换 UUID。
     */
    public static void registerEntitySource(
            ServerLevel level,
            UUID entityId,
            GhostModuleHost host,
            ResourceLocation soundType,
            Vec3 position,
            double range,
            Predicate<BlockState> blocker,
            GhostSoundPlayback playback
    ) {
        if (level == null || entityId == null || host == null
                || soundType == null || position == null
                || blocker == null || !isValidRange(range)) {
            return;
        }

        SourceKey key = SourceKey.entity(
                level.dimension().location(),
                entityId
        );

        SoundSource source = new SoundSource(
                key,
                level,
                host,
                null,
                soundType,
                position,
                range,
                playback == null ? GhostSoundPlayback.NONE : playback
        );

        SOURCES.put(key, source);
    }

    /**
     * 兼容旧的七参数实体声音源注册调用。
     */
    public static void registerEntitySource(
            ServerLevel level,
            UUID entityId,
            GhostModuleHost host,
            ResourceLocation soundType,
            Vec3 position,
            double range,
            Predicate<BlockState> blocker
    ) {
        registerEntitySource(
                level,
                entityId,
                host,
                soundType,
                position,
                range,
                blocker,
                GhostSoundPlayback.NONE
        );
    }


    /**
     * 注册模块声明的方块声音源。
     *
     * 声音位置使用方块中心附近的固定坐标。
     * 播放协议后续统一接入通用声音网络层。
     */
    public static void registerModuleBlockSource(
            ServerLevel level,
            BlockPos blockPos,
            GhostModuleHost host,
            ResourceLocation moduleId,
            ResourceLocation soundType,
            ResourceLocation soundEvent,
            double range
    ) {
        if (level == null || blockPos == null || host == null
                || moduleId == null || soundType == null || !isValidRange(range)) {
            return;
        }

        BlockPos immutablePos = blockPos.immutable();

        SourceKey key = SourceKey.moduleBlock(
                level.dimension().location(),
                immutablePos,
                moduleId,
                soundType
        );

        UUID networkSourceId = createNetworkSourceId(key);

        GhostSoundPlayback playback = new GhostSoundPlayback() {
            @Override
            public void start(ServerPlayer player) {
                GhostSoundNetwork.sendPlay(
                        player,
                        new PlayGhostSoundPayload(
                                networkSourceId,
                                level.dimension().location(),
                                soundEvent,
                                immutablePos.getX() + 0.5D,
                                immutablePos.getY() + 0.7D,
                                immutablePos.getZ() + 0.5D
                        )
                );
            }

            @Override
            public void stop(ServerPlayer player) {
                GhostSoundNetwork.sendStop(
                        player,
                        new StopGhostSoundPayload(
                                networkSourceId,
                                level.dimension().location()
                        )
                );
            }
        };

        SoundSource source = new SoundSource(
                key,
                level,
                host,
                moduleId,
                soundType,
                new Vec3(
                        immutablePos.getX() + 0.5D,
                        immutablePos.getY() + 0.7D,
                        immutablePos.getZ() + 0.5D
                ),
                range,
                playback
        );

        SOURCES.put(key, source);
    }

    /**
     * 注销指定模块在指定方块位置声明的声音源。
     */
    public static void unregisterModuleBlockSource(
            ServerLevel level,
            BlockPos blockPos,
            ResourceLocation moduleId,
            ResourceLocation soundType
    ) {
        if (level == null || blockPos == null
                || moduleId == null || soundType == null) {
            return;
        }

        unregisterSource(
                SourceKey.moduleBlock(
                        level.dimension().location(),
                        blockPos,
                        moduleId,
                        soundType
                )
        );
    }


    private static boolean isValidRange(double range) {
        return Double.isFinite(range) && range >= 0.0D;
    }

    // =========================================================
    // 声源注销
    // =========================================================

    /**
     * 注销方块声音源。
     */
    public static void unregisterBlockSource(
            ServerLevel level,
            BlockPos blockPos
    ) {
        if (level == null || blockPos == null) {
            return;
        }

        unregisterSource(
                SourceKey.block(
                        level.dimension().location(),
                        blockPos
                )
        );
    }

    /**
     * 注销实体声音源。
     */
    public static void unregisterEntitySource(
            ServerLevel level,
            UUID entityId
    ) {
        if (level == null || entityId == null) {
            return;
        }

        unregisterSource(
                SourceKey.entity(
                        level.dimension().location(),
                        entityId
                )
        );
    }

    /**
     * 注销声音源。
     *
     * 对仍在听该声源的玩家：
     * 1. 派发 LEFT 事件。
     * 2. 停止客户端播放。
     * 3. 判断该玩家是否仍能听到同类别的其他声源。
     * 4. 如果同类别声源全部消失，只派发一次 INTERRUPTED。
     *
     * 注意：
     * 这里必须先保留被注销的 SoundSource 快照，
     * 否则从 SOURCES 删除后就无法找到原宿主。
     */
    private static void unregisterSource(SourceKey key) {
        SoundSource source = SOURCES.get(key);

        if (source == null) {
            return;
        }

        /*
         * 收集需要更新的玩家，避免遍历 HEARD_SOURCES 时修改其内部集合。
         */
        List<UUID> affectedPlayers = new ArrayList<>();

        for (Map.Entry<UUID, Map<SourceKey, SoundSource>> entry
                : HEARD_SOURCES.entrySet()) {
            if (entry.getValue().containsKey(key)) {
                affectedPlayers.add(entry.getKey());
            }
        }

        /*
         * 对每位仍在听该声源的玩家执行退出传播处理。
         */
        for (UUID playerId : affectedPlayers) {
            Map<SourceKey, SoundSource> heard =
                    HEARD_SOURCES.get(playerId);

            if (heard == null) {
                continue;
            }

            SoundSource previousSource = heard.remove(key);

            if (previousSource == null) {
                continue;
            }

            ServerPlayer player = source.level()
                    .getServer()
                    .getPlayerList()
                    .getPlayer(playerId);

            /*
             * 玩家可能已离线，或已经切换维度。
             * 离线玩家不再需要发送播放停止数据包。
             */
            if (player == null || player.level() != source.level()) {
                continue;
            }

            /*
             * 该声源不再向这名玩家传播。
             */
            dispatch(
                    previousSource,
                    GhostSoundEvent.Type.LEFT,
                    player
            );

            safeStop(previousSource, player);

            /*
             * 只有同类别的其他声源也全部不可听时，
             * 才触发一次 INTERRUPTED。
             */
            boolean stillHearsSameType = hasHeardSoundType(
                    heard,
                    previousSource.soundType()
            );

            if (!stillHearsSameType) {
                dispatch(
                        previousSource,
                        GhostSoundEvent.Type.INTERRUPTED,
                        player
                );
            }

            /*
             * 没有任何听觉状态时清理玩家条目。
             */
            if (heard.isEmpty()) {
                HEARD_SOURCES.remove(playerId);
            }
        }

        /*
         * 所有听众状态处理完成后，再正式移除声源。
         */
        SOURCES.remove(key);
    }

    // =========================================================
    // 服务端 tick
    // =========================================================

    /**
     * 定期刷新所有在线玩家的声音传播状态。
     *
     * 本方法应由现有的 NeoForge 事件注册机制调用。
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % CHECK_INTERVAL != 0) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                updatePlayer(player, level);
            }
        }
    }

    /**
     * 计算玩家当前能听到的所有声源，并与上次状态比较。
     */
    private static void updatePlayer(
            ServerPlayer player,
            ServerLevel level
    ) {
        UUID playerId = player.getUUID();

        Map<SourceKey, SoundSource> previous =
                HEARD_SOURCES.computeIfAbsent(
                        playerId,
                        ignored -> new HashMap<>()
                );

        Map<SourceKey, SoundSource> current = new HashMap<>();

        /*
         * 找出当前维度内，玩家能够听到的所有声源。
         */
        for (SoundSource source : SOURCES.values()) {
            if (source.level() != level) {
                continue;
            }

            if (canHear(source, player)) {
                current.put(source.key(), source);
            }
        }

        /*
         * 处理新进入听觉范围的声源。
         *
         * 只有此前没有听到该具体声源时，才发送开始播放通知。
         */
        for (Map.Entry<SourceKey, SoundSource> entry : current.entrySet()) {
            SourceKey key = entry.getKey();
            SoundSource source = entry.getValue();

            if (previous.containsKey(key)) {
                continue;
            }

            dispatch(
                    source,
                    GhostSoundEvent.Type.ENTERED,
                    player
            );

            safeStart(source, player);
        }

        /*
         * 处理离开听觉范围的声源。
         *
         * 此处使用 previous 中的旧快照，而不是从 SOURCES 查询。
         * 因此即使配置发生更新，也能正确处理上次听到的声源。
         */
        for (Map.Entry<SourceKey, SoundSource> entry : previous.entrySet()) {
            SourceKey key = entry.getKey();
            SoundSource oldSource = entry.getValue();

            if (current.containsKey(key)) {
                continue;
            }

            dispatch(
                    oldSource,
                    GhostSoundEvent.Type.LEFT,
                    player
            );

            safeStop(oldSource, player);
        }

        /*
         * 按 soundType 聚合听觉状态。
         *
         * 对每一种声音类别：
         * - 上一轮听得到；
         * - 这一轮完全听不到；
         *
         * 才产生 INTERRUPTED。
         *
         * 同一种声音即使有多个声源同时消失，也只派发一次中断事件。
         * 不同声音类别之间互不影响。
         */
        Set<ResourceLocation> previousTypes = collectSoundTypes(previous);
        Set<ResourceLocation> currentTypes = collectSoundTypes(current);

        for (ResourceLocation soundType : previousTypes) {
            if (currentTypes.contains(soundType)) {
                continue;
            }

            SoundSource interruptionSource =
                    findSourceByType(previous, soundType);

            if (interruptionSource == null) {
                continue;
            }

            dispatch(
                    interruptionSource,
                    GhostSoundEvent.Type.INTERRUPTED,
                    player
            );
        }

        /*
         * 用本轮快照替换旧状态。
         *
         * 即使声源 key 没变，也会更新保存的 SoundSource 配置，
         * 不会因为每 tick 注册同一个声源而重复触发 ENTERED。
         */
        previous.clear();
        previous.putAll(current);

        if (previous.isEmpty()) {
            HEARD_SOURCES.remove(playerId);
        }
    }

    // =========================================================
    // 声音传播判断
    // =========================================================

    /**
     * 判断玩家是否能听到指定声音源。
     *
     * 传播规则：
     * 1. 声音必须处于最大传播距离内。
     * 2. 声源与玩家必须属于同一个灵异隔绝 Region。
     *
     * Region 查询只使用 GhostIsolationSystem.getRegionId()，
     * 不主动触发新的空间检测。
     */

    private static boolean canHear(
            SoundSource source,
            ServerPlayer player
    ) {
        Vec3 start = source.position();
        Vec3 end = player.getEyePosition();

        // 检查声音传播距离。
        if (start.distanceToSqr(end)
                > source.range() * source.range()) {
            return false;
        }

        ServerLevel level = source.level();

        BlockPos sourcePos = BlockPos.containing(
                start.x,
                start.y,
                start.z
        );

        BlockPos listenerPos = BlockPos.containing(
                end.x,
                end.y,
                end.z
        );

        // 无法确认空间归属时，不传播声音。
        if (!level.hasChunkAt(sourcePos)
                || !level.hasChunkAt(listenerPos)) {
            return false;
        }

        UUID sourceRegionId =
                GhostIsolationSystem.getRegionId(level, sourcePos);

        UUID listenerRegionId =
                GhostIsolationSystem.getRegionId(level, listenerPos);

        // 只有处于同一个 Isolation，声音才可以传播。
        return Objects.equals(sourceRegionId, listenerRegionId);
    }

    // =========================================================
    // 声音类别聚合
    // =========================================================

    /**
     * 获取玩家当前听到的所有不同声音类别。
     */
    private static Set<ResourceLocation> collectSoundTypes(
            Map<SourceKey, SoundSource> sources
    ) {
        Set<ResourceLocation> result = new HashSet<>();

        for (SoundSource source : sources.values()) {
            result.add(source.soundType());
        }

        return result;
    }

    /**
     * 判断玩家是否仍在听某一类别的声音。
     */
    private static boolean hasHeardSoundType(
            Map<SourceKey, SoundSource> sources,
            ResourceLocation soundType
    ) {
        for (SoundSource source : sources.values()) {
            if (source.soundType().equals(soundType)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 从指定的听觉快照中，寻找某种声音类别的一个声源。
     *
     * 中断事件只需要一个宿主处理，避免同一类别重复攻击。
     */
    private static SoundSource findSourceByType(
            Map<SourceKey, SoundSource> sources,
            ResourceLocation soundType
    ) {
        for (SoundSource source : sources.values()) {
            if (source.soundType().equals(soundType)) {
                return source;
            }
        }

        return null;
    }

    // =========================================================
    // 事件派发与客户端播放
    // =========================================================

    /**
     * 向发声模块宿主派发声音事件。
     *
     * 声音管理器只报告传播状态，不直接执行灵异攻击。
     */
    private static void dispatch(
            SoundSource source,
            GhostSoundEvent.Type type,
            ServerPlayer listener
    ) {
        GhostModuleRuntime.dispatch(
                source.host(),
                new GhostSoundEvent(
                        type,
                        source.soundType(),
                        listener
                )
        );
    }

    /**
     * 安全调用客户端播放适配器。
     *
     * 播放适配器异常不应破坏整个服务端 tick。
     */
    private static void safeStart(
            SoundSource source,
            ServerPlayer player
    ) {
        source.playback().start(player);
    }

    /**
     * 安全调用客户端停止适配器。
     */
    private static void safeStop(
            SoundSource source,
            ServerPlayer player
    ) {
        source.playback().stop(player);
    }

    // =========================================================
    // 玩家生命周期
    // =========================================================

    /**
     * 玩家退出服务器时清理听觉状态。
     *
     * 由现有的玩家退出事件处理器调用。
     *
     * 玩家已经退出，不再派发 INTERRUPTED：
     * 离开服务器不是灵异声音传播中断造成的效果。
     */
    public static void removePlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }

        Map<SourceKey, SoundSource> heard =
                HEARD_SOURCES.remove(player.getUUID());

        if (heard == null) {
            return;
        }

        /*
         * 清理玩家对应的所有客户端播放状态。
         */
        for (SoundSource source : heard.values()) {
            safeStop(source, player);
        }
    }

    // =========================================================
    // 内部数据结构
    // =========================================================

    /**
     * 声音源的稳定标识。
     *
     * 方块源：维度 + 方块位置。
     * 实体源：维度 + 实体 UUID。
     */
    private record SourceKey(
            ResourceLocation dimension,
            BlockPos blockPos,
            UUID entityId,
            ResourceLocation moduleId,
            ResourceLocation soundType
    ) {
        private static SourceKey block(
                ResourceLocation dimension,
                BlockPos pos
        ) {
            return new SourceKey(
                    dimension,
                    pos.immutable(),
                    null,
                    null,
                    null
            );
        }

        private static SourceKey entity(
                ResourceLocation dimension,
                UUID entityId
        ) {
            return new SourceKey(
                    dimension,
                    null,
                    entityId,
                    null,
                    null
            );
        }

        private static SourceKey moduleBlock(
                ResourceLocation dimension,
                BlockPos pos,
                ResourceLocation moduleId,
                ResourceLocation soundType
        ) {
            return new SourceKey(
                    dimension,
                    pos.immutable(),
                    null,
                    moduleId,
                    soundType
            );
        }
    }

    /**
     * 已注册声音源的完整配置快照。
     */
    private record SoundSource(
            SourceKey key,
            ServerLevel level,
            GhostModuleHost host,
            ResourceLocation moduleId,
            ResourceLocation soundType,
            Vec3 position,
            double range,
            GhostSoundPlayback playback
    ) {
    }

    private static UUID createNetworkSourceId(SourceKey key) {
        String identity = key.dimension()
                + "|" + key.blockPos()
                + "|" + key.entityId()
                + "|" + key.moduleId()
                + "|" + key.soundType();

        return UUID.nameUUIDFromBytes(
                identity.getBytes(StandardCharsets.UTF_8)
        );
    }
}