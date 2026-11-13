package com.qidate.qisplan2.ghost.corrosion;

import com.qidate.qisplan2.ghost.bearing.SpiritualPart;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** 厉鬼声明的侵蚀贡献，支持任意宿主部位；侵蚀数值和承载容量是两个独立概念。 */
public record GhostCorrosion(Map<SpiritualPart, Integer> values) {
    /** 防御性复制，避免外部改变已经声明的侵蚀配置。 */
    public GhostCorrosion { values = values == null ? Map.of() : Map.copyOf(values); }

    /** 未声明的部位视为没有侵蚀。 */
    public int get(SpiritualPart part) { return values.getOrDefault(part, 0); }

    /** 返回不可变的部位贡献映射。 */
    public Map<SpiritualPart, Integer> values() { return values; }

    /** 创建可逐项累加的声明构造器。 */
    public static Builder builder() { return new Builder(); }

    /** 构造阶段允许重复添加同一部位，最终合并贡献。 */
    public static final class Builder {
        private final Map<SpiritualPart, Integer> values = new LinkedHashMap<>();

        /** 按部位标识累加，GLOBAL 仍可以作为全局侵蚀来源。 */
        public Builder add(SpiritualPart part, int amount) {
            values.merge(Objects.requireNonNull(part), amount, Integer::sum);
            return this;
        }

        /** 完成独立的不可变声明。 */
        public GhostCorrosion build() { return new GhostCorrosion(values); }
    }

    /** 提供矩阵计算使用的只读贡献集合。 */
    public Map<SpiritualPart, Integer> entries() { return values; }
}
