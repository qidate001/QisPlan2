package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.event.GhostEvent;

/**
 * 可选的状态机规则基类。语义事件是输入，不要求每个事件都启动新行为。
 * 阶段存入当前模块实例的 ruleStates，定义对象自身不保存可变运行状态。
 * 子类可同时声明 createTasks；任务与事件通过同一 GhostRuleServices 状态端口协作。
 */
public abstract class GhostStateMachineRule implements GhostRule {
    /** 首次运行或没有持久阶段时的初始状态。 */
    protected String initialState() { return "idle"; }
    /** 读取当前实例的阶段，不访问具体宿主。 */
    public final String state(GhostRuleContext context) {
        var data=context.services().state();
        return data.contains("phase") ? data.getString("phase") : initialState();
    }
    /** 默认接受已订阅的输入，子类可根据事实细化入口条件。 */
    public boolean canTrigger(GhostRuleContext context,GhostEvent event) { return true; }
    /** 由当前阶段决定同一语义的含义。 */
    public final void onTrigger(GhostRuleContext context,GhostEvent event) { onEvent(context,state(context),event); }
    /** 子类实现行为决策，可调用 transition 并通过统一执行器释放能力。 */
    protected abstract void onEvent(GhostRuleContext context,String phase,GhostEvent event);
    /**
     * 转移阶段并保留其他规则数据。先保存阶段再执行进入回调，嵌套语义能看到新状态。
     * 返回 false 表示当前载体未能保存状态，此时不会执行进入阶段的效果。
     */
    protected final boolean transition(GhostRuleContext context,String next,GhostEvent event) {
        if(next==null || next.isBlank())throw new IllegalArgumentException("Empty rule phase");
        String previous=state(context);
        if(previous.equals(next))return true;
        var services=context.services();var data=services.state();data.putString("phase",next);
        if(!services.saveState(data))return false;
        onTransition(context,previous,next,event);
        return true;
    }
    /** 状态成功改变后的扩展入口，可用于进入／离开阶段时的模块逻辑。 */
    protected void onTransition(GhostRuleContext context,String previous,String next,GhostEvent event) {}
}
