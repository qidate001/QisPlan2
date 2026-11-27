# 模块所属的诅咒与持续能力

诅咒是完整厉鬼留下的持续能力实例。它有独立 UUID、目标 UUID、强度、来源和寿命；原宿主不在场，实例仍可继续存在。它不会被安装进被诅咒者的内在模块容器，因此承受诅咒不等于驾驭来源厉鬼。

## 从模块声明

在 `GhostModule.curses()` 返回稳定的 `List<CurseType>`。只注册完整的 `GhostModule`，不要再调用独立诅咒注册函数。

`CurseRegistry` 现在是模块声明的派生索引；类型 ID、所属模块、命令补全和存档解码都从这里查询。类型 ID 必须唯一。旧生签 `qisplan2:ghost_divination`、墓碑 `qisplan2:ghost_tombstone` 保持兼容，鬼签复苏诅咒使用 `qisplan2:ghost_divination_revival`。

类型提供 `create(target, source, initialState)` 和 `load(tag)`。实例通过 `CurseManager.add(server, curse)` 加入世界。`icon()` 与 `detectable()` 也归声明，客户端不再注册另一套诅咒类型。

## 规则、能力、媒介

`CurseType.program()` 返回 `GhostComponentProgram<CurseAbilityContext>`。同一个组件程序模型也用于鬼域：

- `Rule(eventClass, condition, abilityId)`：订阅语义事实并决定是否调用能力。
- `Ability(id, medium, effect)`：媒介成立才执行效果，直接调用与规则调用均经过这个检查。
- 定义创建时校验重复能力 ID 与不存在的能力引用，按事件类型建立订阅索引。
- 具体事件类型的继承路由缓存，不在每次事件发生时广播给全部规则。

诅咒上下文只提供实例/目标 UUID、诅咒强度、语义事件、对抗上下文和操作回调，没有宿主对象。不能通过来源类型判断应使用哪套厉鬼效果。

目前运行时提供周期 `ModuleTickEvent`，以及保护响应中的 `SupernaturalAttackEvent`、`SupernaturalSuppressionEvent`。后者表示已经形成的压制事实，不是技能请求。持续时间独立于能力程序推进，不能在能力 tick 中重复扣寿命。

需要直接调用时使用 `CurseManager.execute(server, instanceId, abilityId, event)`，它会检查实例有效性、运行状态和媒介。不要绕过运行时直接调用程序来执行世界效果。

## 当前三个持续诅咒

| 诅咒 | 持续效果 | 结束/沉寂 |
|---|---|---|
| 生签 | 受到袭击或压制时运行保护能力；强度及同源冲突沿用鬼签声明 | 停止提供保护 |
| 鬼签 | 运行时共享复苏倍率为 2；重复使用刷新五分钟寿命，倍率不叠乘 | 倍率立即停止参与计算 |
| 鬼墓碑 | 沿用土质媒介下沉与传送；目标扩展为生物 | 释放穿透状态，避免移除后仍无碰撞 |

生签对袭击保留最近一秒累计强度政策；对持续压制比较该压制的强度，不把每次权限查询累加为新的攻击。无解压制不靠无穷大数值参与保护计算。

## 运行与沉寂

`Curse.status(server)` 根据目标当前关系与**该诅咒自己的强度**计算状态。压制达到基础等级即暂停该诅咒效果；解除后自动继续。目标未加载时同样暂停执行，但不会强制加载区块。有限寿命在暂停时仍流逝。

诅咒会受到声音、附着、鬼域、其他诅咒以及短时压制的共同影响。自己声明的常态压制不会反过来压制自己的实例。HUD 同步沉寂状态，`/qisplan2 curse list` 展示类型、强度、剩余 tick 和状态。

## 常态压制预设

`CurseType.suppression()` 可选择 `new GhostSuppressionAura(1, multiplier)`。框架以实例 UUID 维护它对目标的持续关系；移除、沉寂、失效时撤销。当前生签、鬼签、墓碑均保持 `NONE`，不会额外压制目标。

鬼域使用相同的预设，通过 `withSuppression(...)` 声明。鬼眼选择 `(5, 1)`，鬼雾选择 `(1, 1)`：直接采用当前鬼域有效强度，包含交界处的强度削弱。

## 删除与持久化

- 删除指定实例：`CurseManager.remove(server, instanceId)`。
- 按目标删除：`removeTarget(server, targetId, typeId)`；`typeId == null` 删除该目标全部诅咒。
- 命令：`/qisplan2 curse remove <实例UUID>`，不再只显示成功而不执行删除。
- 删除、到期和沉寂调用 `suspend(server)` 释放临时效果；自定义实例应实现可重复调用的释放操作。
- 世界保存保留类型、实例 UUID、目标、来源、强度和寿命。特殊数据由实例 `save()` 与类型 `load()` 对称保存。
- 计时变化只标记待保存，实际写盘时构建快照，不在每次寿命变化时重建全服 NBT。

参见 [规则](rules.md)、[裁决与对抗](adjudication-combat.md)、[鬼域](domain.md)。


## 死亡后的保留声明

在模块提供的 `CurseType` 中覆写 `persistsAfterDeath()` 即可声明死亡后是否保留，默认返回 `false`。
生签与鬼签复苏诅咒使用默认值，鬼墓碑返回 `true`。此策略对玩家和其他生物目标一致。

```java
/** 目标死亡后保留同一诅咒实例。 */
@Override
public boolean persistsAfterDeath() { return true; }
```

框架由 `QisDeathHandler.cleanup` 统一调度死亡清理，诅咒管理器仅按声明移除实例、释放效果和压制关系并更新存档。
普通死亡、强制死亡以及跳过掉落阶段的死亡兜底均进入此入口；诅咒无需自行订阅死亡事件。
死亡被取消时不清理。保留实例通过目标 UUID 延续，重生不会重置其强度、寿命与独有数据，
玩家重生后框架重新同步展示。死亡期间保留实例处于沉寂状态，既有寿命规则继续生效；
“死亡后保留”不代表永不过期，也不绕过来源失效规则。
