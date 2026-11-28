package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import net.minecraft.resources.ResourceLocation;
import java.util.Set;
import java.util.List;
import java.util.function.Function;

/**
 * 灵异规则定义。
 *
 * 规则属于 GhostModule。
 *
 * 描述完整行为逻辑：语义输入、自主启动、持续推进和阶段转移。
 * 定义本身不保存实例状态；持续任务由工厂创建，持久阶段通过规则服务保存。
 * 一个规则可以有多个语义入口，也可以同时声明持续任务。
 *
 * 规则不直接等同于能力：
 * 规则负责决定何时触发，能力负责产生具体效果。
 */
public interface GhostRule {
    /** 是否声明持续过程；注册索引允许这类规则没有语义事件入口。 */
    default boolean hasTasks() { return false; }
    /** 为一个模块实例创建持续任务，禁止返回跨实例共享的可变任务。 */
    default List<GhostRuleTask> createTasks(GhostRuleServices services) { return List.of(); }

    /** 声明仅由自主条件启动的规则；没有行动端口时，工厂可以返回空列表。 */
    static GhostRule autonomous(ResourceLocation id,
            Function<GhostRuleServices, List<GhostRuleTask>> factory) {
        return new GhostRule() {
            public ResourceLocation getId() { return id; }
            public Set<Class<? extends GhostEvent>> eventTypes() { return Set.of(); }
            public boolean hasTasks() { return true; }
            public List<GhostRuleTask> createTasks(GhostRuleServices services) { return factory.apply(services); }
            public boolean canTrigger(GhostRuleContext context, GhostEvent event) { return false; }
            public void onTrigger(GhostRuleContext context, GhostEvent event) {}
        };
    }


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

    /** 携带语义的按需观察订阅；框架只为已安装且可观察的规则查询指定范围。 */
    default List<CarriedItemObservation> carriedItems() { return List.of(); }

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