package com.qidate.qisplan2.ghost.bearing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 宿主自行声明的真实组成及各部位替换式模块容量。
 * 当前预设容量均为一；使用数量而非布尔占位，为将来多个厉鬼共用部位留出扩展。
 * 定义本身不可变，模块占位由当前模块列表推导，不另存一份容易失步的状态。
 */
public record HostComposition(Map<SpiritualPart, Integer> capacities) {
    public static final HostComposition HUMANOID = humanoid();
    public static final HostComposition UNIVERSAL = new HostComposition(Map.of(SpiritualPart.UNIVERSAL, 1));

    /** 校验真实部位与正整数容量，拒绝把 GLOBAL 当作可占用部位。 */
    public HostComposition {
        Objects.requireNonNull(capacities, "capacities");
        var copy = new LinkedHashMap<SpiritualPart, Integer>();
        capacities.forEach((part, capacity) -> {
            if (part == null || part.equals(SpiritualPart.GLOBAL) || capacity == null || capacity < 1)
                throw new IllegalArgumentException("Invalid bearing part or capacity");
            copy.put(part, capacity);
        });
        capacities = Collections.unmodifiableMap(copy);
    }

    /** 构造默认二十一个部位，所有部位暂时只能容纳一个替换式模块。 */
    private static HostComposition humanoid() {
        var parts = new LinkedHashMap<SpiritualPart, Integer>();
        SpiritualPart.humanoidParts().forEach(part -> parts.put(part, 1));
        return new HostComposition(parts);
    }

    /** 精确匹配优先；缺少对应组成时才使用万能部位，精确部位已满不会绕路占位。 */
    public SpiritualPart resolve(SpiritualPart requested) {
        if (capacities.containsKey(requested)) return requested;
        return capacities.containsKey(SpiritualPart.UNIVERSAL) ? SpiritualPart.UNIVERSAL : null;
    }
}
