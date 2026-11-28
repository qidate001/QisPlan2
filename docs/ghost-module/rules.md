# 厉鬼规则、状态机与语义入口

[返回开发指南目录](../ghost-module-development.md)

## 先设计规则、媒介、能力

先写设计表，再写类。本文的教学模块叫“触响鬼”：

| 项目 | 定义 |
| --- | --- |
| 模块 ID | `qisplan2:contact_echo` |
| 初始属性 | 强度 10，复苏 0 |
| 形式 | 独立式，使用框架默认值 |
| 规则 | 开始接触一个存活目标时，尝试发动袭击 |
| 媒介 | 具有有效接触目标 |
| 能力 | `qisplan2:contact_echo/contact_attack` |
| 主动输入 | 服务端视线目标或可信调用参数；仍要验证当前确实接触 |
| 复苏代价 | 实际击杀成功时增加 1 点复苏 |
| 持久状态 | 本例不需要额外状态 |
| 世界效果 | 调用 `world.attack()` |

“开始接触”是自动触发规则；“正在接触”是硬性媒介。主动调用可以越过前者，不能越过后者。

如果设计的是“累计接触十五秒才自动袭击”，累计时间应放规则及关系状态中。不要让主动袭击也必须先积攒十五秒，除非你确实希望十五秒成为所有渠道共用的能力前提。

## 语义事件与调用参数

### 事件必须是事实

先复用 `ghost.module.event` 中已有事件，再考虑新增：

- 接触：`TouchEvent`。
- 被看见：`SourceSeenEvent`；看见目标：`TargetSeenEvent`。
- 门变动：`DoorChangedEvent`。
- 听见敲门：`DoorKnockHeardEvent`。
- 灵异声音变化：`GhostSoundEvent`。
- 受到灵异袭击：`SupernaturalAttackEvent`。
- 生命周期采样：`ModuleTickEvent`。

新增事件放到 `com.qidate.qisplan2.ghost.module.event` 独立类中，不嵌在具体模块内。

“目标刚转身”可以是事件；“请求释放转身杀技能”不是语义事件。“请求敲指定门”用调用参数，敲门后真的听到了声音才产生听声事件。

## 显式订阅：从注册到派发

每条规则必须实现 `eventTypes()`，返回稳定的事件类型集合；普通规则不能为空，声明了 `carriedItems()` 的观察规则可以返回空集合。定义在注册时读取一次，`getRules()` 不再是运行时动态查询接口。类型声明可以使用通用事件父接口。摧毁直接订阅 `DestructionAttemptEvent.class`：方块、物品和实体的适配器发布的就是同一个事件类，不再携带可识别宿主的子类。规则同时订阅父接口与具体事件时只执行一次，规则顺序保持模块原声明顺序。

```java
@Override
public Set<Class<? extends GhostEvent>> eventTypes() {
    return Set.of(TouchEvent.class);
}

@Override
public boolean matches(GhostEvent event) {
    return event instanceof TouchEvent touch && touch.isBeginning();
}
```

`eventTypes` 负责订阅路由；`matches` 只筛选已收到事实的内容；`canTrigger` 判断模块状态和规则条件；`onTrigger` 提交能力或灵异影响。默认 `matches` 返回 true，没有额外事实限制时无需覆写。沉睡观察只收到已订阅事实，仍通过 `disturbedWhileSleeping` 判断是否被触犯，不执行本次反击。

`GhostEventSubscriptions` 在模块注册时建立“事件类型 → 模块与规则”的反向索引，同时建立“模块 ID + 规则 ID → 规则定义”的索引。具体事件首次出现时，仅沿该类的父类/接口树查表并合并、去重，之后复用不可变派发计划。不是每次扫描全部规则，也不通过调用所有规则的过滤器来发现订阅者。新模块注册会使具体事件计划失效并重新生成。

派发仍需读取宿主已安装模块的数据快照，将订阅者与安装关系相交，并保持宿主原顺序；这一步是成员查询，不向未订阅模块发送事件。不会缓存可变宿主的安装关系，因此移除模块立即生效。执行前继续校验承载、沉睡、死机和压制，持续能力与规则的权限互相独立。所有接收者依旧共享一次 `GhostInfluenceSession`，最后统一裁决。

ModuleTick 的压制计时、复苏失控、声音和鬼域生命周期属于宿主公共维护，并非隐式订阅所有模块规则。`SupernaturalAttackEvent` 的主动防护声明仍由对抗管线收集；事件订阅没有取代防护接口。不要为规避订阅而把逻辑搬到宿主维护中。

## 持续能力订阅

```java
@Override
public Set<Class<? extends GhostEvent>> abilityEventTypes() {
    return Set.of(GhostSoundEvent.class);
}

@Override
public boolean ticksAbilities() { return true; }
```

`onAbilityEvent` 仅接收 `abilityEventTypes` 声明的事实；`tickAbilities` 仅在 `ticksAbilities()` 为 true 时执行。两者默认都没有订阅，模块可分别选择。注册时声明不随某个实例的开关变化；实例尚未启动能力时，在回调内检查 customData 即可。不要订阅 `GhostEvent.class` 接收所有事件，除非这条规则确实具有全事件语义。


## 语义与底层事实的边界

事件表达对世界事实的解释，不表示某个 Minecraft 回调或模块内部步骤。一次事实可以由适配层解释为多个语义，例如鬼域覆盖可以同时产生接触与看见；多个底层事实也可以汇合为同一个语义。

- `DestructionAttemptEvent(attacker)`：即将被摧毁。订阅者只可读取责任者、请求取消；方块损坏、耐久回滚、实体死亡取消由各自入口落实。环境破坏的责任者允许为空。
- `GhostUseEvent(user, target)`：使用，target 可为空，是此次操作的作用对象，不是模块宿主。
- `SourceSeenEvent(observer)` / `TargetSeenEvent(target)`：被看见 / 看见目标，方向不能颠倒。普通视觉与特殊鬼域视觉可发布相同语义。
- `TouchEvent(target, phase, elapsedTicks)` 与 `EntityNearbyEvent(target)` 不携带来源宿主；鬼域暴露事件也只保留鬼域 ID、目标和强度。
- 鬼签的抽样是模块内部控制流：读取一次随机数，选择对应能力 ID，沿用原来的使用事件提交裁决。不存在“签文结果语义事件”。
- `ModuleTickEvent` 是调度通知，而非世界语义；仅用于明确订阅的维护逻辑，不能将“技能请求”包装成它来绕过媒介。

## 按需订阅携带物品

携带是持续事实，每次观察对一名匹配目标产生一次 `ItemCarriedEvent(target, item)`；不按相同物品的槽位或数量重复袭击。事件中的物品是快照，不暴露背包结构，能力媒介仍须重查实时条件。

```java
@Override
public Set<Class<? extends GhostEvent>> eventTypes() { return Set.of(); }

@Override
public List<CarriedItemObservation> carriedItems() {
    return List.of(new CarriedItemObservation(
            new AABB(-5, -5, -5, 5, 5, 5),
            CarriedItemSelector.tag(TagKey.create(Registries.ITEM,
                    ResourceLocation.parse("example:cursed_offerings")))));
}
```

也可使用 `CarriedItemSelector.items(id1, id2)` 指定物品 ID；`CarriedItemSelector.food()` 使用食物属性条件，鬼灶台采用此方式。自定义条件实现 `matches(stack, carrier)`，不得通过它识别模块自身宿主。

注册时构建模块 ID 到观察规则的索引。每轮只查询已安装、已承载、未死机且规则未被压制的观察计划；未暴露到世界不查询。同模块相同 AABB 的实体查询复用一次；同一规则声明多个观察条件时按“任一命中”处理，同一目标每轮只投递一次，命中物品条件后直接投递对应规则，和本轮其他事实共享裁决会话，不向所有携带规则广播再过滤。沉睡模块仍观察触犯并复苏，但不会反击。观察声明无需再订阅 `ModuleTickEvent` 或自行生成携带事件。


## 规则是行为程序，语义是输入

`getRules()` 是完整厉鬼行为的声明入口。一个规则可以订阅多个语义，也可以通过
`hasTasks()` / `createTasks(GhostRuleServices)` 声明持续过程。
收到事件不意味着重新启动：在不同阶段，同一事件可以更新目标、推进阶段或结束行为。
纯自主规则可以不订阅事件；注册索引不会为了执行任务而伪造语义广播。

瞬时规则继续实现 `canTrigger` 与 `onTrigger`，例如鬼签使用后只随机一次并调用三种能力之一。
复杂规则可继承 `GhostStateMachineRule`，实现 `initialState()`、`onEvent(context, phase, event)`，
通过 `transition(context, next, event)` 改变阶段。定义本身不保存任何实例状态。

```java
@Override
protected void onEvent(GhostRuleContext context, String phase, GhostEvent event) {
    if (event instanceof BeginEvent && phase.equals("idle")) {
        transition(context, "waiting", event);
    } else if (event instanceof FinishEvent && phase.equals("waiting")) {
        transition(context, "idle", event);
    }
}
```

这里的 BeginEvent / FinishEvent 仅示意：真实模块应订阅有世界含义的语义，
不要把内部“进入某阶段”包装成全局事件。状态成功保存后才调用 `onTransition`。
持久状态只决定行为进度，不构成释放能力的许可；能力执行器始终重新检查媒介与状态。
沉睡时仍走独立的触犯观察，不调用正常状态转移和攻击效果。
持续任务与中断清理详见[自主过程](autonomous-behavior.md)。
