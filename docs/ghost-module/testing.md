# 调试、测试与源码索引

[返回开发指南目录](../ghost-module-development.md)

## 调试、测试与常见问题

### 推荐开发顺序

1. 完成设计表，决定哪些是规则、媒介、技能、内部维护。
2. 写模块和真实能力实现，先使用已有世界接口。
3. 注册定义，增加双语语言键和图标。
4. 用开发命令安装/驾驭模块，确认目录和承载。
5. 验证自动触发、主动释放、失效条件、死机和保存恢复。
6. 最后接入专用物品外观、实体模型或新物理采集入口。

### 构建检查

项目根目录执行：

```powershell
.\gradlew.bat check --offline
```

`--offline` 要求依赖已缓存；新机器需要先准备依赖环境。

当前 `check` 包含编译、模块隔离检查和项目已有回归任务。`verifyModuleIsolation` 扫描 `implementation/**/*Module.java` 中的宿主访问与宿主适配包引用。扫描是基础约束，不是完整的静态分析；不要把错误依赖藏进 helper 来绕过它。

回归测试位于 `src/regression/java`，由 `FrameworkRegressionTest` 串联。新增测试类不会仅因放进目录就自动执行，需要接入该入口或独立 Gradle 任务。

当前仅保留有明确回归价值的框架与公共接口检查：承载关系、语义订阅、能力调用与媒介、裁决响应、压制权限和关系缓存、沉睡、诅咒、鬼域及附着物品的真实序列化。单个厉鬼的迁移步骤、固定数值和重复状态断言不再分别维护专项测试。统一任务为 `frameworkRegressionTest`，已接入 `check`；物品快照与着色器仍保留各自的独立任务。

### 至少验证这些行为

| 检查 | 预期 |
| --- | --- |
| 未发生触发事实 | 自动规则不执行 |
| 主动调用，媒介成立 | 进入同一能力实现 |
| 主动调用，媒介不成立 | 无效果、无不应支付的代价 |
| 目标死亡、跨世界、离开范围 | 安全失败 |
| 模块死机 | 自动规则停止，主动仍需检查媒介 |
| 模块被移除或承载冲突 | 不允许执行 |
| 延迟期间条件失效 | 延迟效果被拒绝 |
| 相同接口由不同假宿主实现 | 模块行为一致 |
| 两个实例同时使用 | 状态不串台 |
| 保存恢复、迁移宿主 | 实例属性和专属数据保留 |
| 多能力 ID 与语言键 | 不重复，所有实际声明的技能均可解析 |
| 第十一项能力 | 完整目录保留，可在总面板拖入任意槽位 |

编译通过不能替代真实客户端的鼠标、文字布局、声音和动画测试。

### 常见问题

**模块注册了，轮盘里没有？**

先检查玩家是否真的携带且可承载该实例、`getIntrinsicAbilities()` 是否为空、是否已在总面板配置到槽位、客户端模块数据是否同步。不能只看物品里有该模块就认为玩家已经驾驭。

**轮盘点了技能却失败？**

依次检查：模块归属与承载、媒介、`canExecute()`、输入目标、世界接口支持、真实返回值。尤其检查是否仍然把某个事件类型写成主动能力唯一可执行条件。

**没有事件参数会崩溃？**

轮盘 `event == null` 是正常情况。不要未经检查强转 `context.event()`；应提供主动目标解析路径，或者通过明确的媒介/参数要求返回失败。

**为什么规则没触发，却能主动放技能？**

这是预期设计。规则决定自动行为；主动调用直接执行同一个能力。媒介才是不能绕过的条件。

**为什么所有宿主都加了模块，却只有部分宿主能用某个技能？**

检查世界接口是否有对应实现，或物理来源是否提供了所需输入/媒介。修复适配层，不在模块里按宿主类型分支。

**为什么静态方块写 customData 报错？**

它当前不是可变专属数据宿主。按[实例状态说明](basics.md)选择关系状态或提供持久化能力，不能靠捕获异常吞掉存档失败。


## 现阶段边界与发布清单

### 当前边界

- 全部声明能力进入目录，不等于每个能力在没有媒介、作用对象时都能成功。
- 例如寿衣抵挡当前需要真实待处理的灵异袭击；轮盘没有实现“预存一次未来抵挡”的机制，不能仅修改显示就声称支持主动蓄盾。
- 当前鬼烛效果消费端还检查点燃和模块可用状态。新增“脱离点燃规则持续主动维持”的能力，需要同步定义持续期、代价及效果查询逻辑，不能仅调用一次 `emit()` 就认为已经完成。
- 题字仍依赖题字能力出口和可题字对象，注册到玩家模块列表不会自动获得该物理交互能力。
- 规则按语义事件共享裁决批次；主动能力按调用建立裁决批次，详见[灵异裁决与对抗](adjudication-combat.md)。
- `customData` 并非所有宿主都可读写，持续任务由规则的 `createTasks()` 工厂创建，运动任务要求 `GhostActor` 端口；模块不得引用原版 Goal 或具体宿主。

这些是当前实现需要明确面对的边界，不是建议在模块里绕过接口。后续扩展时应沿用“模块声明规则与效果，接口提供环境能力，宿主落实物理行为”的分工。

### 发布前清单

- [ ] 一个模块确实代表完整的一只厉鬼。
- [ ] 模块 ID、规则 ID、媒介 ID、能力 ID 稳定且无重复。
- [ ] 能力仅通过 `getIntrinsicAbilities()` 声明，无第二张主动技能表。
- [ ] 模块不获取、识别或保存宿主。
- [ ] 事件是事实，操作请求是参数。
- [ ] 规则声明 eventTypes，持续回调声明 abilityEventTypes，周期维护声明 ticksAbilities。
- [ ] 注册后的规则列表不随实例状态变化。
- [ ] 自动规则与主动输入共用技能实现，并分别验证目标路径。
- [ ] 每个媒介在所有调用渠道都有效。
- [ ] 实际效果失败时返回失败，代价时机符合设计。
- [ ] 内部维护、计时和数据更新没有伪装成轮盘技能。
- [ ] 持久状态放在正确存储范围，字段迁移不覆盖框架数据。
- [ ] 该宿主确实具备所需世界接口、事件采集和生命周期适配。
- [ ] 模块/能力译名齐全，图标资源存在。
- [ ] 承载与侵蚀声明符合设计。
- [ ] 编译、隔离检查、回归与必要的游戏内测试通过。
## 源码阅读索引

以下路径均相对于 `src/main/java/com/qidate/qisplan2/`：

| 主题 | 优先阅读 |
| --- | --- |
| 模块与注册 | `ghost/module/GhostModule.java`、`GhostModuleRegistry.java` |
| 能力 API | `ghost/module/ability/GhostAbility.java`、`DefinedGhostAbility.java` |
| 调用与媒介 | `GhostAbilityContext.java`、`GhostAbilityDefinitions.java`、`GhostAbilityExecutor.java` |
| 规则与裁决 | `ghost/module/GhostEventSubscriptions.java`、`GhostModuleRuntime.java`、`GhostRuleRuntime.java`、`GhostRuleContext.java` |
| 声明多个技能 | `ghost/module/implementation/eye/GhostEyeModule.java` |
| 同一模块不同结果能力 | `ghost/module/implementation/divinationslip/GhostDivinationSlipModule.java` |
| 接触与静态来源 | `ghost/module/implementation/GhostGrassModule.java`、`GhostCarpetModule.java` |
| 替换式与抵挡 | `ghost/module/implementation/GhostShroudModule.java` |
| 接口与适配 | `ghost/module/environment/`、`ghost/module/host/` |
| 技能目录与网络 | `ghost/module/ability/GhostAbilityCatalog.java`、`network/GhostAbilityNetwork.java` |
| 轮盘 | `client/screen/AbilityWheelScreen.java`、`AbilityWheelLayout.java` |
| 宿主承载 | `ghost/bearing/`；另见本文关联的承载文档 |

- `RuleLifecycleTest` 验证多入口、持久状态隔离、死机阻断、强度竞争、时间片轮换及中断只清理一次。
