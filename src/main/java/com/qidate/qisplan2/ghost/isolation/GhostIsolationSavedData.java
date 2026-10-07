package com.qidate.qisplan2.ghost.isolation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * 灵异隔绝系统持久化数据。
 *
 * <p>
 * 负责：
 *
 * <ol>
 *     <li>将已经识别出的灵异隔绝区域保存到世界存档</li>
 *     <li>从世界存档读取灵异隔绝区域</li>
 * </ol>
 *
 * <p>
 * 数据属于整个服务器，而不是某一个维度，
 * 因此统一挂载到主世界（Overworld）的 DataStorage。
 */
public final class GhostIsolationSavedData
        extends SavedData {

    private static final String NAME =
            "qisplan2_ghost_isolation";

    private static final String REGIONS =
            "Regions";

    /**
     * 当前服务器正在使用的灵异隔绝存档数据。
     */
    private static GhostIsolationSavedData instance;

    /**
     * 已保存的灵异隔绝区域。
     *
     * UUID → Region
     */
    private final Map<UUID, GhostIsolationRegion> regions =
            new LinkedHashMap<>();

    /**
     * Chunk → Region 索引。
     *
     * <p>
     * 用于快速找到一个 Chunk 内可能存在的灵异隔绝 Region。
     * </p>
     *
     * <p>
     * Key 同时包含维度与 Chunk 坐标，
     * 防止不同维度的相同 Chunk 坐标发生冲突。
     * </p>
     */
    private final Map<RegionChunkKey, Set<UUID>> regionsByChunk =
            new HashMap<>();

    private record RegionChunkKey(
            ResourceKey<Level> dimension,
            long chunkPos
    ) {
    }

    private GhostIsolationSavedData() {
    }

    /**
     * 获取当前服务器的灵异隔绝存档数据。
     *
     * <p>
     * 如果存档中已经存在数据，
     * DataStorage 会自动读取。
     *
     * <p>
     * 如果不存在，
     * 则创建新的 GhostIsolationSavedData。
     */
    public static GhostIsolationSavedData get(
            ServerLevel level
    ) {

        GhostIsolationSavedData data =
                level.getServer()
                        .overworld()
                        .getDataStorage()
                        .computeIfAbsent(
                                new Factory<>(
                                        GhostIsolationSavedData::new,
                                        GhostIsolationSavedData::load
                                ),
                                NAME
                        );

        instance = data;

        return data;
    }

    /**
     * 从 NBT 加载灵异隔绝区域。
     */
    private static GhostIsolationSavedData load(
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {

        GhostIsolationSavedData data =
                new GhostIsolationSavedData();

        ListTag regionList =
                tag.getList(
                        REGIONS,
                        Tag.TAG_COMPOUND
                );

        for (Tag regionTag :
                regionList) {

            if (!(regionTag instanceof CompoundTag regionData)) {
                continue;
            }

            GhostIsolationRegion region =
                    GhostIsolationRegion.load(
                            regionData
                    );

            if (region == null) {
                continue;
            }

            data.regions.put(
                    region.getId(),
                    region
            );

            data.addRegionToIndex(
                    region
            );
        }

        return data;
    }

    /**
     * 保存到世界存档。
     */
    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {

        ListTag regionList =
                new ListTag();

        for (GhostIsolationRegion region :
                regions.values()) {

            regionList.add(
                    region.save()
            );
        }

        tag.put(
                REGIONS,
                regionList
        );

        return tag;
    }

    /**
     * 获取所有已经保存的区域。
     */
    public Collection<GhostIsolationRegion> getRegions() {

        return regions.values();
    }

    /**
     * 获取指定 UUID 的区域。
     */
    public GhostIsolationRegion getRegion(
            UUID id
    ) {

        return regions.get(id);
    }

    /**
     * 添加一个区域。
     */
    public void addRegion(
            GhostIsolationRegion region
    ) {
        GhostIsolationRegion old =
                regions.put(
                        region.getId(),
                        region
                );

        if (old != null) {
            removeRegionFromIndex(old);
        }

        addRegionToIndex(region);

        setDirty();
    }

    /**
     * 删除一个区域。
     */
    public void removeRegion(
            UUID id
    ) {
        GhostIsolationRegion region =
                regions.remove(id);

        if (region == null) {
            return;
        }

        removeRegionFromIndex(region);

        setDirty();
    }

    /**
     * 添加一个索引。
     */
    private void addRegionToIndex(
            GhostIsolationRegion region
    ) {
        ResourceKey<Level> dimension =
                region.getDimension();

        BlockPos min =
                region.getMin();

        BlockPos max =
                region.getMax();

        int minChunkX =
                min.getX() >> 4;

        int maxChunkX =
                max.getX() >> 4;

        int minChunkZ =
                min.getZ() >> 4;

        int maxChunkZ =
                max.getZ() >> 4;

        for (int chunkX = minChunkX;
             chunkX <= maxChunkX;
             chunkX++) {

            for (int chunkZ = minChunkZ;
                 chunkZ <= maxChunkZ;
                 chunkZ++) {

                long chunkPos =
                        ChunkPos.asLong(
                                chunkX,
                                chunkZ
                        );

                RegionChunkKey key =
                        new RegionChunkKey(
                                dimension,
                                chunkPos
                        );

                regionsByChunk
                        .computeIfAbsent(
                                key,
                                ignored -> new HashSet<>()
                        )
                        .add(region.getId());
            }
        }
    }

    /**
     * 获取区块索引。
     */
    public Set<UUID> getRegionIds(
            ResourceKey<Level> dimension,
            ChunkPos chunkPos
    ) {
        Set<UUID> ids =
                regionsByChunk.get(
                        new RegionChunkKey(
                                dimension,
                                chunkPos.toLong()
                        )
                );

        if (ids == null) {
            return Set.of();
        }

        return Collections.unmodifiableSet(ids);
    }

    /**
     * 删除一个索引。
     */
    private void removeRegionFromIndex(
            GhostIsolationRegion region
    ) {
        ResourceKey<Level> dimension =
                region.getDimension();

        BlockPos min =
                region.getMin();

        BlockPos max =
                region.getMax();

        int minChunkX =
                min.getX() >> 4;

        int maxChunkX =
                max.getX() >> 4;

        int minChunkZ =
                min.getZ() >> 4;

        int maxChunkZ =
                max.getZ() >> 4;

        for (int chunkX = minChunkX;
             chunkX <= maxChunkX;
             chunkX++) {

            for (int chunkZ = minChunkZ;
                 chunkZ <= maxChunkZ;
                 chunkZ++) {

                RegionChunkKey key =
                        new RegionChunkKey(
                                dimension,
                                ChunkPos.asLong(
                                        chunkX,
                                        chunkZ
                                )
                        );

                Set<UUID> ids =
                        regionsByChunk.get(key);

                if (ids == null) {
                    continue;
                }

                ids.remove(region.getId());

                if (ids.isEmpty()) {
                    regionsByChunk.remove(key);
                }
            }
        }
    }

    /**
     * 清空所有区域。
     *
     * <p>
     * 主要用于系统重建或调试。
     */
    public void clearRegions() {

        if (regions.isEmpty()) {
            return;
        }

        regions.clear();
        regionsByChunk.clear();

        setDirty();
    }

    /**
     * 通知当前服务器的灵异隔绝数据发生变化。
     */
    public static void markDirty() {

        if (instance == null) {
            return;
        }

        instance.setDirty();
    }

    /**
     * 清除当前服务器实例。
     *
     * <p>
     * 防止服务器关闭后，
     * 静态 instance 仍然指向旧服务器的数据。
     */
    public static void clearInstance() {

        instance = null;
    }
}