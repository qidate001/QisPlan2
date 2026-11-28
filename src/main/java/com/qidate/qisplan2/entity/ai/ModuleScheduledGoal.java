package com.qidate.qisplan2.entity.ai;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.module.GhostModuleCombatState;
import com.qidate.qisplan2.ghost.module.GhostRuleExecution;
import com.qidate.qisplan2.ghost.module.GhostRuleTask;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

/** 实体与通用规则调度器之间的薄适配层，不实现厉鬼行为或强度竞争算法。 */
public final class ModuleScheduledGoal extends Goal {
    private final GhostRuleExecution execution;
    /** 把实体状态转换为通用事实；模块任务不会取得来源实体。 */
    public ModuleScheduledGoal(ModularGhostEntity host, ResourceLocation module, int order,
            GhostRuleTask task, Supplier<List<ModuleScheduledGoal>> peers) {
        var flags=EnumSet.noneOf(Goal.Flag.class);
        for(var flag:task.getFlags())flags.add(Goal.Flag.valueOf(flag.name()));
        setFlags(flags);
        execution=new GhostRuleExecution(module,order,task,
                () -> host.isAlive() && !host.isSupernaturallyStunned()
                        && host.carriesModule(module) && host.suppression(module).canAct()
                        && host.suppression(module).canRunRules()
                        && host.getModules().stream().anyMatch(e -> e.id().equals(module) && GhostModuleCombatState.available(e)),
                () -> host.getModules().stream().filter(e -> e.id().equals(module)).mapToDouble(e -> e.intensity()).findFirst().orElse(0),
                () -> host.level().getGameTime(),
                () -> peers.get().stream().map(peer -> peer.execution).toList());
    }
    /** 保留现有调用者使用的强度比较入口，算法由规则框架提供。 */
    public static boolean dominates(double stronger,double weaker){return GhostRuleExecution.dominates(stronger,weaker);}
    /** 原版仅负责实体资源接管，启动决策由规则运行时产生。 */
    public boolean canUse(){return execution.canUse();}
    /** 运行中重新评估规则状态与竞争。 */
    public boolean canContinueToUse(){return execution.canContinueToUse();}
    /** 取得资源后启动规则。 */
    public void start(){execution.start();}
    /** 每次停止只释放一次任务资源。 */
    public void stop(){execution.stop();}
    /** 将推进请求交给规则运行时。 */
    public void tick(){execution.tick();}
    /** 读取规则的抢占声明。 */
    public boolean isInterruptable(){return execution.isInterruptable();}
    /** 读取规则的更新频率。 */
    public boolean requiresUpdateEveryTick(){return execution.requiresUpdateEveryTick();}
}
