package com.qidate.qisplan2.ghost.curse.type.ghostdivination;

import com.qidate.qisplan2.ghost.curse.Curse;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public class GhostDivinationCurse
        implements Curse {

    /**
     * 生签诅咒类型。
     */
    public static final ResourceLocation TYPE =
            GhostDivinationCurseType.ID;

    /**
     * 生签默认强度。
     */
    public static final int DEFAULT_STRENGTH = 4;

    /**
     * 生签持续时间。
     */
    public static final int DEFAULT_DURATION =
            5 * 60 * 20;

    private final UUID id;
    private final UUID target;
    private final GhostDivinationCurseSource source;

    /**
     * 当前诅咒强度。
     */
    private int strength;

    /**
     * 剩余持续时间。
     */
    private int remainingTicks;

    /**
     * 创建新的生签诅咒。
     */
    public GhostDivinationCurse(
            UUID target,
            GhostDivinationCurseSource source,
            int strength,
            int remainingTicks
    ) {
        this(
                UUID.randomUUID(),
                target,
                source,
                strength,
                remainingTicks
        );
    }

    /**
     * 从保存数据恢复生签诅咒。
     */
    public GhostDivinationCurse(
            UUID id,
            UUID target,
            GhostDivinationCurseSource source,
            int strength,
            int remainingTicks
    ) {
        this.id = id;
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
        return TYPE;
    }

    @Override
    public UUID getTarget() {
        return target;
    }

    @Override
    public GhostDivinationCurseSource getSource() {
        return source;
    }

    public int getStrength() {
        return strength;
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    /**
     * 增加剩余持续时间。
     *
     * @param ticks 增加的 Tick 数
     */
    public void addTime(
            int ticks
    ) {
        if (ticks <= 0) {
            return;
        }

        remainingTicks += ticks;
    }

    @Override
    public void tick(
            MinecraftServer server
    ) {
        remainingTicks--;
    }

    @Override
    public boolean isValid(
            MinecraftServer server
    ) {
        return remainingTicks > 0;
    }

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