package com.qidate.qisplan2.ghost.module.domain;

import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** 服务端安全的面板声明。模块提供布局、能力引用和状态，客户端只负责绘制通用控件。 */
public record GhostDomainPanel(List<Control> controls, List<ResourceLocation> buttons,
                               List<Automation> automations, Map<String,ResourceLocation> common) {
    public static final GhostDomainPanel EMPTY = new GhostDomainPanel(List.of(), List.of(), List.of(), Map.of());
    /** 固定声明快照，避免运行时修改共享集合；自定义能力最多显示五项。 */
    public GhostDomainPanel { controls=List.copyOf(controls); buttons=List.copyOf(buttons); automations=List.copyOf(automations); common=Map.copyOf(common); }
    public enum Style { BUTTON, EYE }
    /** x/y 为控制区内的归一化坐标；integer 是向声明能力提交的参数，不是客户端代码。 */
    public record Control(String label, double x, double y, Style style, ResourceLocation ability,
                          Integer integer, Predicate<GhostModuleContext> active) {}
    /** 自动化仅引用模块已有能力；实现不允许客户端传入任意代码或任意能力 ID。 */
    public record Automation(ResourceLocation ability, String label) {}
}
