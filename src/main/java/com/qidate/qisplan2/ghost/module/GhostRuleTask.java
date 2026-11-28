package com.qidate.qisplan2.ghost.module;

import java.util.EnumSet;

/**
 * 规则的一次持续运行实例，不继承原版 Goal，也不持有具体宿主。
 * 工厂必须为每个模块实例创建独立任务；临时目标可保存在字段，需跨卸载保存的数据写入规则状态。
 * 任务只通过模块提供的环境与行动接口操作世界，控制资源由适配层仲裁。
 */
public abstract class GhostRuleTask {
    public enum Flag { MOVE, LOOK, JUMP, TARGET }
    private EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);
    /** 声明当前任务所需的控制资源。 */
    protected final void setFlags(EnumSet<Flag> flags) { this.flags = flags.clone(); }
    /** 返回资源集合副本，避免调度器修改定义。 */
    public final EnumSet<Flag> getFlags() { return flags.clone(); }
    /** 判断是否需要启动；框架每刻最多探测一次，不能在此执行能力或移动。 */
    public abstract boolean canUse();
    /** 判断是否继续，默认复用启动条件。 */
    public boolean canContinueToUse() { return canUse(); }
    /** 取得控制权后开始本轮行为。 */
    public void start() {}
    /** 持续推进状态，按模块逻辑调用能力或环境接口。 */
    public void tick() {}
    /** 结束或被中断时释放资源；此回调不受死机、压制等执行限制阻挡。 */
    public void stop() {}
    /** 是否允许正常资源抢占；状态失效始终强制停止。 */
    public boolean isInterruptable() { return true; }
    /** 是否需要逐刻推进，默认沿用低频调度。 */
    public boolean requiresUpdateEveryTick() { return false; }
}
