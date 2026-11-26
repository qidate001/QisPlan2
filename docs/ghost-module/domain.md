# 鬼域声明、能力与成长

[返回开发指南目录](../ghost-module-development.md)

### 鬼域：模块声明、世界能力与客户端表现

鬼域是模块身体的一部分。分层、覆盖竞争、隔绝、追踪和生命周期属于框架；伤害、袭击等世界效果属于模块能力；雨柱、后处理、雨声属于客户端表现。三者不能混在一个控制器中。

`domainDefinition()` 返回 `Optional.empty()` 表示没有鬼域；返回 `GhostDomainDefinition` 则由统一宿主适配器维护。模块无需注册另一种鬼域，也不需要自己创建 `GhostDomain`。生命周期由 `GhostModuleRuntime` 与宿主适配器负责，模块无需调用手动维护接口。

运行时定位使用“实际宿主＋模块 ID”。物品按物品栈身份区分，不能用携带者 UUID 代替宿主身份；关闭玩家的鬼域不能关闭其手中物品上的同名鬼域。`domainOpen / domainStrength / domainRadius / domainLayer` 保存在模块 `customData`，运行时 `GhostDomain` 由这些数据重建。旧驭鬼数据类型仅保留兼容视图与旧存档迁移用途。

当前适配范围：暴露在世界中的实体、玩家、物品和方块都可展开鬼域；化虹、指向目标升降层与持续重启的输入实现目前仍面向玩家。其他宿主调用这些出口会返回失败，模块不能为此判断宿主类型；扩展支持时应修改适配器与底层操作。

#### 完整的最小模块

下面是可以放入 `ghost/module/implementation/ExampleMistModule.java` 的完整示例。它使用现成的雾气后处理，只公开一个开关能力，不自动造成伤害：

```java
package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.ghost.domain.shape.SphereDomainShape;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.ability.DefinedGhostAbility;
import com.qidate.qisplan2.ghost.module.ability.GhostAbility;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.domain.GhostDomainDefinition;
import com.qidate.qisplan2.ghost.module.domain.GhostDomainShaderEffect;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** 示例厉鬼：声明鬼域身体与开关能力，不认识自己的宿主。 */
public final class ExampleMistModule implements GhostModule {
    public static final ResourceLocation ID = ResourceLocation.parse("qisplan2:example_mist");
    private static final ResourceLocation TOGGLE = ResourceLocation.parse("qisplan2:example_mist/toggle");

    /** 模块身份与着色器资源身份可以不同。 */
    @Override public ResourceLocation getId() { return ID; }

    /** 新实例的初始强度和复苏。 */
    @Override public GhostModuleInitialData getInitialData() {
        return new GhostModuleInitialData(30, 0);
    }

    /** 使用框架球形几何和接触语义，组合一个标准后处理效果。 */
    @Override public Optional<GhostDomainDefinition> domainDefinition() {
        return Optional.of(new GhostDomainDefinition(
                32, 1, false, SphereDomainShape::new,
                true, false, 0, true, true,
                List.of(new GhostDomainShaderEffect(
                        ResourceLocation.parse("qisplan2:ghost_mist"), "GhostMist"))
        ));
    }

    /** 同一能力可以由轮盘、快捷键或其他可信输入渠道调用。 */
    @Override public List<GhostAbility> getIntrinsicAbilities() {
        return List.of(new DefinedGhostAbility(
                TOGGLE,
                ResourceLocation.parse("minecraft:textures/item/ender_pearl.png"),
                context -> context.getModuleContext().world().canToggleDomain(),
                context -> context.getModuleContext().world().toggleDomain()
        ));
    }
}
```

完成集成还需要以下步骤：

1. 在 `GhostModuleRegistry.bootstrap()` 中添加 `register(new ExampleMistModule())`，并按需要导入类。仅注册模块，不再注册鬼域类型。
2. 在 `zh_cn.json` 和 `en_us.json` 中分别添加 `module.qisplan2.example_mist` 与 `ability.qisplan2.example_mist.toggle` 的译文。
3. 通过现有模块命令或宿主的默认模块组件安装该模块。玩家驾驭后，公开开关能力会进入能力目录；物品如果需要右键开关，应另声明响应 `GhostUseEvent` 的规则，参照 `GhostUmbrellaModule`，不能假设定义公开技能就自动绑定物品右键。
4. 本例直接复用 `ghost_mist.json/.vsh/.fsh`，所以无需新增资源或客户端注册。它只复用视觉，不继承鬼雾伤害能力。运行 `gradlew.bat check --offline`，然后在游戏中验证开关、跨维度、移除模块、隔绝与压制后的清理。

`GhostDomainDefinition` 的参数按顺序为：

| 参数 | 含义与实现责任 |
| --- | --- |
| `radius` | 新实例默认范围；运行时范围保存在模块数据 |
| `initialLayer` | 初始层数；框架统一限制在 1～10 |
| `automatic` | 未显式设置开关时，是否允许自主展开；仍受宿主自主许可和模块状态约束 |
| `shape` | 独立几何实例工厂，例如 `SphereDomainShape::new`，不要共享可变形状 |
| `touch` | 有效覆盖是否构成身体接触 |
| `sight` | 有效覆盖是否构成来源看见目标 |
| `exposureInterval` | 周期曝光间隔，以 tick 计；0 表示不产生周期曝光 |
| `entityVision` | 是否使用框架实体视觉 |
| `teleport` | 是否允许框架化虹 |
| `effects` | 不可变的独立效果声明列表；可以为空，也可组合多个不同效果 |

分层和传送的实现继续使用 `world().domain()`。模块可以声明调层能力，但不要复制框架的覆盖优先级或实体追踪算法。

#### 状态、语义和伤害

开关、强度、范围、层数使用模块 `customData` 中的 `domainOpen / domainStrength / domainRadius / domainLayer`，沿用旧字段。无显式开关时，框架结合 `automatic` 和宿主自主运行许可决定是否展开。玩家默认主动开启；实体、物品和方块共用适配入口。物品按实际物品栈身份区分，不能借用携带者的同名模块。模块移除、载体失效、收起物品或外部完全压制会收回运行时域，持久字段保留；主动开启的能力不因规则死机而失效。

只对追踪器确认的最终有效覆盖集合采集事实，已经被其他鬼域或隔绝规则排除的目标不会收到事件：

- `TouchEvent`：本体与鬼域共用接触采样，一个宿主对一个目标每 tick 最多累计一次。离开后再次进入重新产生 `BEGIN`，不补算离开的时间。
- `TargetSeenEvent(target)`：来源看见目标，与 `SourceSeenEvent(observer)` 方向相反。有实体观察者时，中间层还向目标宿主派发 `SourceSeenEvent(observer)`。
- `GhostDomainExposureEvent`：周期曝光事实，携带域 ID、来源、目标和域强度。静态来源的实体字段可为空，使用 `world.containsInDomain(event.domainId(), event.target())` 校验归属，不要仅凭事件类型实施效果。

鬼雾声明每 20 tick 采样，并由自己的能力调用 `world.hurt(target, ModDamageTypes.GHOST_MIST, 0.1F)`。`hurt` 是原版数值伤害，不提交灵异袭击影响；`world.attack` 才进入灵异袭击裁决。普通伤害仍受原版伤害和死亡事件处理。当前公开的鬼雾侵蚀技能复用相同的普通伤害能力。

### 鬼域等级、分层与空间对抗

`GhostDomainDefinition` 的 `initialLayer` / 存档 `domainLayer` 保留兼容名称，含义是鬼域等级（1～10）。等级参与对抗，并不自动提供多个可进入的层。默认 `layered=false`；需要分层时在声明后调用 `.withLayering(true)`，目前只有鬼眼声明此能力。非分层鬼域的占据层上限为 1，目标分层、来源分层和旧热键均检查此声明。

新实例默认所有分类允许进入、禁止离开。模块可用 `.withDefaults(new GhostDomainSettings(enter, leave, rules))` 声明自己的默认权限及自动化；已有实例已保存的设置优先，不被新默认值覆盖。

框架以 `GhostDomainConflict.Node` 的原始范围、来源位置、等级、强度和形状求解。模块不判断玩家、实体或物品宿主，也不参与双方身份区分。`GhostDomainManager.conflict()` 缓存结果，输入变化才重新求解；范围从原始值计算，不把上一帧的受压范围再次当作原始范围。

- 嵌套低等级内域：`floor(原始范围 / 2^等级差 × 内域强度/外域强度)`，不超过原始范围；差三级完全压制。高等级内域保留原始范围。同级嵌套双方按相对强度收缩。
- 外部同级交界：双方保留范围，交界处各自影响强度为 `自身强度 / 参与同级域强度之和`。
- 外部不同级、差距小于三级：低等级域使用 `(原始范围 - 原始范围/2^等级差 × 自身强度/双方强度之和) × (1 - 覆盖面积占比)`，覆盖越多，剩余范围越小。无交集时不执行此式。覆盖面积按水平投影圆交集计算；球体三维完全分离时不算交叠。
- 外部差三级：只扣除低等级域的交界领地；来源一旦进入高三级域，则整个域完全压制。已完全压制的域不再压制第三域。
- 双方来源互相在原始范围内时，用较小原始范围作为内域；范围相同则较低等级作为内域，同形同级保持对称交界。

`Result.owns` 决定实际领地，`weight` 决定当地有效强度。内域从外域中挖出领地，外域不能在内域中发出接触或曝光。边界检测用真实球体/圆柱交点，内部挖空边缘不当成外域禁出边界。能力效果经影响执行器应用局部倍率，模块持久强度不变；延迟裁决捕获产生事实时的倍率。

客户端使用同一求解器，网络同步原始范围。标准渲染效果共享 `DomainConflictGeometry`、`DomainConflictCuts`、`DomainConflictCount`、`DomainConflictIndex`；`domain_conflict.glsl` 沿视线按球体/圆柱交点分段积分实际归属与强度，外域不会在内域段渲染，内域外的外域段仍可见。专属效果参数前缀继续由模块效果声明负责，公共流水线不列举厉鬼类型。新增自定义后处理应复用该空间契约，不能仅按相机当前所在鬼域做全屏覆盖。


### 可选的鬼域成长策略

模块在自己的 `domainDefinition()` 中调用 `.withGrowth(GhostDomainGrowth.STANDARD)` 选择成长。目前仅鬼雾接入；鬼眼的多眼叠加、鬼伞的特殊等级机制不使用成长公式，未声明策略的模块保持原有行为。

默认门槛为 `ceil(50 + 20 × (1.5^(L-1) - 1) / (1.5 - 1))`，公式一级至十级的参考门槛为（不是维持默认等级的条件）：50、70、100、145、213、314、466、694、1036、1548。强度不被升级消耗。`GhostDomainGrowth` 提供 `requiredIntensity(grade)`、`maxGrade()`、`gradeFor(intensity)`；可选预设有 `incremental`、`linear`、`table`，也允许模块自行实现接口。预设仅在声明时计算门槛。

成长只看模块的基础强度，不看 `domainStrength` 或交界对抗的局部强度，避免压制与等级形成反馈循环。公式只控制默认等级以上的成长解锁，`initialLayer` 是永久下限：`最高可展开等级 = max(initialLayer, 公式解锁等级)`。鬼雾默认一级，即使强度为 1 或 5 也能展开一级，70 强度才解锁二级。默认六级、初始强度 5 的鬼域仍有六级，到 466 强度解锁七级，不能把成长门槛改成从默认等级重新累计。当前展开等级由技能选择，不自动升满；强度降低会收回超限的成长等级，但绝不低于默认等级。读档、运行时、主动调级和关闭状态下的等级保存均遵守上下限。鬼眼与鬼伞未声明成长，继续使用原有特殊机制。

模块通过 `GhostDomainAccess.maxGrade()` 查询当前上限，设置等级依然使用 `setLayer`。技能、面板、运行时管理器与关闭状态下的等级存档都执行相同限制。成长不意味着分层；鬼雾仍然只有一个可占据空间层。模块不需要知道宿主类型。


### 同源鬼域的对抗豁免

同源按实际物理来源标识判断，不按模块 ID、厉鬼种类或阵营判断。同一来源展开的多个鬼域跳过彼此的范围压制、三级完全压制、领地扣除及同级交界强度分摊，各自保留效果；与外来源的鬼域仍各自正常对抗。同一实体及其携带物品沿用实体来源 UUID，没有实体身份的静态宿主由适配层提供共用来源标识。模块只声明自己的鬼域，不读取这些宿主信息。

来源 UUID 已包含在鬼域同步数据中；服务端和客户端共同传入空间求解器。GPU 几何数据第二列的 w 分量保存本帧来源分组，保证渲染强度与服务端的同源豁免一致，不使用可能碰撞的 UUID 哈希。

## 鬼域内部的规则与能力

`GhostDomainDefinition.withProgram(GhostComponentProgram<GhostDomainAbilityContext>)` 声明鬼域自己的规则、能力和媒介，仍由完整厉鬼模块返回定义。上下文提供有效覆盖目标、等级、有效强度、事件及 `GhostWorldAccess`，不暴露宿主。

运行时按有效覆盖发送 `ModuleTickEvent`，按 `exposureInterval` 发送 `GhostDomainExposureEvent`。组件规则遵守来源模块的规则权限，死机、沉睡或高度压制会停用规则；常态压制作为持续能力由鬼域自身生命周期维护。

快速声明压制可使用 `withSuppression(new GhostSuppressionAura(minimumGrade, multiplier))`。鬼眼从 5 级开始、鬼雾始终以 1 倍有效鬼域强度压制覆盖内目标。框架复用有效覆盖集合维护来源关系，退出、空间裁剪、等级变化和鬼域关闭都会撤销对应关系。自己身体不被自己的鬼域压制。

更多组件与生命周期约定见 [诅咒与持续能力](curse.md)。
