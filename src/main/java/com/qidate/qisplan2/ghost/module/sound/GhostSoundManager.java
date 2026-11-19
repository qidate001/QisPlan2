package com.qidate.qisplan2.ghost.module.sound;

import com.qidate.qisplan2.core.ModTags;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.isolation.GhostIsolationSystem;
import com.qidate.qisplan2.ghost.GhostAvoidanceSystem;
import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.GhostModuleCombatState;
import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.event.GhostSoundEvent;
import com.qidate.qisplan2.network.ghostsound.GhostSoundNetwork;
import com.qidate.qisplan2.network.payload.PlayGhostSoundPayload;
import com.qidate.qisplan2.network.payload.StopGhostSoundPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import com.qidate.qisplan2.ghost.module.host.GhostSoundSuppression;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
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
 * 2. 判断所有生物是否处于声音传播范围内。
 * 3. 判断声音传播路径是否被指定方块阻挡。
 * 4. 追踪每个生物正在听到的具体声音源。
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
     * 同一个 key 重复注册时只更新配置，不会直接重置听众的听觉状态。
     */
    private static final Map<SourceKey, SoundSource> SOURCES =
            new HashMap<>();

    /**
     * 每个生物当前听到的声音源快照。
     *
     * 使用 SoundSource 快照，而不是只保存 SourceKey。
     *
     * 原因：
     * 声源可能在听众下一次检查之前就被注销。
     * 保存快照后，管理器仍然可以找到原宿主和播放适配器，
     * 正确停止声音，并判断该声音类别是否彻底中断。
     */
    private static final Map<UUID, Map<SourceKey, SoundSource>> HEARD_SOURCES =
            new HashMap<>();

    /** 仅跟踪当前或上一轮听众，不保留已卸载实体的强引用。 */
    private static final Map<UUID, WeakReference<LivingEntity>> LISTENERS = new HashMap<>();


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
     * 注册或更新模块声明的实体声音源。
     *
     * 声源使用实体 UUID、模块 ID 和声音类型作为稳定标识。
     * 实体移动时重复注册同一个标识，只更新声源位置和配置。
     */
    public static void registerModuleEntitySource(
            ServerLevel level,
            UUID entityId,
            GhostModuleHost host,
            ResourceLocation moduleId,
            ResourceLocation soundType,
            ResourceLocation soundEvent,
            Vec3 position,
            double range
    ) {
        if (level == null || entityId == null || host == null
                || moduleId == null || soundType == null
                || soundEvent == null || position == null
                || !isValidRange(range)) {
            return;
        }

        SourceKey key = SourceKey.moduleEntity(
                level.dimension().location(),
                entityId,
                moduleId,
                soundType
        );

        UUID networkSourceId = createNetworkSourceId(key);

        // 保存本次注册时的位置。
        // 实体移动后重新注册，会生成使用新位置的播放适配器。
        Vec3 soundPosition = position;

        GhostSoundPlayback playback = new GhostSoundPlayback() {
            @Override
            public void start(ServerPlayer player) {
                GhostSoundNetwork.sendPlay(
                        player,
                        new PlayGhostSoundPayload(
                                networkSourceId,
                                level.dimension().location(),
                                soundEvent,
                                soundPosition.x,
                                soundPosition.y,
                                soundPosition.z
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
                soundPosition,
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

    /**
     * 注销指定实体上，指定模块声明的声音源。
     */
    public static void unregisterModuleEntitySource(
            ServerLevel level,
            UUID entityId,
            ResourceLocation moduleId,
            ResourceLocation soundType
    ) {
        if (level == null || entityId == null
                || moduleId == null || soundType == null) {
            return;
        }

        unregisterSource(
                SourceKey.moduleEntity(
                        level.dimension().location(),
                        entityId,
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
     * 对仍在听该声源的生物：
     * 1. 派发 LEFT 事件。
     * 2. 停止客户端播放。
     * 3. 判断该听众是否仍能听到同类别的其他声源。
     * 4. 如果同类别声源全部消失，只派发一次 INTERRUPTED。
     *
     * 注意：
     * 这里必须先保留被注销的 SoundSource 快照，
     * 否则从 SOURCES 删除后就无法找到原宿主。
     */
    private static void unregisterSource(SourceKey key) {
        // 先注销，避免中断规则执行期间重新进入同一注销流程。
        SoundSource source = SOURCES.remove(key);
        if (source == null) return;
        for (var entry : List.copyOf(HEARD_SOURCES.entrySet())) {
            var heard = entry.getValue();
            var previous = heard.remove(key);
            if (previous == null) continue;
            GhostSoundSuppression.remove(entry.getKey(), createNetworkSourceId(key));
            var reference = LISTENERS.get(entry.getKey());
            var listener = reference == null ? null : reference.get();
            if (heard.isEmpty()) { HEARD_SOURCES.remove(entry.getKey()); LISTENERS.remove(entry.getKey()); }
            if (listener == null) continue;
            safeStop(previous, listener);
            if (!listener.isAlive() || listener.isRemoved() || listener.level() != source.level()) continue;
            dispatch(previous, GhostSoundEvent.Type.LEFT, listener);
            if (!hasHeardSoundType(heard, previous.soundType()))
                dispatch(previous, GhostSoundEvent.Type.INTERRUPTED, listener);
        }
    }

    /**
     * 每五 tick 检查活跃声源附近的空间索引及上一轮听众，不遍历世界全部生物。
     * 先完成整轮传播采样，再更新关系，避免本轮按声源遍历先后改变传播判定。
     * 相同来源、目标和强度的关系更新是幂等操作，稳定压制不会重新计算。
     */
    @SubscribeEvent public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % CHECK_INTERVAL != 0) return;
        for (var source : List.copyOf(SOURCES.values())) {
            if (source.moduleId() != null && (!source.host().hasModule(source.moduleId())
                    || !source.host().moduleWorld(source.moduleId()).present()
                    || !source.host().moduleWorld(source.moduleId()).sameWorld(source.level()))) unregisterSource(source.key());
        }
        Map<LivingEntity, Map<SourceKey, SoundSource>> next = new LinkedHashMap<>();
        for (var entry : List.copyOf(LISTENERS.entrySet())) {
            var listener = entry.getValue().get();
            if (listener == null) {
                HEARD_SOURCES.remove(entry.getKey()); LISTENERS.remove(entry.getKey());
                GhostSoundSuppression.removeTarget(entry.getKey());
            } else if (!listener.isAlive() || listener.isRemoved()) removeListener(listener);
            else next.put(listener, new HashMap<>());
        }
        for (var source : List.copyOf(SOURCES.values())) {
            if (!sourceEnabled(source)) continue;
            var point = source.position();
            var area = new AABB(point, point).inflate(source.range());
            for (var listener : source.level().getEntitiesOfClass(LivingEntity.class, area,
                    candidate -> candidate.isAlive() && !candidate.isRemoved())) {
                if (canHear(source, listener)) next.computeIfAbsent(listener, ignored -> new HashMap<>()).put(source.key(), source);
            }
        }
        // 一次性提交本轮所有关系变化，随后再派发可能引发袭击的语义事件。
        for (var entry : next.entrySet()) syncRelations(entry.getKey(), entry.getValue());
        for (var entry : next.entrySet()) updateListener(entry.getKey(), entry.getValue());
    }

    /** 按声源身份维护关系，离开一个声源不撤销其他声源；持续听到时也能更新强度。 */
    private static void syncRelations(LivingEntity listener, Map<SourceKey, SoundSource> current) {
        var previous = HEARD_SOURCES.getOrDefault(listener.getUUID(), Map.of());
        for (var key : previous.keySet()) if (!current.containsKey(key))
            GhostSoundSuppression.remove(listener.getUUID(), createNetworkSourceId(key));
        for (var source : current.values()) GhostSoundSuppression.update(createNetworkSourceId(source.key()),
                source.host(), source.moduleId(), source.soundType(), listener);
    }

    /** 所有生物共享进入、离开和同类别中断语义；只有玩家需要客户端播放包。 */
    private static void updateListener(LivingEntity listener, Map<SourceKey, SoundSource> current) {
        if (!listener.isAlive() || listener.isRemoved()) { removeListener(listener); return; }
        UUID id = listener.getUUID();
        // 前一个听众的语义事件可能注销声源；丢弃过期采样，避免重新建立已解除的听觉状态。
        current.entrySet().removeIf(entry -> {
            if (SOURCES.containsKey(entry.getKey())) return false;
            GhostSoundSuppression.remove(id, createNetworkSourceId(entry.getKey()));
            return true;
        });
        var previous = new HashMap<>(HEARD_SOURCES.getOrDefault(id, Map.of()));
        var transition = GhostSoundTransition.between(soundTypes(previous), soundTypes(current));
        if (current.isEmpty()) { HEARD_SOURCES.remove(id); LISTENERS.remove(id); }
        else { HEARD_SOURCES.put(id, new HashMap<>(current)); LISTENERS.put(id, new WeakReference<>(listener)); }
        for (var source : current.values()) {
            if (!transition.entered().contains(source.key())) continue;
            dispatch(source, GhostSoundEvent.Type.ENTERED, listener);
            if (listener.isAlive() && !listener.isRemoved() && SOURCES.containsKey(source.key())) safeStart(source, listener);
        }
        for (var source : previous.values()) {
            if (!transition.left().contains(source.key())) continue;
            safeStop(source, listener);
            if (listener.isAlive() && !listener.isRemoved()) dispatch(source, GhostSoundEvent.Type.LEFT, listener);
        }
        for (var type : transition.interrupted()) {
            if (!listener.isAlive() || listener.isRemoved()) continue;
            var source = findSourceByType(previous, type);
            if (source != null) dispatch(source, GhostSoundEvent.Type.INTERRUPTED, listener);
        }
    }

    /** 提取声音类别供纯差异计算，不向模块暴露具体来源身份。 */
    private static Map<SourceKey, ResourceLocation> soundTypes(Map<SourceKey, SoundSource> sources) {
        var result = new HashMap<SourceKey, ResourceLocation>();
        sources.forEach((key, source) -> result.put(key, source.soundType()));
        return result;
    }

    /** 模块的播放意图、媒介和能力权限统一决定声源是否实际生效。 */
    private static boolean sourceEnabled(SoundSource source) {
        if (source.moduleId() == null) return true;
        var host = source.host();
        var module = GhostModuleRegistry.get(source.moduleId());
        var entry = host.getModules().stream().filter(e -> e.id().equals(source.moduleId())).findFirst().orElse(null);
        return module != null && entry != null && host.carriesModule(entry.id())
                && host.moduleEnvironment(entry.id()).exposed()
                && host.suppression(entry.id()).canUseAbility(host.controlsIntrinsicModules())
                && module.soundEnabled(entry, host.allowsAutomaticAbilities());
    }

    // =========================================================
    // 声音传播判断
    // =========================================================

    /**
     * 判断生物是否能听到指定声音源。
     *
     * 传播规则：
     * 1. 声音必须处于最大传播距离内。
     * 2. 声源与听众必须属于同一个灵异隔绝 Region。
     * 3. 听众没有有效的白色鬼烛屏蔽，且不属于无听力标签。
     *
     * Region 查询只使用 GhostIsolationSystem.getRegionId()，
     * 不主动触发新的空间检测。
     */

    private static boolean canHear(
            SoundSource source,
            LivingEntity player
    ) {
        // 白烛只改变本轮传播结果，不能清空 HEARD_SOURCES 或提前跳过传播差异处理。
        // 上轮已听见、本轮被屏蔽时，正常执行停止播放、LEFT 和按声音类别聚合的 INTERRUPTED；
        // 一开始就被屏蔽则没有中断，持续屏蔽也不会重复触发。熄灭后重新进入听觉范围。
        if (player.getType().is(ModTags.NO_HEARING) || GhostAvoidanceSystem.hasWhiteGhostCandle(player)) {
            return false;
        }

        return canReachListener(source, player);
    }

    /**
     * 仅判断声音本来能否传播到听众，不考虑白烛。
     * 屏蔽消耗也必须满足距离、维度和空间隔绝条件，不能对本来听不到的声音收费。
     */
    private static boolean canReachListener(SoundSource source, LivingEntity player) {
        if (!player.isAlive() || player.isRemoved() || source.level() != player.level()) return false;
        if (!sourceEnabled(source)) return false;
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

    /**
     * 当前真正被白烛屏蔽的最高声源强度。
     * 模块声源只读取所属模块，不把同宿主的其他模块强度算进来；
     * 旧版未指定模块 ID 的声源以宿主最高有效模块强度兼容。
     * 此查询不修改听觉快照，不派发事件。
     */
    public static double getBlockedSoundIntensity(LivingEntity player) {
        if (player.getType().is(ModTags.NO_HEARING) || !GhostAvoidanceSystem.hasWhiteGhostCandle(player)) return 0;
        double strongest = 0;
        for (SoundSource source : SOURCES.values()) {
            if (!canReachListener(source, player)) continue;
            for (var entry : source.host().getModules()) {
                if (source.moduleId() != null && !source.moduleId().equals(entry.id())) continue;
                if (source.moduleId() == null && !GhostModuleCombatState.available(entry)) continue;
                if (Double.isFinite(entry.intensity())) strongest = Math.max(strongest, entry.intensity());
            }
        }
        return strongest;
    }

    // =========================================================
    // 声音类别聚合
    // =========================================================

    /**
     * 判断听众是否仍在听某一类别的声音。
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
            LivingEntity listener
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
            LivingEntity player
    ) {
        if (player instanceof ServerPlayer serverPlayer) source.playback().start(serverPlayer);
    }

    /**
     * 安全调用客户端停止适配器。
     */
    private static void safeStop(
            SoundSource source,
            LivingEntity player
    ) {
        if (player instanceof ServerPlayer serverPlayer) source.playback().stop(serverPlayer);
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
    public static void removePlayer(ServerPlayer player) { if (player != null) removeListener(player); }

    /** 离线或卸载不是听觉中断，不发动袭击；清除传播快照及目标关系。 */
    private static void removeListener(LivingEntity listener) {
        var heard = HEARD_SOURCES.remove(listener.getUUID());
        LISTENERS.remove(listener.getUUID());
        GhostSoundSuppression.removeTarget(listener.getUUID());
        if (heard != null) for (var source : heard.values()) safeStop(source, listener);
    }

    /** 生物卸载时及时释放引用和关系，重载后由附近声源自然重建。 */
    @SubscribeEvent public static void entityLeft(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) removeListener(living);
    }

    /** 同进程重新进入存档不能继承上一服务器的声音状态。 */
    @SubscribeEvent public static void serverStopped(ServerStoppedEvent event) {
        SOURCES.clear(); HEARD_SOURCES.clear(); LISTENERS.clear(); GhostSoundSuppression.clear();
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

        private static SourceKey moduleEntity(
                ResourceLocation dimension,
                UUID entityId,
                ResourceLocation moduleId,
                ResourceLocation soundType
        ) {
            return new SourceKey(
                    dimension,
                    null,
                    entityId,
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
