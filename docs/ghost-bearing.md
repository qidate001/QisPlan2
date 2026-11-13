# 宿主灵异承载

## 核心职责

- SpiritualPart：资源标识表示部位，可以自由声明自定义部位。
- HostComposition：宿主组成及每部位的容量。
- GhostModuleForm：默认独立式不占位；替换式声明所需部位。
- GhostBearing：统一匹配、分配和安装检查，模块不识别宿主类型。
- GhostCorrosion：使用部位标识声明侵蚀数值，与占位容量分开处理。

GLOBAL 仅为全局侵蚀来源，不是承载部位。玩家和基础实体有 21 部位；物品和方块暂用 UNIVERSAL。内置容量均为 1，占位使用列表，预留未来多模块容量。

## 扩展方法

鬼寿衣声明：

    @Override
    public GhostModuleForm form() {
        return GhostModuleForm.replacing(SpiritualPart.SKIN);
    }

特殊模型实体继承 ModularGhostEntity，并在构造器调用：

    super(type, level, composition, defaultModules);

自定义组成示例：

    SpiritualPart core = new SpiritualPart(
        ResourceLocation.fromNamespaceAndPath("qisplan2", "core"));
    HostComposition composition = new HostComposition(Map.of(core, 1));
    List<GhostModuleData.Entry> defaultModules =
        List.of(new GhostModuleData.Entry(moduleId, 30));

模块需先注册，默认模块不满足组成约束时拒绝错误配置。基础实体仍为空白模块列表。
新实体进入世界时绑定默认模块行为。读档有明确空列表时，不重新补回已经移除的默认模块；缺少整个模块字段时才保留构造默认值。
独特模型的渲染注册仍由各实体类型负责。

其他宿主可覆盖 GhostModuleHost.composition()；对应可变容器使用 new GhostModuleContainer(this::composition)。

## 分配约定

1. 优先精确匹配部位，缺少该部位时才使用万能部位。精确部位已满不会绕路占用万能部位。
2. 容量不足拒绝新增，不覆盖已有模块。独立式不占位。
3. 死机、沉睡及强度变化不释放部位；移除才释放。
4. 驾驭开始前和真正导入时均检查。导入失败不会移除来源模块。
5. 宿主之间独立占位：物品本身的部位不是穿戴者的部位。

host.bearing().occupants() 查询占位列表；rejected() 查询无法承载的模块。

## 存档

沿用原模块存档结构，保留实例属性与 customData。占位按宿主组成、模块形式和保存顺序重建，不保存第二份占位数据。普通状态更新保持条目顺序。

loadEntries 是旧数据恢复入口：冲突或失配实例不丢弃，但没有承载资格时不执行规则、主动能力或 Goal。移除冲突来源后，保留实例可重新获得承载资格。
运行时批量导入用 replaceEntries，新增冲突时原子失败。

## 验证

运行 gradlew.bat check --offline。
GhostBearingTest 覆盖自定义组成、万能部位、独立式、互斥安装、容量扩展、死机占位、移除释放、批量原子失败、旧数据恢复、顺序稳定和主动调用限制。
