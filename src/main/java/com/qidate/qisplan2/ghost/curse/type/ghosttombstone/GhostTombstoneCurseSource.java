package com.qidate.qisplan2.ghost.curse.type.ghosttombstone;

import com.qidate.qisplan2.ghost.curse.CurseSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * 鬼墓碑诅咒的来源。
 *
 * 用来记录是哪一个维度、哪一块鬼墓碑产生了这个诅咒。
 */
public record GhostTombstoneCurseSource(
        ResourceKey<Level> dimension,
        BlockPos pos
) implements CurseSource {
}