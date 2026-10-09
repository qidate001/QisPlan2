
package com.qidate.qisplan2.ghost.module;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * 实体所承载的厉鬼模块数据。
 */
public final class GhostEntityModuleData {

    private GhostEntityModuleData() {
    }

    public record Entry(
            ResourceLocation id,
            double intensity
    ) {
        public Entry {
            Objects.requireNonNull(id, "id");

            if (!Double.isFinite(intensity) || intensity < 0.0D) {
                throw new IllegalArgumentException(
                        "Invalid ghost module intensity: " + intensity
                );
            }
        }
    }
}