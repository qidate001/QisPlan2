# 规则与语义事件订阅

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
- 看见/被感知：`EntityPerceivedEvent`。
- 门变动：`DoorChangedEvent`。
- 听见敲门：`DoorKnockHeardEvent`。
- 灵异声音变化：`GhostSoundEvent`。
- 受到灵异袭击：`SupernaturalAttackEvent`。
- 生命周期采样：`ModuleTickEvent`。

新增事件放到 `com.qidate.qisplan2.ghost.module.event` 独立类中，不嵌在具体模块内。

“目标刚转身”可以是事件；“请求释放转身杀技能”不是语义事件。“请求敲指定门”用调用参数，敲门后真的听到了声音才产生听声事件。

## 显式订阅：从注册到派发

每条规则必须实现 `eventTypes()`，返回稳定且非空的事件类型集合。定义在注册时读取一次，`getRules()` 不再是运行时动态查询接口。类型声明可以使用事件父接口，例如 `DestructionAttemptEvent.class`；具体的方块、物品、实体摧毁事件会自动命中它。规则同时订阅父接口与具体事件时只执行一次，规则顺序保持模块原声明顺序。

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
