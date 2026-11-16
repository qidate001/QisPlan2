package com.qidate.qisplan2.ghost.doorplate;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import java.util.Set;

/** 静态门牌的持久化位置索引。规则与能力由模块定义，这里不执行袭击或传送。 */
public final class GhostDoorPlateRegistry {
    /** 索引工具无需实例化。 */
    private GhostDoorPlateRegistry() {}
    /** 同时保存维度和不可变坐标，避免跨维度误配与可变坐标污染。 */
    public record DoorLocation(ResourceKey<Level> dimension, BlockPos pos) {
        public DoorLocation { pos = pos.immutable(); }
    }
    /** 新增或更新号码，同一个位置自动从旧号码集合中移除。 */
    public static void register(int number, ServerLevel level, BlockPos pos) {
        GhostDoorPlateSavedData.get(level).register(number, level.dimension(), pos);
    }
    /** 方块破坏、模块移除或发现失效索引时撤销位置。 */
    public static void unregisterPosition(ServerLevel level, BlockPos pos) {
        GhostDoorPlateSavedData.get(level).unregisterPosition(level.dimension(), pos);
    }
    /** 返回快照，遍历校验时允许移除失效记录。 */
    public static Set<DoorLocation> getLocations(ServerLevel level, int number) {
        return Set.copyOf(GhostDoorPlateSavedData.get(level).getLocations(number));
    }
}
