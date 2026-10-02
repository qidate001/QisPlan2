package com.qidate.qisplan2.ghost.isolation;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

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

        regions.put(
                region.getId(),
                region
        );

        setDirty();
    }

    /**
     * 删除一个区域。
     */
    public void removeRegion(
            UUID id
    ) {

        if (regions.remove(id) != null) {
            setDirty();
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