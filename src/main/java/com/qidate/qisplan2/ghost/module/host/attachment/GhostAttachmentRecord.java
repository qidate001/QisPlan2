package com.qidate.qisplan2.ghost.module.host.attachment;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** 独立来源的存档事实。来源 UUID 跟随附着关系，不使用目标实体 UUID 冒充来源。 */
public record GhostAttachmentRecord(UUID instance, CompoundTag item, boolean recoverable) {
    /** 保存物品组件的完整副本，调用者修改原标签不会改变已建立关系。 */
    public GhostAttachmentRecord {
        Objects.requireNonNull(instance);
        item = Objects.requireNonNull(item).copy();
    }
    /** 防止外部取得可变引用，绕过关系失效通知。 */
    @Override public CompoundTag item() { return item.copy(); }
    /** 序列化时保持来源身份、原物品及能否返还的事实。 */
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putUUID("instance", instance);
        tag.put("item", item.copy());
        tag.putBoolean("recoverable", recoverable);
        return tag;
    }
    /** 无效记录不制造来源，不把目标原有模块误当作附着来源。 */
    public static Optional<GhostAttachmentRecord> load(CompoundTag tag) {
        if (!tag.hasUUID("instance") || !tag.contains("item", Tag.TAG_COMPOUND)) return Optional.empty();
        return Optional.of(new GhostAttachmentRecord(tag.getUUID("instance"), tag.getCompound("item"), tag.getBoolean("recoverable")));
    }
}
