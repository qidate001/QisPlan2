# 灵异裁决与对抗

[返回开发指南目录](../ghost-module-development.md)

## 灵异裁决与复苏代价

### 规则触发袭击

规则中优先使用 `submitAbilityInfluence()` 提交袭击/保护竞争。它先准备能力，准备失败不提交；裁决通过后延迟调用执行器，执行时再次检查媒介。

规则的 `submitInfluence()` 袭击入口会统一收集目标身上的有效保护诅咒；显式 `GhostInfluenceCollector.collectCurses(...)` 仍可使用，同一会话按诅咒实例去重。不要把“规则触发”直接等同于“已经成功击杀”。

`GhostModuleRuntime.dispatch(List<...>, event)` 可以把多个宿主放进同一影响批次。需要竞争同一个事实时，应由外部调度统一组织批次，而非让模块自己查找全部宿主。

### 主动能力的裁决声明

能力通过 `GhostAbility.activeInfluence(context)` 返回本次影响，声明目标、影响类型与实际强度。`GhostAbilityExecutor` 在主动渠道完成状态、媒介、执行条件检查后，收集持续保护并进行裁决；获准后重新校验并执行效果，复苏刺激只结算一次。被压制或死机的本次效果不执行。

```java
@Override
public GhostAbilityInfluence activeInfluence(GhostAbilityContext context) {
    return GhostAbilityInfluence.of(contactTarget(context),
            GhostInfluenceType.ATTACK, context.getModuleContext().intensity());
}
```

声明使用作用对象与抽象环境接口，不读取宿主类型。轮盘、热键和直接 `POSSESSION` 调用共用此入口。旧的模块级 `activeInfluence()` 已移除；不同技能可以分别声明保护、袭击或无裁决。

规则依旧共享其事件会话，不在执行器内重复提交。主动敲门的延迟袭击在实际执行时裁决。移动、开关鬼域、化虹、题字等不直接构成攻防竞争的操作返回 `null`；寿衣仍通过真实待处理袭击事件抵挡。不能把所有能力都强制当成袭击。

鬼签的生签声明保护、死签声明袭击、鬼签结果不声明竞争。生签与同源同强度死签相遇时写入模块死机并清理该生签，本次死签不执行；死机之后，玩家依然可绕过自动规则主动使用满足媒介的能力。

当前持续生签历史数据没有保存具体来源模块实例 UUID，来源区分兼容地依据提供者模块 ID；已经避免把鬼签保护与其他模块的袭击误认为同源。区分同类型但不同实例的来源仍需扩展诅咒存储，不能把模块类型标识当成完整的实例溯源。

### 三种刺激时机

| 时机 | 含义 | 适合情况 |
| --- | --- | --- |
| `ON_ACCEPTED` | 条件通过、执行效果前结算 | 开始释放就支付代价 |
| `ON_EFFECT_SUCCESS` | `executeWithResult()` 返回成功后结算 | 真实效果成功才付费 |
| `MANUAL_CHECKPOINT` | 效果执行到 `checkpointStimulation()` 时结算 | 明确阶段支付，一次调用只结算一次 |

刺激金额必须是有限非负数。执行器还检查宿主能否修改复苏、模块 `getRevivalPolicy()` 是否允许。不要把“声明了刺激”当作所有宿主都一定已支付。

效果成功与输入被接受不同：`tryExecute()` 表示执行被调用，`tryExecuteWithResult()` 才用于取得真实效果结果。技能需要成功才消耗标记、增加成长或蔓延时，应使用真实结果。

`prepareDeferred()` 可能返回 `null`。延迟 Runnable 执行时重新检查，不代表它自动提供一次性执行保证；调度层仍需避免重复排队。

## 声明模块参与灵异对抗的方式

对抗不是在 `tryKill` 中追加某个厉鬼的专用判断。模块通过 `getAttackResponses()` 声明响应，适配层只收集受袭目标当前承载的模块、装备和有效持续诅咒；没有声明的模块不执行对抗逻辑。

```java
/** 收到袭击时调用既有的私有保护能力。 */
@Override
public List<GhostAttackResponse> getAttackResponses() {
    return List.of((module, attack) -> module.reactAbility(PROTECT, attack.event()));
}
```

这里的 `module` 是 `GhostModuleContext`，只能使用状态和环境端口，不能获取或识别宿主。`reactAbility` 走规则来源的统一执行器，继续检查死机、沉睡、媒介与能力条件。持续主动能力如白烛则根据已启动状态与暴露媒介声明响应，不能无条件绕过状态判断。

`GhostAttackResponse.order()` 决定先后顺序，默认 100。当前约定屏蔽使用 0，支付代价的吸收使用 100，累计型持续保护使用 200；相同顺序保留收集次序。不要依赖具体厉鬼 ID 排序。

响应使用 `attack.event().remainingIntensity()` 获取当前剩余强度。支付代价成功后调用 `absorb(amount)`，完全抵挡可调用 `block()`。支付失败不改变袭击。前层完全阻断后，后层不执行、不会重复扣费。未声明累计型保护时不统计其历史；`attack.recentIntensity()` 在一次袭击中最多记录一次。

脱离原宿主持续存在的影响，可以由 `GhostInfluenceProvider.attackResponse(remove)` 提供响应。诅咒只桥接存续状态、强度和注销出口，具体保护政策仍可放在所属模块中。返回响应后，自动收集器不会再将其作为静态强度保护重复提交。生签保留“最近一秒累计低于保护强度时抵挡，达到上限击穿”的政策；同源强度相当时优先死机并注销该保护。

### 使用真正的裁决强度

`GhostInfluenceOutcome` 包含状态、原始强度、最终有效强度与抵挡量。新回调可用 `session.submitResolved(...)` 接收结果；主动调用可用 `GhostAbilityInfluence.executeResolved(...)` 接收最终强度。能力内部也可调用：

```java
double actual = context.resolvedIntensity(target.getUUID(), originalIntensity);
```

不要把模块 `intensity()` 改成临时剩余值：它仍代表模块本身强度。最终伤害入口会领取同一同步效果的裁决结果，避免寿衣重复付费、生签重复累计；同一回调的第二次实际伤害需要重新结算。嵌套裁决独立保存并恢复上下文，异常退出也会清理。

同步作用域不能跨 tick 或线程保留。延迟能力应在真正生效时重新提交影响，不能保存“已获准”状态后跳过目标的新防护。当前同源政策仍按同类灵异判定，尚未引入跨宿主保存的模块实例身份；不要把会话来源 UUID 当作永久实例 ID。


## 独立附着来源与持续压制

插在目标身体上，不等于被目标驾驭。`AttachedGhostHost` 保存原物品模块，目标只是附着锚点。`GhostAttachmentRecord` 保存独立来源 UUID、完整物品组件及回收标记；它不占用目标承载部位，也不加入玩家能力目录。插入、拔出、加载及同步时更新关系，稳定后只读缓存，不用 tick 刷新死机。

模块通过 `getSuppressions(GhostModuleContext)` 声明持续压制。媒介通过 `world().attachments().embedded()` 查询；普通压制使用有限强度，无解压制使用 `GhostSuppression.absolute()`。模块不知道承载它的是什么，也不处理具体存档、实体或网络。

棺材钉的公开 `coffin_nail/embed` 能力通过 `GhostAttachmentAccess` 查询并实施插入；同一个模块可以放在支持该端口的宿主中。当前实装物理端口支持手持物品来源，未实现物理插入的宿主返回媒介不成立，不在模块内猜测宿主。交互适配器统一发布 `GhostUseEvent(user, target)`，并查询 `abilityEventTypes()` / 规则订阅索引决定是否投递；空目标代表无指定作用对象的使用。无需额外声明实体交互开关，它不是插入请求本身。

已建立的插入关系由物理媒介维持，源模块死机不自动拔钉。来源存放于附着容器内，不执行普通背包 tick，也不能被目标当作自身模块调用。首版实体交互限制一个插入槽位；存储采用列表，保留多附着扩展空间。拔出、死亡掉落及原宿主最后一个模块被驾驭时返还原物品，不生成默认的新物品；创造模式插入不消费，也不产生可回收副本。

`GhostModuleHost.suppression(moduleId)` 提供已建立关系的状态。行动、规则、能力分别检查权限：基础禁止自主行动，高度同时禁止规则，完全同时禁止能力。主动调用绕过旧死机不代表可以绕过压制。`controlsIntrinsicModules()` 仅由真实驭鬼适配器声明，允许主动调用豁免体内压制，但外来完全压制仍然有效。

附着模型由模块的 `attachmentModel()` 声明，客户端统一注册和渲染；人形使用胸部骨骼，其他原版生物模型提供保守锚点。模型只负责表现，不参与压制计算。棺材钉立体模型直接复用现有锈蚀贴图，背包物品模型保持原样。

本次实现附着来源的稳定身份与压制权限基础，不代表已经完成所有模块的永久实例身份、钢琴压制关系或循环对抗求解器。扩展这些功能应继续沿关系接口实现，不能回到实体上的专用布尔量。


## 压制入睡与沉睡规则观察

灵异防御数值和临时死机倒计时已移除。显式 SUPPRESSION 或 AUTO 选中压制分支时，以裁决剩余强度建立普通压制，持续 20 tick；再次命中替换本次强度并刷新到期时间，不叠加时长。到期仅解除这一短时来源，音乐与附着物等关系仍独立有效。

普通完全压制改为 `P ≥ 1.5 × S`（且 P > 0），基础与高度仍保持严格大于。令 `q = (P / (1.5 × S))²`，连续完全压制达到 `6000 / q` tick 后进入沉睡；恰好1.5倍对应五分钟。完全压制中断便清零入睡进度。无解压制单独取 q=20，即十五秒入睡，不向 NBT 保存无穷大。

初次沉睡获得六十秒自然苏醒深度。无完全压制时每个有效宿主 tick 自然减少 1 tick 深度；完全压制期间默认每 tick 净增 `0.5`，即压制 2 秒积累 1 秒沉睡。普通与无解压制采用相同积累速率，入睡门槛仍使用 q。模块可覆盖 `suppressionSleepAccumulationRate()` 声明非负有限速率。普通上限为 `12000q` tick（基础十分钟，三倍强度时四十分钟），无解上限二十四小时。更弱来源接管不会瞬间抹掉已有深度；超过新上限时自然恢复。稳定上限不重复写入状态。卸载/离线不补算时间，数据随模块保存和转移；恢复宿主生命周期后继续。

`entry.sleeping()` 是唯一沉睡开关，`qisplan2:combat.sleepTicks` 只是对应的自然苏醒剩余深度；不要另建一份沉睡布尔量。沉睡恢复不修改玩家原有失控复苏值，也不解除裁决 deadlocked。旧 stunTicks 迁移为沉睡深度，旧 permanentStun 迁移为模块 deadlocked，迁移后移除旧字段。调试命令由 `/qisplan2 stun` 改为 `/qisplan2 sleep`。

新增 `GhostRule.disturbedWhileSleeping(context, event)`：返回 true 表示本次确实触犯沉睡规则。运行时只调用此观察方法，不执行 `onTrigger` 或反击。默认离散事件复用 canTrigger，ModuleTick 默认不算触犯。以 Tick 查询目标、累计接触或等待开门的规则必须覆写，允许维护接触/计时数据，但禁止操作世界、提交影响、释放能力。鬼地毯继续累计满十五秒才惊醒；鬼灶台必须确有携食目标；鬼门必须计时到点且袭击线上有人。鬼寿衣的受袭观察也从统一对抗管线接收事实，不会重复抵挡。

触犯每秒最多削减一次沉睡深度，削减 `5 × sqrt(max(1, S))` 秒，越强越快苏醒。即使本次耗尽沉睡深度，也不会用本次事件反击，下一次新的事实才恢复正常规则。普通自动行为与持续能力停止；驾驭者主动能力仍遵守原有主动调用策略和压制权限，不能绕过外来完全压制。

状态推进接入宿主已有 ModuleTick，不能因为 AI 被禁用而停止 Tick 或环境事实派发。模块定义只读取世界端口与自己的数据，不识别宿主类型。关系求值继续复用缓存，不新增全世界厉鬼扫描。

## 明确袭击意图

`tryKill` 与 `world.attack` 现在只表示纯灵异袭击，对实际承载厉鬼模块的目标无效，包括驾驭了厉鬼的玩家。它们不会再隐式转换为一秒压制。

使用 `world.affect(target, damageType, strength, GhostAttackMode)` 选择：

- `ATTACK`：只袭击非厉鬼。
- `SUPPRESSION`：只对目标的厉鬼模块及持续诅咒建立短时压制，不杀死普通生物。
- `AUTO`：按目标是否实际承载厉鬼模块，在提交裁决前选择压制或袭击。

能力的 `activeInfluence` 也可调用 `GhostAbilityInfluence.of(target, mode, strength)`，其意图必须与最终世界操作一致。压制拥有独立 `GhostInfluenceType.SUPPRESSION`，参与保护对抗；不能声明 ATTACK 却在获准回调执行 SUPPRESSION。

一次性压制持续一秒，连续命中刷新到期时间，不累计时长。持续鬼域和诅咒通过来源关系维持压制，不能每秒补一次短时袭击模拟。诅咒的独立强度与状态见 [诅咒与持续能力](curse.md)。

## 死机后的复苏回落

死机不把复苏值锁死为零。主动能力仍经过媒介与正常代价结算，随后共享状态运行时按每秒 `deadlockedRevivalDecayPerSecond()` 回落，默认等于复苏上限的 10%。该速度不乘鬼签的正向复苏倍率，不补算离线时间；归零后停止写入，不会解除死机。模块可以覆盖该非负有限数值。死签使用 AUTO，能力声明、规则提交和世界操作保持一致，因此对凡人与驭鬼者都能进入生签对应的同源对抗。
