package com.qidate.qisplan2.ghost.module;

import java.util.Objects;
import net.minecraft.world.phys.AABB;

/** 注册期声明的携带观察：相对空间范围与物品条件，不绑定物理宿主。 */
public record CarriedItemObservation(AABB area, CarriedItemSelector items) {
    /** 拒绝缺失的空间或物品条件。 */
    public CarriedItemObservation { Objects.requireNonNull(area); Objects.requireNonNull(items); }
}
