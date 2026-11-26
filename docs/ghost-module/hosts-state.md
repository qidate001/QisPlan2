# 宿主适配与共享身体效果

[返回开发指南目录](../ghost-module-development.md)

## 让不同宿主携带模块

注册定义并不等于世界里已经出现了这只鬼。还需要安装实例或配置默认实例。

### 玩家与调试命令

在具有相应权限的开发环境中：

```text
/qisplan2 module list
/qisplan2 possess qisplan2:contact_echo
/qisplan2 module player info
/qisplan2 release qisplan2:contact_echo
```

`/qisplan2 module player` 当前提供查询入口，不要写成不存在的 `module player add` 命令。正常玩法的驾驭由统一驾驭流程完成，不为新模块另建 possession ability。

### 通用实体宿主

普通厉鬼使用 `ModularGhostEntity` 携带模块。只有确实需要独特物理模型或宿主组成时，才创建继承该通用类的实体。

自定义实体构造器可调用：

```java
super(type, level, composition,
        List.of(new GhostModuleData.Entry(ContactEchoModule.ID, 10, 0)));
```

这是构造器内的片段，`type / level / composition` 由具体实体提供。无需为每种规则重建一个专用实体行为类；缺少的物理行为应在适配层实现。

新增实体类型仍需正常注册属性、渲染器等宿主资源。注册模块本身不会完成这些 Minecraft 实体注册工作。

### 普通物品与剑

普通模块物品可继承 `GhostModuleItem`。默认模块通过数据组件设置：

```java
properties.component(
        ModDataComponents.GHOST_MODULES.get(),
        List.of(new GhostModuleData.Entry(ContactEchoModule.ID, 10, 0))
)
```

参考 `GhostDivinationSlipItem` 的实际构造方式。这个表达式用于物品属性配置，不需要把数据再写一份到模块定义中。

Java 不支持同时继承 `SwordItem` 和 `GhostModuleItem`。需要保留剑父类时实现 `GhostModuleItemSupport`，再正确转交 `useModules`、`tickModules` 等生命周期入口。

给现有物品安装模块可用：

```text
/qisplan2 module item add qisplan2:contact_echo
/qisplan2 module item info
/qisplan2 module item remove qisplan2:contact_echo
```

给物品添加实例与采集物理事实是两件事。普通右键物品入口发出的是 `GhostUseEvent`，不会自动变成接触规则；示例需要现有接触采集入口或为该物品补齐对应事实采集。

### 静态方块宿主

`StaticBlockModuleHost` 适合模块配置固定、无需逐方块保存实例状态的方块：

```java
new StaticBlockModuleHost(ContactEchoModule.ID, 10);
```

宿主适配或方块接触采集代码在服务端确认接触后分发：

```java
GhostModuleRuntime.dispatch(host, TouchEvent.sample(null, target, beginning));
```

`host / target / beginning` 由采集层取得。`beginning` 必须来自真实的进入/离开检测，不能每 tick 一律传 `true`，否则“开始接触”规则会每 tick 重复触发。

静态宿主没有实体形体，也不一定保存自身世界坐标。能力不能为了工作而强制向它索取实体来源。本例使用可信接触事件里的目标，并把实际袭击交给世界接口。

### 仅有独特外观的实体宿主

`ModularGhostEntity` 可直接继承，其受保护构造器接受 `HostComposition` 和默认模块列表。使用 GeckoLib 外观时，继承 `AnimatedModularGhostEntity`，子类只在构造器中声明这两项数据；参照 `CallingGhostEntity`。不要在子类复制喊名、袭击或追踪逻辑，也不要根据实体类型决定模块能否使用能力。移除默认模块后实体仍保留模型，空模块列表读档时不会重新补回。

实体类型在 `ModEntities` 注册并使用通用属性；刷怪蛋注册真实实体类型，不添加指向 `modular_ghost` 的 `ENTITY_DATA`。独特外观刷怪蛋排在实体厉鬼创造栏最前，原通用宿主和模块预设蛋继续保留。

客户端统一注册 `AnimatedModularGhostRenderer`。`AnimatedModularGhostModel` 按实体类型 ID 查找以下资源，例如 `qisplan2:calling_ghost`：

- `geo/calling_ghost.geo.json`
- `textures/entity/calling_ghost.png`
- `animations/entity/calling_ghost.animation.json`

具体外观实体自行覆盖 `registerControllers`，注册自己的待机、行走与动作控制器，并通过 `getVisualActions()` 声明支持的动作映射；同一映射用于注册 `action` 控制器的可触发动画。公共基类只提供缓存和 `playVisualAction` 同步入口，不规定动画集合或资源名。世界适配器报告动作时，当前外观未声明的动作直接忽略。例如喊人鬼外观搭载敲门模块时仍正常行走和敲门，但不会播放敲门动画，也不需要补充占位资源。现有资源使用 `animation.ghost.*` 名称，例如敲门使用 `animation.ghost.knock`。动画不会反过来驱动规则或判定能力成功。附着物由 `GhostGeoAttachmentLayer` 读取现有快照，在实体局部躯干锚点渲染。

### 宿主朝向接口

模块通过 `world().facePosition(Vec3)` 面向世界坐标，或通过 `world().faceDirection(Vec3)` 面向世界方向向量，不自行判断宿主类型。零向量、非有限数值或缺少有效世界时返回 false。

适配层解释具体操作：实体设置身体、头部和俯仰朝向，玩家同时同步视角；物品忽略操作，不转动携带者；方块有 `FACING` 时支持六方向，有 `HORIZONTAL_FACING` 时只使用水平分量，否则不操作。该接口只转向，不移动位置。喊名与敲门的世界操作已经在适配层调用它，其他能力可按需要直接调用。

## 完整模块与宿主的职责边界

`*Module.java` 是一只厉鬼的唯一声明入口：规则、媒介、公开和私有能力、鬼域、行为工厂、侵蚀和身体效果都从这里声明。复杂逻辑可以拆成同目录的辅助类；不能再到宿主包增加 `CallingGhostEntityBinding` 一类的第二份厉鬼定义，也不能维护按具体厉鬼 ID 分支的行为枚举。

### 身体效果与共享属性

`bodyEffects(context)` 返回 `GhostBodyEffects`，声明移动速度加值和短时药水效果。夜游鬼的明暗速度、失明和夜视已经使用这个入口；不再存在玩家版和厉鬼实体版两套公式。

`GhostHostProperties` 根据宿主实际组成和已承载模块计算侵蚀、普通伤害减免、生命上限加成及有效强度。自主模块完整发挥，受控模块按复苏比例发挥；此差异由通用承载关系声明，不能按玩家身份判断。`GhostHostBodyHandler` 只把身体自身承载的通用生命上限和减伤应用到该身体。`GhostLivingHosts` 区分 `INTRINSIC` 与 `CARRIED_ITEM`；手持或穿戴物品仍是独立宿主，其侵蚀和通用属性不会转移给携带者。装备通过媒介提供的显式能力效果（例如寿衣抵挡）仍照常执行；没有生命属性的载体不会被强行添加生命值。界面同样读取这个计算入口。

### 安装、生命周期与数据

玩家是 `PlayerGhostModuleHost`，通过宿主的 `possess(entry)` 接受完整实例，通过 `release(id)` 释放模块。原 `PossessionHandler` 已移除。`PlayerGhostData` 仅负责玩家附件持久化及旧界面/快照 DTO 的转换，不定义厉鬼玩法。

受控关系由 `controlsIntrinsicModules()` 声明，并非检查宿主是不是玩家。复苏达到上限由 `GhostModuleRuntime` 统一判断，再调用 `onControlLost(module)`；宿主落实其物理释放结果。模块不自行决定玩家死亡、实体删除或物品掉落。

`GhostModuleOperations` 提供跨宿主沉睡和复苏缓冲操作。缓冲存于 `customData.revivalReserve`（0..100 百分比），`GhostModuleContext.addRevival` 统一抵扣。旧 `qisplan2:player.shallowStun` 随模块数据读取迁移，不丢失其他独有字段，也不意味着新增一种死机状态。
