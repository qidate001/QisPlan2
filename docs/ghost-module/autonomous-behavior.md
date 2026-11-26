# 自主行为（Goal）

[返回开发指南目录](../ghost-module-development.md)

### 自主行为

覆盖 `GhostModule.behavior()`，返回 `GhostBehavior` 工厂。工厂每次为当前模块实例创建新的 Goal；实例状态依旧写入模块 customData。示例：

```java
@Override
public GhostBehavior behavior() {
    return context -> {
        GhostActor actor = context.world().actor();
        if (actor == null) return List.of();
        return List.of(new MyGhostBehavior(actor));
    };
}
```

`MyGhostBehavior` 只持有 `GhostActor` 行动端口，不能接收 `ModularGhostEntity` 或玩家。端口提供寻路、视线、移动、碰撞查询、随机落点、当前模块数据和能力请求，不提供取回宿主的方法。`execute` 仍经过统一能力执行器；`nearby` 发布附近实体事实。没有自主寻路能力时，工厂返回空列表，主动技能和其他世界出口仍可使用。

`GhostModuleGoals` 仅调用模块的工厂，实体保留现有强度竞争和 Goal 控制标记调度。公共宿主不需要知道新厉鬼的名称。

## Goal 生命周期与实例状态

行为类继承原版 `Goal`，构造函数接收 `GhostActor`，通过 `setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK))` 声明实际占用的控制资源。只观察目标而不控制移动时，不应占用 MOVE。资源标记不重叠的行为可以并行。

- `canUse()`：读取端口和目标条件，判断是否有执行需求；不要在探测中释放能力或产生不可逆效果。
- `start()`：获得控制后初始化本次行动。
- `canContinueToUse()`：判断目标、路径和状态是否仍有效。
- `tick()`：通过端口推进寻路、朝向等动作；触发技能使用端口的能力请求，事实采集则发布语义事件。
- `stop()`：恢复本行为修改的重力、碰撞等状态，停止本行为的控制，避免模块卸载后残留。

短暂的寻路缓存可以放 Goal 实例，需随卸载、重启或转移保留的数据必须写回模块 customData。每次工厂调用创建新 Goal，禁止多个宿主共享同一个有状态的 Goal。

## 多模块争夺控制

`ModuleScheduledGoal` 只协调控制标记冲突的行为。高强度严格大于低强度，并且达到其两倍或差值至少 100 时，强者可持续占用；弱者仅在没有可执行的强者冲突行为时运行。未达到压倒门槛时按 100 tick 轮换；同一模块内部保留声明顺序。强度变化运行中生效，无需重装模块。

该调度在宿主适配层完成，行为类不得自己枚举宿主身上其他厉鬼。沉睡、死机、压制与目标吸引/屏蔽等通用条件通过行动端口解释。主动技能并不要求必须有 Goal：无法自主寻路的宿主仍可调用满足媒介的能力。

可对照 `GhostWanderGoal`、`KnockingGhostBehavior`、`CallingGhostBehavior` 和 `NightWandererBehavior`。它们的完整玩法仍由各自模块声明，不在宿主新增专用 Binding。
