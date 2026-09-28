package com.qidate.qisplan2.ghost.curse.type.ghosttombstone;

import com.qidate.qisplan2.ghost.curse.CurseSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * 鬼墓碑诅咒来源。
 *
 * 表示：
 *
 * “这个诅咒来自哪一座鬼墓碑？”
 *
 * 当前来源信息：
 *
 * - 所在维度
 * - 鬼墓碑坐标
 */
public class GhostTombstoneCurseSource
        implements CurseSource {

    /**
     * 来源类型 ID。
     */
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    "qisplan2",
                    "ghost_tombstone"
            );

    /**
     * 所在维度。
     */
    private final ResourceKey<Level> dimension;

    /**
     * 鬼墓碑位置。
     */
    private final BlockPos pos;

    /**
     * 创建一个鬼墓碑来源。
     */
    public GhostTombstoneCurseSource(
            ResourceKey<Level> dimension,
            BlockPos pos
    ) {
        this.dimension = dimension;
        this.pos = pos;
    }

    /**
     * 获取来源类型。
     */
    @Override
    public ResourceLocation getType() {
        return ID;
    }

    /**
     * 获取所在维度。
     */
    public ResourceKey<Level> dimension() {
        return dimension;
    }

    /**
     * 获取鬼墓碑位置。
     */
    public BlockPos pos() {
        return pos;
    }

    /**
     * 保存来源数据。
     */
    @Override
    public CompoundTag save() {

        CompoundTag tag =
                new CompoundTag();

        /*
         * 保存维度。
         */
        tag.putString(
                "Dimension",
                dimension.location().toString()
        );

        /*
         * 保存方块坐标。
         */
        tag.putLong(
                "Pos",
                pos.asLong()
        );

        return tag;
    }

    /**
     * 从 NBT 恢复鬼墓碑来源。
     */
    public static GhostTombstoneCurseSource load(
            CompoundTag tag
    ) {

        /*
         * 恢复维度。
         */
        ResourceKey<Level> dimension =
                ResourceKey.create(
                        Registries.DIMENSION,
                        ResourceLocation.parse(
                                tag.getString("Dimension")
                        )
                );

        /*
         * 恢复方块坐标。
         */
        BlockPos pos =
                BlockPos.of(
                        tag.getLong("Pos")
                );

        return new GhostTombstoneCurseSource(
                dimension,
                pos
        );
    }
}