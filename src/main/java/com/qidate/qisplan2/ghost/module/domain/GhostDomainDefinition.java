package com.qidate.qisplan2.ghost.module.domain;

import com.qidate.qisplan2.ghost.domain.GhostDomainShape;
import java.util.Objects;
import java.util.function.DoubleFunction;

/**
 * 模块声明的鬼域身体。只描述几何、感知与表现，不注册第二套厉鬼类型。
 * 分层、覆盖竞争、隔绝、传送和飞行由鬼域框架实施；效果由模块能力实施。
 * visualPrefix 是着色器参数前缀，着色器资源 ID 与模块 ID 相同。
 */
public record GhostDomainDefinition(double radius, int initialLayer, boolean automatic,
        DoubleFunction<GhostDomainShape> shape, boolean touch, boolean sight,
        int exposureInterval, boolean entityVision, boolean teleport, String visualPrefix, boolean rain) {
    /** 拒绝非法声明，避免把不合法的层数或周期带入世界运行时。 */
    public GhostDomainDefinition {
        if (!Double.isFinite(radius) || radius < 0 || initialLayer < 1 || initialLayer > 10 || exposureInterval < 0)
            throw new IllegalArgumentException("Invalid domain definition");
        Objects.requireNonNull(shape);
        Objects.requireNonNull(visualPrefix);
    }
}
