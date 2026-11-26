package com.qidate.qisplan2.ghost.module.domain;

import com.qidate.qisplan2.ghost.module.component.GhostComponentProgram;
import com.qidate.qisplan2.ghost.module.component.GhostSuppressionAura;

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
        int exposureInterval, boolean entityVision, boolean teleport, List<GhostDomainEffect> effects, boolean layered, GhostDomainSettings defaultSettings, GhostDomainGrowth growth, GhostComponentProgram<GhostDomainAbilityContext> program, GhostSuppressionAura suppression) {
    /** 兼容原有声明；组件能力和常态压制均需完整模块明确选择。 */
    public GhostDomainDefinition(double radius,int initialLayer,boolean automatic,DoubleFunction<GhostDomainShape> shape,
            boolean touch,boolean sight,int exposureInterval,boolean entityVision,boolean teleport,List<GhostDomainEffect> effects,
            boolean layered,GhostDomainSettings defaultSettings,GhostDomainGrowth growth) {
        this(radius,initialLayer,automatic,shape,touch,sight,exposureInterval,entityVision,teleport,effects,layered,defaultSettings,growth,
            GhostComponentProgram.empty(),GhostSuppressionAura.NONE);
    }
    /** 同一套组件规则、能力和媒介执行模型用于鬼域与诅咒。 */
    public GhostDomainDefinition withProgram(GhostComponentProgram<GhostDomainAbilityContext> value){
        return new GhostDomainDefinition(radius,initialLayer,automatic,shape,touch,sight,exposureInterval,entityVision,teleport,effects,layered,defaultSettings,growth,value,suppression);
    }
    /** 快速声明鬼域的常态压制；框架只维护覆盖关系，不识别具体厉鬼。 */
    public GhostDomainDefinition withSuppression(GhostSuppressionAura value){
        return new GhostDomainDefinition(radius,initialLayer,automatic,shape,touch,sight,exposureInterval,entityVision,teleport,effects,layered,defaultSettings,growth,program,value);
    }
    /** 未显式声明的鬼域不分层，采用允许进入、禁止离开的通用配置。 */
    public GhostDomainDefinition(double radius, int initialLayer, boolean automatic,
            DoubleFunction<GhostDomainShape> shape, boolean touch, boolean sight,
            int exposureInterval, boolean entityVision, boolean teleport, List<GhostDomainEffect> effects) {
        this(radius, initialLayer, automatic, shape, touch, sight, exposureInterval,
                entityVision, teleport, effects, false, GhostDomainSettings.DEFAULT, null);
    }
    /** 在完整模块声明中显式选择分层能力，保留其他默认值。 */
    public GhostDomainDefinition withLayering(boolean value) {
        return new GhostDomainDefinition(radius,initialLayer,automatic,shape,touch,sight,
                exposureInterval,entityVision,teleport,effects,value,defaultSettings,growth,program,suppression);
    }
    /** 默认配置只用于尚未保存配置的新实例，不覆盖玩家已经编辑的设置。 */
    public GhostDomainDefinition withDefaults(GhostDomainSettings settings) {
        return new GhostDomainDefinition(radius,initialLayer,automatic,shape,touch,sight,
                exposureInterval,entityVision,teleport,effects,layered,settings,growth,program,suppression);
    }
    /** 兼容已有完整声明；未选择成长策略的模块继续使用原有等级机制。 */
    public GhostDomainDefinition(double radius,int initialLayer,boolean automatic,
            DoubleFunction<GhostDomainShape> shape,boolean touch,boolean sight,
            int exposureInterval,boolean entityVision,boolean teleport,List<GhostDomainEffect> effects,
            boolean layered,GhostDomainSettings defaultSettings) {
        this(radius,initialLayer,automatic,shape,touch,sight,exposureInterval,entityVision,teleport,
                effects,layered,defaultSettings,null);
    }
    /** 模块自行选择成长，不隐式开启分层或影响其他模块。 */
    public GhostDomainDefinition withGrowth(GhostDomainGrowth value) {
        return new GhostDomainDefinition(radius,initialLayer,automatic,shape,touch,sight,
                exposureInterval,entityVision,teleport,effects,layered,defaultSettings,Objects.requireNonNull(value),program,suppression);
    }
    /** 成长型鬼域的默认等级是永久下限；特殊鬼域沿用原有调级机制。 */
    public int minGrade() { return growth==null?1:initialLayer; }
    /** 公式只解锁默认等级以上的成长，绝不撤销模块天生拥有的等级。 */
    public int maxGrade(double intensity) {
        return growth==null?10:Math.clamp(growth.gradeFor(intensity),initialLayer,10);
    }
    /** 拒绝非法声明，避免把不合法的层数或周期带入世界运行时。 */
    public GhostDomainDefinition {
        if (!Double.isFinite(radius) || radius < 0 || initialLayer < 1 || initialLayer > 10 || exposureInterval < 0)
            throw new IllegalArgumentException("Invalid domain definition");

        Objects.requireNonNull(program);
        Objects.requireNonNull(suppression);
        Objects.requireNonNull(shape);
        Objects.requireNonNull(defaultSettings);
        effects = List.copyOf(effects);
    }
}
