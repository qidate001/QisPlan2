# 规则的持续过程与行动适配

[返回开发指南目录](../ghost-module-development.md)

## 唯一声明入口

所有行为通过 `GhostModule.getRules()` 声明。旧 `behavior()` 与 `GhostBehavior` 已移除。
规则定义可以同时订阅语义事件并创建持续任务；仅自主启动的规则可使用便捷工厂：

```java
@Override
public List<GhostRule> getRules() {
    return List.of(GhostRule.autonomous(ACTIVITY_RULE, services -> {
        GhostActor actor = services.world().actor();
        if (actor == null) return List.of();
        return List.of(new MyRuleTask(actor));
    }));
}
```

`MyRuleTask` 继承本模组的 `GhostRuleTask`，不继承原版 Goal。
`GhostRuleServices` 只提供环境、世界操作、当前规则的状态读写，不提供宿主引用或类型。
检查 actor 是否存在，是检查行动能力端口，不是判断载体是什么；没有该端口时仍能接收其他语义和使用满足媒介的能力。

## 任务生命周期

每次工厂调用必须创建新任务；模块定义是共享的，不能把目标、计时、阶段放入定义字段。

- `setFlags(...)`：声明移动、朝向、跳跃或目标控制资源，不冲突的任务可以并行。
- `canUse()`：提出启动需求。可以准备候选与内部计时，不得抢先移动或执行能力；每刻最多探测一次。
- `start()`：取得控制后开始本轮过程。
- `canContinueToUse()`：检查目标、阶段和环境是否允许继续。
- `tick()`：推进行为、调用能力、转换阶段。
- `stop()`：结束或被中断时释放寻路、重力、碰撞等资源。不能依赖规则仍然可执行。

短暂目标和路径可以保存于任务实例；需要跨卸载、读档或转移延续的数据使用
`services.state()` / `services.saveState(data)`，存入当前模块 `customData.ruleStates[规则ID]`。
同一规则的语义回调通过 `context.services()` 访问同一状态；不同实例与不同规则互相隔离。

## 调度与宿主边界

`GhostRuleExecution` 是独立于实体的规则调度器，只接收可运行性、强度、时间与竞争者接口。
强度达到另一方两倍或差值至少 100 时，强者优先；只比较控制资源冲突的任务。
同等竞争按原有 100 tick 时间片轮换，停止后短暂让出控制。

实体适配器 `ModuleScheduledGoal` 把通用资源请求接入原版 GoalSelector，规则本身不引用 Goal。
该适配器检查行动许可、规则许可、承载、沉睡和死机。实体卸载与模块移除会停止任务。
其他支持主动行动的载体可接入同一调度器，不需要修改规则定义。
当前非实体载体没有提供自主寻路适配时，不会凭空获得移动能力。

## 已迁移的规则过程

- 敲门鬼：寻找 → 接近 → 调用敲门能力 → 冷却 → 再寻找；无门时可游荡。
- 喊人鬼：跟踪目标、接管物理控制、中断时恢复。
- 夜游鬼：夜间追踪与目标观察。
- 鬼雾：复用 `GhostWanderTask`。

这些过程位于各自模块目录，均由 `getRules()` 声明。鬼签的随机选签是瞬时规则，不需要伪造持续任务。
