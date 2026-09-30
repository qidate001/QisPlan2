package com.qidate.qisplan2.ghost.curse;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/**
 * Curse 的通用基础实现。
 *
 * <p>
 * 负责保存所有诅咒共有的基础属性：
 *
 * <ul>
 *     <li>ID</li>
 *     <li>类型</li>
 *     <li>目标</li>
 *     <li>来源</li>
 *     <li>强度</li>
 *     <li>剩余持续时间</li>
 * </ul>
 *
 * <p>
 * 具体诅咒只需要处理自己的特殊逻辑。
 */
public abstract class AbstractCurse
        implements Curse {

    /**
     * 无限持续。
     */
    public static final int INFINITE_DURATION = -1;

    private final UUID id;

    private final ResourceLocation type;

    private final UUID target;

    private final CurseSource source;

    private int strength;

    /**
     * 剩余持续 Tick。
     *
     * <p>
     * -1 表示无限持续。
     */
    private int remainingTicks;

    protected AbstractCurse(
            UUID id,
            ResourceLocation type,
            UUID target,
            CurseSource source,
            int strength,
            int remainingTicks
    ) {
        this.id = id;
        this.type = type;
        this.target = target;
        this.source = source;
        this.strength = strength;
        this.remainingTicks = remainingTicks;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public ResourceLocation getType() {
        return type;
    }

    @Override
    public UUID getTarget() {
        return target;
    }

    @Override
    public CurseSource getSource() {
        return source;
    }

    @Override
    public int getStrength() {
        return strength;
    }

    @Override
    public void setStrength(int strength) {
        this.strength = strength;
    }

    @Override
    public int getRemainingTicks() {
        return remainingTicks;
    }

    @Override
    public void setRemainingTicks(int ticks) {
        this.remainingTicks = ticks;
    }

    /**
     * 每 Tick 处理通用持续时间。
     *
     * <p>
     * 无限持续的诅咒不会减少时间。
     */
    protected void tickDuration() {

        if (remainingTicks > 0) {
            remainingTicks--;
        }
    }

    /**
     * 判断持续时间是否仍然有效。
     */
    protected boolean isDurationValid() {
        return remainingTicks != 0;
    }

    @Override
    public boolean isValid(
            MinecraftServer server
    ) {
        return isDurationValid();
    }

    /**
     * 保存通用诅咒数据。
     *
     * <p>
     * 具体 Curse 可以调用 super.save()
     * 后继续写入自己的特殊数据。
     */
    @Override
    public CompoundTag save() {

        CompoundTag tag =
                new CompoundTag();

        tag.putUUID(
                "Id",
                id
        );

        tag.putUUID(
                "Target",
                target
        );

        tag.put(
                "Source",
                source.save()
        );

        tag.putInt(
                "Strength",
                strength
        );

        tag.putInt(
                "RemainingTicks",
                remainingTicks
        );

        return tag;
    }
}