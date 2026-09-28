package com.qidate.qisplan2.ghost.curse;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 诅咒系统持久化数据。
 *
 * 负责：
 *
 * 1. 将 CurseManager 中的诅咒保存到世界存档
 * 2. 从世界存档读取诅咒
 * 3. 将读取到的诅咒恢复到 CurseManager
 *
 * 数据属于整个服务器，而不是某一个维度，
 * 因此统一挂载到主世界（Overworld）的 DataStorage。
 */
public final class CurseSavedData
        extends SavedData {

    private static final String NAME =
            "qisplan2_curses";

    private static final String CURSES =
            "Curses";

    /**
     * 当前服务器正在使用的 CurseSavedData。
     *
     * 服务器启动时初始化。
     */
    private static CurseSavedData instance;

    /**
     * 保存的诅咒列表。
     */
    private final ListTag curses =
            new ListTag();

    private CurseSavedData() {
    }

    /**
     * 获取当前服务器的诅咒存档数据。
     *
     * 如果存档中已经存在数据，
     * DataStorage 会自动读取。
     *
     * 如果不存在，
     * 则创建新的 CurseSavedData。
     */
    public static CurseSavedData get(
            ServerLevel level
    ) {

        CurseSavedData data =
                level.getServer()
                        .overworld()
                        .getDataStorage()
                        .computeIfAbsent(
                                new Factory<>(
                                        CurseSavedData::new,
                                        CurseSavedData::load
                                ),
                                NAME
                        );

        instance = data;

        return data;
    }

    /**
     * 从 NBT 加载诅咒数据。
     */
    private static CurseSavedData load(
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {

        CurseSavedData data =
                new CurseSavedData();

        data.curses.addAll(
                tag.getList(
                        CURSES,
                        Tag.TAG_COMPOUND
                )
        );

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

        tag.put(
                CURSES,
                curses.copy()
        );

        return tag;
    }

    /**
     * 将当前 CurseManager 的内容重新写入 SavedData。
     *
     * 注意：
     *
     * 这里只是在内存中更新 SavedData，
     * setDirty() 会告诉 Minecraft：
     *
     * “这个 SavedData 已经发生变化，
     * 下次保存世界时请把它写入磁盘。”
     */
    public void rebuildFromManager() {

        curses.clear();

        for (Curse curse :
                CurseManager.getAll()) {

            CompoundTag entry =
                    new CompoundTag();

            entry.putString(
                    "Type",
                    curse.getType().toString()
            );

            entry.put(
                    "Data",
                    curse.save()
            );

            curses.add(entry);
        }

        setDirty();
    }

    /**
     * 通知当前服务器的诅咒数据发生变化。
     */
    public static void markDirty() {

        if (instance == null) {
            return;
        }

        instance.rebuildFromManager();
    }

    /**
     * 从 SavedData 恢复诅咒到 CurseManager。
     */
    public void restoreToManager() {

        CurseManager.clear();

        for (Tag tag :
                curses) {

            if (!(tag instanceof CompoundTag entry)) {
                continue;
            }

            String typeString =
                    entry.getString("Type");

            if (typeString.isEmpty()) {
                continue;
            }

            ResourceLocation typeId;

            try {

                typeId =
                        ResourceLocation.parse(
                                typeString
                        );

            } catch (Exception exception) {

                continue;
            }

            CurseType type =
                    CurseRegistry.get(typeId);

            if (type == null) {
                continue;
            }

            CompoundTag data =
                    entry.getCompound("Data");

            try {

                Curse curse =
                        type.load(data);

                /*
                 * 恢复存档时不能调用 add()。
                 *
                 * 因为 add() 会触发 SavedData 更新，
                 * 这会让“恢复过程”反过来修改正在读取的数据。
                 */
                CurseManager.restore(
                        curse
                );

            } catch (Exception exception) {

                // 单个诅咒恢复失败时，
                // 不影响其他诅咒继续恢复。
            }
        }
    }

    /**
     * 清除当前服务器实例。
     *
     * 防止服务器关闭后，
     * 静态 instance 仍然指向旧服务器的数据。
     */
    public static void clearInstance() {
        instance = null;
    }
}