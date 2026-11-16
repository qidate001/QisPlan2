package com.qidate.qisplan2.ghost.module.data;

import net.minecraft.nbt.CompoundTag;

/** 工作台的模块存储约定；库存编码仍使用原版 ContainerHelper。 */
public final class GhostWorkstationData {
    public static final String KEY = "workstation";
    /** 数据工具无需实例化。 */
    private GhostWorkstationData() {}
    /** 将旧容器标签复制到模块专属数据，不带入方块坐标或其他载体字段。 */
    public static CompoundTag fromLegacy(CompoundTag legacy) {
        var contents = new CompoundTag();
        for (String key : new String[]{"Items","CookTime","CookTimeTotal"})
            if (legacy.contains(key)) contents.put(key,legacy.get(key).copy());
        var custom = new CompoundTag(); custom.put(KEY,contents); return custom;
    }
}
