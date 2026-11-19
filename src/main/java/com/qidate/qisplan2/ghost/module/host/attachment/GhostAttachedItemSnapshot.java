package com.qidate.qisplan2.ghost.module.host.attachment;

import java.util.ArrayList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * 附着物使用独立的完整组件快照。原版 ItemStack.save 仅写组件差量，
 * 此处显式写入插入时全部可持久化组件，包含物品默认提供的厉鬼模块。
 */
public final class GhostAttachedItemSnapshot {
    private static final String VERSION = "qisplan2:component_snapshot";
    private GhostAttachedItemSnapshot() {}

    /** 保存真实物品 ID、数量、全部持久组件与显式移除标记，不依赖默认物品还原模块。 */
    public static CompoundTag save(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) throw new IllegalArgumentException("Cannot attach an empty item");
        var item = (CompoundTag) stack.save(registries);
        var full = (CompoundTag) DataComponentMap.CODEC.encodeStart(
                registries.createSerializationContext(NbtOps.INSTANCE), stack.getComponents()).getOrThrow();
        // 默认组件被显式移除时，原版用 !组件ID 记录；保留这些标记以便标准读取器也正确解析。
        var patch = item.getCompound("components");
        for (var key : patch.getAllKeys()) if (key.startsWith("!")) full.put(key, patch.get(key).copy());
        item.put("components", full);
        item.putInt(VERSION, 1);
        return item;
    }

    /**
     * 恢复原物品而非创建棺材钉。新版快照禁止重新注入快照中不存在的持久默认组件；
     * 旧版差量存档仍按原版解释，兼容此前只有物品 ID 的记录。
     */
    public static ItemStack restore(CompoundTag item, HolderLookup.Provider registries) {
        var stack = ItemStack.parseOptional(registries, item);
        if (stack.isEmpty() || !item.contains(VERSION, Tag.TAG_INT)) return stack;
        var components = item.getCompound("components");
        for (var type : new ArrayList<>(stack.getComponents().keySet())) {
            if (type.isTransient()) continue;
            var key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
            if (key != null && !components.contains(key.toString())) stack.remove(type);
        }
        return stack;
    }

    /** 用于旧记录一次性补全快照；不更换附着来源身份。 */
    public static boolean isComplete(CompoundTag item) { return item.getInt(VERSION) == 1; }
}
