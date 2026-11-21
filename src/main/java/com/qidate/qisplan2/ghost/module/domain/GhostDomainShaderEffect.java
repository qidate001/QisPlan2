package com.qidate.qisplan2.ghost.module.domain;

import net.minecraft.resources.ResourceLocation;
import java.util.Objects;

/** 可复用的标准后处理声明。前缀是此效果的参数约定，不是鬼域框架的固定属性。 */
public record GhostDomainShaderEffect(ResourceLocation shaderId, String uniformPrefix) implements GhostDomainEffect {
    /** 校验资源与参数约定；不需要此后处理时直接不声明本效果。 */
    public GhostDomainShaderEffect {
        Objects.requireNonNull(shaderId);
        Objects.requireNonNull(uniformPrefix);
        if(uniformPrefix.isBlank()) throw new IllegalArgumentException("Shader uniform prefix must not be blank");
    }
}
