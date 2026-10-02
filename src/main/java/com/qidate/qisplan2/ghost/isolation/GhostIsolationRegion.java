package com.qidate.qisplan2.ghost.isolation;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * 一个已经被灵异隔绝系统识别过的空间区域。
 *
 * <p>
 * Region 本身并不负责检测空间是否隔绝。
 * 它只是保存一次检测结果以及后续缓存所需要的信息。
 * </p>
 */
public class GhostIsolationRegion {

    private final UUID id;

    private final ResourceKey<Level> dimension;

    private final BlockPos min;

    private final BlockPos max;

    private final BlockPos seed;

    private IsolationState state;

    public GhostIsolationRegion(
            UUID id,
            ResourceKey<Level> dimension,
            BlockPos min,
            BlockPos max,
            BlockPos seed,
            IsolationState state
    ) {
        this.id = id;
        this.dimension = dimension;
        this.min = min;
        this.max = max;
        this.seed = seed;
        this.state = state;
    }

    public UUID getId() {
        return id;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public BlockPos getMin() {
        return min;
    }

    public BlockPos getMax() {
        return max;
    }

    public BlockPos getSeed() {
        return seed;
    }

    public IsolationState getState() {
        return state;
    }

    public void setState(
            IsolationState state
    ) {
        this.state = state;
    }

    /**
     * 判断一个坐标是否位于这个 Region 的包围盒内。
     */
    public boolean contains(
            ResourceKey<Level> dimension,
            BlockPos pos
    ) {
        if (!this.dimension.equals(dimension)) {
            return false;
        }

        return pos.getX() >= min.getX()
                && pos.getX() <= max.getX()
                && pos.getY() >= min.getY()
                && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ()
                && pos.getZ() <= max.getZ();
    }

    /**
     * 将 Region 保存为 NBT。
     */
    public CompoundTag save() {

        CompoundTag tag =
                new CompoundTag();

        tag.putUUID(
                "Id",
                id
        );

        tag.putString(
                "Dimension",
                dimension.location().toString()
        );

        tag.put(
                "Min",
                BlockPos.CODEC
                        .encodeStart(
                                net.minecraft.nbt.NbtOps.INSTANCE,
                                min
                        )
                        .getOrThrow()
        );

        tag.put(
                "Max",
                BlockPos.CODEC
                        .encodeStart(
                                net.minecraft.nbt.NbtOps.INSTANCE,
                                max
                        )
                        .getOrThrow()
        );

        tag.put(
                "Seed",
                BlockPos.CODEC
                        .encodeStart(
                                net.minecraft.nbt.NbtOps.INSTANCE,
                                seed
                        )
                        .getOrThrow()
        );

        tag.putString(
                "State",
                state.name()
        );

        return tag;
    }

    /**
     * 从 NBT 读取 Region。
     *
     * @return 读取成功的 Region，
     *         如果数据损坏则返回 null
     */
    public static GhostIsolationRegion load(
            CompoundTag tag
    ) {

        try {

            UUID id =
                    tag.getUUID("Id");

            String dimensionString =
                    tag.getString("Dimension");

            if (dimensionString.isEmpty()) {
                return null;
            }

            ResourceLocation dimensionId =
                    ResourceLocation.parse(
                            dimensionString
                    );

            ResourceKey<Level> dimension =
                    ResourceKey.create(
                            net.minecraft.core.registries.Registries.DIMENSION,
                            dimensionId
                    );

            BlockPos min =
                    BlockPos.CODEC
                            .parse(
                                    net.minecraft.nbt.NbtOps.INSTANCE,
                                    tag.get("Min")
                            )
                            .getOrThrow();

            BlockPos max =
                    BlockPos.CODEC
                            .parse(
                                    net.minecraft.nbt.NbtOps.INSTANCE,
                                    tag.get("Max")
                            )
                            .getOrThrow();

            BlockPos seed =
                    BlockPos.CODEC
                            .parse(
                                    net.minecraft.nbt.NbtOps.INSTANCE,
                                    tag.get("Seed")
                            )
                            .getOrThrow();

            String stateString =
                    tag.getString("State");

            IsolationState state =
                    IsolationState.valueOf(
                            stateString
                    );

            return new GhostIsolationRegion(
                    id,
                    dimension,
                    min,
                    max,
                    seed,
                    state
            );

        } catch (Exception exception) {

            return null;
        }
    }
}