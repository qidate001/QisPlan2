package com.qidate.qisplan2.ghost.curse;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 诅咒系统的持久化数据。
 *
 * CurseSavedData 只负责：
 *
 * 1. 将 CurseManager 中当前存在的诅咒保存到世界存档
 * 2. 从世界存档读取诅咒
 * 3. 根据 Type 找到对应的 CurseType
 * 4. 将完整 NBT 交给 CurseType 恢复
 *
 * 它不负责：
 *
 * - 创建诅咒
 * - 执行诅咒
 * - Tick
 * - 解析具体 Curse 的字段
 * - 解析 CurseSource
 *
 * 这些工作分别由 CurseType、Curse 和 CurseSource 自己负责。
 */
public final class CurseSavedData
        extends SavedData {

    /**
     * 世界 SavedData 的唯一名称。
     */
    private static final String NAME =
            "qisplan2_curses";

    /**
     * 所有诅咒的持久化数据。
     *
     * 每一个元素的结构为：
     *
     * {
     *     Type: "qisplan2:ghost_tombstone",
     *     Data: {
     *         ...
     *     }
     * }
     *
     * Data 的具体内容完全由对应 Curse 自己决定。
     */
    private final ListTag curses =
            new ListTag();

    private CurseSavedData() {
    }

    /**
     * 获取当前世界的诅咒 SavedData。
     *
     * 诅咒属于整个服务器，
     * 因此统一保存到主世界（Overworld）的 DataStorage。
     */
    public static CurseSavedData get(
            ServerLevel level
    ) {

        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        new Factory<>(
                                CurseSavedData::new,
                                CurseSavedData::load
                        ),
                        NAME
                );
    }

    /**
     * 从世界存档读取 CurseSavedData。
     *
     * 注意：
     *
     * 这里暂时只负责读取 NBT。
     *
     * 真正恢复 Curse 实例，
     * 由 restoreToManager() 完成。
     */
    private static CurseSavedData load(
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {

        CurseSavedData data =
                new CurseSavedData();

        /*
         * 读取所有诅咒数据。
         *
         * 如果旧存档没有 Curses，
         * 则使用空列表。
         */
        data.curses.addAll(
                tag.getList(
                        "Curses",
                        Tag.TAG_COMPOUND
                )
        );

        return data;
    }

    /**
     * 将当前 SavedData 写入世界存档。
     */
    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {

        /*
         * 保存所有诅咒。
         */
        tag.put(
                "Curses",
                curses
        );

        return tag;
    }

    /**
     * 根据当前 CurseManager 的状态，
     * 重新生成持久化数据。
     *
     * 注意：
     *
     * CurseSavedData 不解析 Curse 内部数据。
     *
     * 它只保存：
     *
     * Type
     * Data
     *
     * 其中 Data 完全由 Curse.save() 决定。
     */
    public void rebuildFromManager() {

        /*
         * 清空旧数据。
         *
         * 这样可以避免已经被删除的诅咒继续残留在存档里。
         */
        curses.clear();

        /*
         * 遍历当前所有正在运行的诅咒。
         */
        for (Curse curse :
                CurseManager.getAll()) {

            CompoundTag entry =
                    new CompoundTag();

            /*
             * 保存诅咒类型。
             *
             * 例如：
             *
             * qisplan2:ghost_tombstone
             */
            entry.putString(
                    "Type",
                    curse.getType().toString()
            );

            /*
             * 保存诅咒自己的完整数据。
             *
             * CurseSavedData 不关心 Data 里面有什么。
             */
            entry.put(
                    "Data",
                    curse.save()
            );

            /*
             * 添加到持久化列表。
             */
            curses.add(entry);
        }

        /*
         * 标记 SavedData 已经发生变化。
         */
        setDirty();
    }

    /**
     * 将 SavedData 中保存的所有诅咒恢复到 CurseManager。
     *
     * 恢复流程：
     *
     * SavedData
     *     ↓
     * Type
     *     ↓
     * CurseRegistry
     *     ↓
     * CurseType
     *     ↓
     * CurseType.load(Data)
     *     ↓
     * Curse
     *     ↓
     * CurseManager.add()
     */
    public void restoreToManager() {

        /*
         * 先清空当前运行中的诅咒。
         *
         * 防止重复恢复。
         */
        CurseManager.clear();

        /*
         * 遍历存档中的所有诅咒。
         */
        for (Tag tag :
                curses) {

            /*
             * 理论上 Curses 列表中全部都是 CompoundTag。
             *
             * 为了避免损坏存档导致服务器直接崩溃，
             * 这里进行类型检查。
             */
            if (!(tag instanceof CompoundTag entry)) {
                continue;
            }

            /*
             * 获取诅咒类型 ID。
             */
            String typeString =
                    entry.getString("Type");

            /*
             * 如果类型为空，
             * 跳过这一条损坏数据。
             */
            if (typeString.isEmpty()) {
                continue;
            }

            ResourceLocation typeId;

            try {

                /*
                 * 将字符串转换为 ResourceLocation。
                 */
                typeId =
                        ResourceLocation.parse(
                                typeString
                        );

            } catch (Exception exception) {

                /*
                 * 存档中的 Type 格式错误。
                 *
                 * 跳过这一条数据，
                 * 不让单个坏数据导致整个服务器崩溃。
                 */
                continue;
            }

            /*
             * 根据 Type 找到对应的 CurseType。
             */
            CurseType type =
                    CurseRegistry.get(typeId);

            /*
             * 如果当前版本的模组已经不存在这种诅咒类型，
             * 则无法恢复。
             *
             * 直接跳过。
             */
            if (type == null) {
                continue;
            }

            /*
             * 获取这个诅咒自己的完整数据。
             */
            CompoundTag data =
                    entry.getCompound("Data");

            try {

                /*
                 * 让具体 CurseType 自己负责恢复。
                 */
                Curse curse =
                        type.load(data);

                /*
                 * 恢复到运行时管理器。
                 */
                CurseManager.add(curse);

            } catch (Exception exception) {

                /*
                 * 单个诅咒数据损坏时，
                 * 不让整个服务器启动失败。
                 *
                 * 后续如果需要，
                 * 可以在这里增加 LOGGER。
                 */
            }
        }
    }
}