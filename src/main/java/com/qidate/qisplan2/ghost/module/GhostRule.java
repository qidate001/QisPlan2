package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import net.minecraft.resources.ResourceLocation;
import java.util.Set;

/**
 * 灵异规则定义。
 *
 * 规则属于 GhostModule。
 *
 * 负责判断语义事件是否满足自身的触发条件，
 * 并在条件满足时执行规则逻辑。
 *
 * 规则不直接等同于能力：
 * 规则负责决定何时触发，能力负责产生具体效果。
 */
public interface GhostRule {

    /**
     * 沉睡时观察规则是否被真正触犯，不执行 onTrigger 或任何能力。
     * 离散语义默认复用触发条件；Tick 维护不算触犯，范围与计时规则需覆写。
     * 可维护规则自己的接触/计时进度，但不得改变世界、提交影响或释放能力。
     */
    default boolean disturbedWhileSleeping(GhostRuleContext context, GhostEvent event) {
        return !(event instanceof ModuleTickEvent)
                && canTrigger(context, event);
    }


    /**
     * 获取规则在所属模块中的唯一标识。
     */
    ResourceLocation getId();

    /** 注册时声明订阅类型；支持父事件接口，声明必须稳定且不得读取实例状态。 */
    Set<Class<? extends GhostEvent>> eventTypes();

    /** 订阅已命中后筛选事件内容，例如开始接触或特定声音；不承担广播筛选职责。 */
    default boolean matches(GhostEvent event) { return true; }

    /**
     * 判断规则当前是否满足触发条件。
     *
     * 此方法应只进行判断，不应产生游戏效果。
     */
    boolean canTrigger(
            GhostRuleContext context,
            GhostEvent event
    );

    /**
     * 执行规则逻辑。
     *
     * 调用方应先完成事件筛选、状态检查和前置条件检查。
     */
    void onTrigger(
            GhostRuleContext context,
            GhostEvent event
    );
}