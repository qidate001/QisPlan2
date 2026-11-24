package com.qidate.qisplan2.ghost.module.domain;

import com.qidate.qisplan2.ghost.domain.GhostDomainShape;
import java.util.Objects;
import java.util.List;
import java.util.function.DoubleFunction;

/**
 * 模块声明的鬼域身体。只描述几何、感知与表现，不注册第二套厉鬼类型。
 * 分层、覆盖竞争、隔绝、传送和飞行由鬼域框架实施；效果由模块能力实施。
 * effects 是模块自行组合的表现声明；框架不列举鬼雨等具体效果。
 */
public record GhostDomainDefinition(double radius, int initialLayer, boolean automatic,
        DoubleFunction<GhostDomainShape> shape, boolean touch, boolean sight,
        int exposureInterval, boolean entityVision, boolean teleport, List<GhostDomainEffect> effects, boolean layered, GhostDomainSettings defaultSettings) {
    /** 未显式声明的鬼域不分层，采用允许进入、禁止离开的通用配置。 */
    public GhostDomainDefinition(double radius, int initialLayer, boolean automatic,
            DoubleFunction<GhostDomainShape> shape, boolean touch, boolean sight,
            int exposureInterval, boolean entityVision, boolean teleport, List<GhostDomainEffect> effects) {
        this(radius, initialLayer, automatic, shape, touch, sight, exposureInterval,
                entityVision, teleport, effects, false, GhostDomainSettings.DEFAULT);
    }
    /** 在完整模块声明中显式选择分层能力，保留其他默认值。 */
    public GhostDomainDefinition withLayering(boolean value) {
        return new GhostDomainDefinition(radius,initialLayer,automatic,shape,touch,sight,
                exposureInterval,entityVision,teleport,effects,value,defaultSettings);
    }
    /** 默认配置只用于尚未保存配置的新实例，不覆盖玩家已经编辑的设置。 */
    public GhostDomainDefinition withDefaults(GhostDomainSettings settings) {
        return new GhostDomainDefinition(radius,initialLayer,automatic,shape,touch,sight,
                exposureInterval,entityVision,teleport,effects,layered,settings);
    }
    /** 拒绝非法声明，避免把不合法的层数或周期带入世界运行时。 */
    public GhostDomainDefinition {
        if (!Double.isFinite(radius) || radius < 0 || initialLayer < 1 || initialLayer > 10 || exposureInterval < 0)
            throw new IllegalArgumentException("Invalid domain definition");

        Objects.requireNonNull(shape);
        Objects.requireNonNull(defaultSettings);
        effects = List.copyOf(effects);
    }
}
