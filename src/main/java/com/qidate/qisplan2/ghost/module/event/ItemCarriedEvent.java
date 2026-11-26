package com.qidate.qisplan2.ghost.module.event;

import java.util.Objects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 携带语义：某实体正在携带某物品；不透露物品位于背包、手中还是装备槽。 */
public record ItemCarriedEvent(LivingEntity target, ItemStack item) implements GhostEvent {
    /** 保留观察时的快照，能力执行时仍须通过媒介重新验证。 */
    public ItemCarriedEvent {
        Objects.requireNonNull(target); Objects.requireNonNull(item);
        if (item.isEmpty()) throw new IllegalArgumentException("Carried item cannot be empty");
        item = item.copy();
    }
    /** 不允许接收者修改其他订阅者所见的事实。 */
    @Override public ItemStack item() { return item.copy(); }
}
