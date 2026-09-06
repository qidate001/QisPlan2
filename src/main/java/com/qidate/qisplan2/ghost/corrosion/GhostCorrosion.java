package com.qidate.qisplan2.ghost.corrosion;

import java.util.EnumMap;
import java.util.Map;

public record GhostCorrosion(
        EnumMap<CorrosionType, Integer> values
) {

    public GhostCorrosion {

        if (values == null) {
            values = new EnumMap<>(CorrosionType.class);
        } else {
            values = new EnumMap<>(values);
        }
    }

    public int get(CorrosionType type) {
        return values.getOrDefault(type, 0);
    }

    public EnumMap<CorrosionType, Integer> values() {
        return new EnumMap<>(values);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final EnumMap<CorrosionType, Integer> values =
                new EnumMap<>(CorrosionType.class);

        public Builder add(
                CorrosionType type,
                int amount
        ) {

            values.merge(type, amount, Integer::sum);
            return this;
        }

        public GhostCorrosion build() {
            return new GhostCorrosion(values);
        }
    }
}