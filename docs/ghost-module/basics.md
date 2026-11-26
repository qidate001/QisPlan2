# 模块基础

[返回开发指南目录](../ghost-module-development.md)

## 模块到底是什么

**一个 `GhostModule` 代表一只完整的厉鬼。** 同一只厉鬼可以有多个规则、多个媒介和多个独立能力。不要给同一只鬼的每个技能分别创建一个模块。

例如鬼签是一个模块，活签、死签、鬼签是三个能力；开门鬼和关门鬼则是两个模块。

| 概念 | 职责 | 示例 |
| --- | --- | --- |
| 模块定义 `GhostModule` | 描述整只厉鬼、初始属性、规则、技能、承载形式 | 鬼寿衣、鬼签 |
| 实例 `GhostModuleData.Entry` | 保存当前强度、复苏、沉睡、死机、专属数据 | 玩家身上的这一只鬼签 |
| 规则 `GhostRule` | 决定何时自动发起能力或影响 | 接触开始、声音中断、受到袭击 |
| 媒介 `GhostMedium` | 无论哪个渠道调用，都必须满足的条件 | 正在接触、暴露在世界、覆盖表面 |
| 能力 `GhostAbility` | 一项具体技能的条件、元数据和世界效果 | 发动袭击、开关鬼域 |
| 语义事件 `GhostEvent` | 描述已经发生、经过采集确认的事实 | `TouchEvent` |
| 调用参数 `GhostAbilityParameters` | 描述一次操作所需的输入 | 指定目标、指定门 |
| 宿主与适配层 | 存数据、采集事实、实现世界操作 | 玩家、实体、物品、方块 |

### 最重要的边界

模块不判断自己由谁承载，也不取得宿主后再向下转型。

模块可以使用：

```java
context.getModuleContext().world();
context.getModuleContext().environment();
context.getModuleContext().targetState();
```

模块中不要使用：

```java
context.host();                         // 不取得宿主对象
// instanceof PlayerGhostModuleHost    // 不判断宿主类型
// new GhostItemModuleHost(...)        // 不创建宿主适配器
// GhostModuleHosts.living(...)        // 不绕路取回宿主实体
```

事件或参数中的目标实体是世界作用对象。允许处理目标，不等于允许查询模块自身的宿主。不要把 `GhostInvocationParameters.recipient()` 当作“获取宿主”的后门。

`Source.RULE / BEHAVIOR / POSSESSION` 表示调用来源，可以用于解释复苏代价；它不是宿主类型，不应被用来推断“这个宿主一定是玩家”。

模块定义在注册表中复用。`static final` 常量、不可变能力列表可以保存在定义里；某个实例的目标、冷却和成长值不能放进定义对象或全局静态字段。

## 最小定义与通用属性

编译上只要求实现 `getId()`，但新模块应明确声明初始数据、复苏策略和展示资源，避免无意继承默认值。一个可注册的最小定义如下；规则和能力在各自文档扩展。

```java
public final class ExampleModule implements GhostModule {
    public static final ResourceLocation ID=ResourceLocation.parse("qisplan2:example");
    @Override public ResourceLocation getId() { return ID; }
    @Override public GhostModuleInitialData getInitialData() {
        return new GhostModuleInitialData(50,0);
    }
    @Override public double getNaturalRevivalRate() { return 1; }
    @Override public double getMaxRevival() { return 100; }
}
```

| 声明 | 作用与默认值 |
|---|---|
| `getId()` | 唯一注册身份；必须实现 |
| `getInitialData()` | 新实例强度、复苏；默认 `(1, 0)`，不重置旧实例 |
| `getNaturalRevivalRate()` | 每秒自然复苏，默认 1 |
| `getMaxRevival()` | 复苏上限，默认 100 |
| `form()` | 默认独立式；替换式声明占据部位 |
| `displayName()` | 默认读取 `module.<namespace>.<path>` |
| `iconTexture()` / `suppressionIconTexture()` | 模块图标，返回 null 使用占位图 |
| `getRules()` | 注册时固定的规则定义，默认空列表 |
| `getIntrinsicAbilities()` / `getMedia()` | 能力与媒介声明，详见对应专题 |

定义对象全局共享，不能在字段里存某个宿主的当前强度或冷却；当前强度、复苏、沉睡、死机属于实例数据。持续对抗关系由共享框架维护。一个模块可以没有规则或没有公开能力，但不能把其组成拆成多个独立注册的“半只鬼”。

## 注册、语言与图标

### 注册模块一次

在 `GhostModuleRegistry.bootstrap()` 增加：

```java
register(new ContactEchoModule());
```

并添加导入：

```java
import com.qidate.qisplan2.ghost.module.implementation.example.ContactEchoModule;
```

重复注册相同模块 ID 会抛异常。注册表保存定义，不能为每个玩家注册一次。

### 添加中英文语言键

合并到 `src/main/resources/assets/qisplan2/lang/zh_cn.json`：

```json
{
  "module.qisplan2.contact_echo": "触响鬼",
  "ability.qisplan2.contact_echo.contact_attack": "触响袭击"
}
```

合并到 `en_us.json`：

```json
{
  "module.qisplan2.contact_echo": "Contact Echo Ghost",
  "ability.qisplan2.contact_echo.contact_attack": "Contact Echo Strike"
}
```

以上是要加入的键，不要覆盖整个语言文件。模块名称用 `module.*`，技能名称用 `ability.*`，二者是不同层次。

技能 ID `qisplan2:contact_echo/contact_attack` 默认推导出 `ability.qisplan2.contact_echo.contact_attack`。不要把资源 ID 对象直接当作 `Component.translatable` 的格式参数；需要显示 ID 时转换成字符串。

### 自定义图标

本例使用原版纹理，无需新增资源。自定义图标例如：

```java
ResourceLocation.parse("qisplan2:textures/gui/ability/contact_echo.png");
```

对应文件：

```text
src/main/resources/assets/qisplan2/textures/gui/ability/contact_echo.png
```

这是完整纹理路径，不是物品注册 ID，也不是模型 ID。技能图标应返回非空资源位置；模块卡片的 `GhostModule.iconTexture()` 允许为空，不代表技能图标也可以为空。

### 自动进入能力目录与轮盘

玩家实际驾驭并成功承载该模块后，`GhostAbilityCatalog` 自动收集 `isPublicSkill() == true` 的能力。无需修改 `ClientKeyHandler`、`GhostAbilityNetwork` 或新增专用技能包。

当前交互：按住 G 打开，鼠标悬停选择，松键或左键提交，中心/外部/Esc 取消。扇区显示图标和技能名称，中央显示选中的完整名称与所属模块。

配置页按厉鬼分组显示全部能力，支持滚轮与滚动条。新技能拖入扇区替换原技能，已装备技能拖入另一槽位交换位置；右键清空槽位，或将轮盘技能拖到中心移除。编辑自动保存，也可恢复默认或手动重试保存。

布局以模块 ID 与能力 ID 保存到客户端游戏目录的 `config/qisplan2-ability-wheel.json`，按玩家 UUID 区分。丢失能力时保留原槽为空，重新获得后恢复，不挪动其他技能；该文件不赋予能力，服务端仍校验归属、承载与媒介。

轮盘十个槽位另有快捷键，默认小键盘 1～9、0，可在按键设置中独立修改；读取玩家保存的槽位布局，空槽或已私有化能力不发送请求。移除或私有化旧技能后保留空槽，不自动移动其他技能。

### 承载形式与侵蚀

默认独立式不占承载部位。替换式例如：

```java
@Override
public GhostModuleForm form() {
    return GhostModuleForm.replacing(SpiritualPart.SKIN);
}
```

侵蚀独立声明：

```java
@Override
public GhostCorrosion corrosion() {
    return GhostCorrosion.builder()
            .add(SpiritualPart.SKIN, 60)
            .build();
}
```

占皮肤与侵蚀皮肤不是同一个概念。当前玩家和基础实体默认 21 个部位，`GLOBAL` 不计入承载部位；物品和方块默认万能部位。部位冲突会导致安装失败或已有旧实例失去运行资格。

详细规则见 [宿主灵异承载](../ghost-bearing.md)。不要在模块里写“玩家占皮肤、物品占万能部位”的分支，匹配由承载框架负责。

## 实例状态与存档

### 通用属性和专属属性分开

实例条目保存：

```text
id / intensity / revival / sleeping / deadlocked / customData
```

强度、复苏、沉睡和裁决死机不应在 `customData` 中重复保存。压制积累、沉睡剩余深度与短时袭击等框架状态存放在保留结构 `qisplan2:combat`，不要覆盖它。

`getInitialData()` 只提供新建实例初始值。修改定义初始强度不会自动重置已有存档。

### 正确读写 customData

对支持模块专属数据读写的宿主，使用上下文读副本、修改、写回：

```java
var module = context.getModuleContext();
var data = module.getCustomData();
int count = Math.max(0, data.getInt("successfulUses"));
data.putInt("successfulUses", count == Integer.MAX_VALUE ? count : count + 1);
module.setCustomData(data);
```

不要只 `putInt` 而不写回，也不要新建空 NBT 后覆盖整个 `customData`，否则会丢失其他框架状态。

保存目标 UUID 而不是长期持有实体对象；重新加载或执行前通过世界接口重新解析和校验。字段改版时用自己的版本字段和明确迁移规则，保留不认识的字段。

### 不可变宿主的真实限制

当前 `GhostModuleContext.getCustomData()` 和 `setCustomData()` 要求宿主实现 `GhostModuleCustomDataModifier`，不具备接口会抛异常。`setIntensity / setRevival` 同理要求 `GhostModuleDataModifier`。

`StaticBlockModuleHost` 当前不提供这些写接口，且不允许复苏刺激。不要在模块中通过 `instanceof` 判断静态宿主来规避问题，应选择合适的数据能力或扩展接口。

- 每个宿主必须独立保存的状态：为相应宿主提供持久化容器/接口。
- 与目标关联的累计接触、袭击时间：考虑 `module.targetState().read(target)` / `write(target, state)`，参考鬼地毯和鬼草丛。
- 是否每块方块独立计时、是否同类静态来源共享关系状态，取决于适配器的存储范围，必须在设计中明确。

本教程的完整模块不读写 `customData`，因此没有这项额外持久能力要求。

### 强度与复苏单位

- `intensity()`：实例保存的本质强度。
- `effectiveIntensity()`：通过共享宿主属性适配计算当前可发挥强度；受控关系与复苏比例由框架解释，不依赖模块判断玩家身份。
- `revival()`：模块保存的原始数值，默认上限 100；不要与旧驭鬼视图中常见的 0～1 比例混用。
- `getNaturalRevivalRate()`：定义声明的每秒自然复苏值，但具体宿主仍要有对应推进逻辑。

规则提交影响时与技能真正袭击时应采用一致的强度口径。本例两处都用 `effectiveIntensity()`。
