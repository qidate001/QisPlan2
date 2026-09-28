package com.qidate.qisplan2.ghost.curse.type.ghosttombstone;

import com.qidate.qisplan2.ghost.curse.Curse;
import com.qidate.qisplan2.ghost.curse.CurseSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/**
 * 鬼墓碑诅咒。
 *
 * 当前阶段：
 *
 * - 记录一个具体的鬼墓碑诅咒实例
 * - 暂时没有实际诅咒效果
 *
 * 后续再逐步增加：
 *
 * - Tick 效果
 * - 玩家状态
 * - 诅咒阶段
 * - 灵异规则
 * 等。
 */
public class GhostTombstoneCurse
        implements Curse {

    /**
     * 鬼墓碑诅咒类型 ID。
     */
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    "qisplan2",
                    "ghost_tombstone"
            );

    /**
     * 当前诅咒实例自己的唯一 ID。
     */
    private final UUID id;

    /**
     * 被诅咒玩家。
     */
    private final UUID target;

    /**
     * 鬼墓碑来源。
     */
    private final GhostTombstoneCurseSource source;

    /**
     * 创建一个新的鬼墓碑诅咒。
     *
     * 新诅咒会自动生成新的实例 UUID。
     */
    public GhostTombstoneCurse(
            UUID target,
            GhostTombstoneCurseSource source
    ) {
        this(
                UUID.randomUUID(),
                target,
                source
        );
    }

    /**
     * 创建一个指定 ID 的鬼墓碑诅咒。
     *
     * 主要用于从存档恢复。
     */
    public GhostTombstoneCurse(
            UUID id,
            UUID target,
            GhostTombstoneCurseSource source
    ) {
        this.id = id;
        this.target = target;
        this.source = source;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public ResourceLocation getType() {
        return ID;
    }

    @Override
    public UUID getTarget() {
        return target;
    }

    @Override
    public CurseSource getSource() {
        return source;
    }

    /**
     * 每个服务器 Tick 执行一次。
     *
     * 当前阶段暂时没有实际效果。
     */
    @Override
    public void tick(
            MinecraftServer server
    ) {
        // 当前阶段暂时没有诅咒效果。
    }

    /**
     * 判断诅咒是否仍然有效。
     *
     * 当前阶段：
     *
     * 只要诅咒存在，
     * 就永久有效。
     */
    @Override
    public boolean isValid(
            MinecraftServer server
    ) {
        return true;
    }

    /**
     * 保存诅咒实例的完整数据。
     *
     * 这里暂时只有：
     *
     * Id
     * Target
     * Source
     *
     * 后续如果增加诅咒自身状态，
     * 直接继续放进这里。
     */
    @Override
    public CompoundTag save() {

        CompoundTag tag =
                new CompoundTag();

        /*
         * 保存诅咒实例 UUID。
         */
        tag.putUUID(
                "Id",
                id
        );

        /*
         * 保存被诅咒玩家 UUID。
         */
        tag.putUUID(
                "Target",
                target
        );

        /*
         * 保存来源类型。
         */
        tag.putString(
                "SourceType",
                source.getType().toString()
        );

        /*
         * 保存来源自己的数据。
         */
        tag.put(
                "Source",
                source.save()
        );

        return tag;
    }
}