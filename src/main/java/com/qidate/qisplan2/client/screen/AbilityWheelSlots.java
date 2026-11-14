package com.qidate.qisplan2.client.screen;

import com.qidate.qisplan2.ghost.module.ability.GhostAbilityCatalog;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 固定十槽的纯配置模型，空槽与缺失技能不会导致后续槽位移动。 */
public final class AbilityWheelSlots {
    public static final int SIZE = 10;
    /** 两个 ID 共同确定技能，不依赖译名或当前列表下标。 */
    public record Key(ResourceLocation module, ResourceLocation ability) {
        /** 从实时目录取得稳定标识。 */
        public static Key of(GhostAbilityCatalog.Entry entry) { return new Key(entry.moduleId(), entry.ability().getId()); }
    }
    private final List<Key> slots = new ArrayList<>(Collections.nCopies(SIZE, null));
    /** 恢复存储，截断超额项并消除重复，保留明确的空槽。 */
    public AbilityWheelSlots(List<Key> saved) {
        for (int i = 0; i < Math.min(SIZE, saved.size()); i++) {
            Key key = saved.get(i);
            if (key != null && !slots.contains(key)) slots.set(i, key);
        }
    }
    /** 导出含空槽的不可变快照。 */
    public List<Key> snapshot() { return Collections.unmodifiableList(new ArrayList<>(slots)); }
    /** 放入新技能替换目标槽，已有技能则交换两个槽，避免重复。 */
    public void assign(int slot, Key key) {
        if (slot < 0 || slot >= SIZE || key == null) return;
        int previous = slots.indexOf(key);
        if (previous == slot) return;
        Key displaced = slots.get(slot);
        slots.set(slot, key);
        if (previous >= 0) slots.set(previous, displaced);
    }
    /** 清空指定槽位，不移动其余槽位。 */
    public void clear(int slot) { if (slot >= 0 && slot < SIZE) slots.set(slot, null); }
    /** 只解析当前真正拥有的能力；丢失的能力保留配置，但不可释放。 */
    public List<GhostAbilityCatalog.Entry> resolve(List<GhostAbilityCatalog.Entry> available) {
        var result = new ArrayList<GhostAbilityCatalog.Entry>();
        for (var key : slots) result.add(key == null ? null : available.stream()
                .filter(entry -> Key.of(entry).equals(key)).findFirst().orElse(null));
        return Collections.unmodifiableList(result);
    }
}
