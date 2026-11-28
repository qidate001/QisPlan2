# 鬼域渲染与效果

[返回开发指南目录](../ghost-module-development.md)

#### 独有效果怎样扩展

`GhostDomainEffect` 是开放接口，**不是框架枚举，也不是一组雨、雪、雾开关**。效果声明必须是服务端安全的数据，不得引用 `Minecraft`、`ShaderInstance` 等客户端类。

当前鬼伞的声明组合为：

```java
List.of(
        new GhostDomainShaderEffect(ID, "GhostUmbrella"),
        GhostRainEffect.INSTANCE
)
```

两项声明有各自的实现：

| 声明 | 客户端适配器 | 负责内容 |
| --- | --- | --- |
| `GhostDomainShaderEffect` | `StandardDomainShaderAdapter` → `ModuleGhostDomainRenderEffect` | 加载 Shader、筛选所属模块的域、绑定标准纹理与参数、交给总管线绘制 |
| `GhostRainEffect` | `GhostRainClient` → `GhostRainRenderer` | 鬼雨专属雨柱、雨声、事件监听和清理；当前雨柱实现使用圆柱几何 |

新增一个独有效果的完整路径为：

1. 在该效果所属模块的包中创建实现 `GhostDomainEffect` 的 record 或类，声明它自己的参数；无参数效果可像 `GhostRainEffect` 一样用单例 enum。
2. 在客户端包实现 `GhostDomainClientEffect<你的声明类型>`。`install(moduleId, definition)` 负责安装该效果；需要 Shader 时覆盖 `registerShaders(event, definition, loaded)`，需要世界渲染、声音或粒子时在自己的适配器中安装相应事件。多模块共享同一监听器时必须防止重复注册，退出世界时清理表现状态，参照 `GhostRainClient`。
3. 在 `QisPlan2Client` 的客户端效果绑定区添加一次 `GhostDomainClientEffects.register(声明类型.class, 适配器)`，位置在 `installModules()` 之前。这里注册的是一种声明的客户端实现，不是再注册厉鬼或鬼域。其他模块复用此效果不必再次注册适配器。
4. 模块在 `domainDefinition().effects()` 中提出该效果。`GhostDomainClientEffects.installModules()` 自动读取所有模块的效果列表；未绑定客户端实现会明确报错，不会静默忽略。匹配采用声明的精确运行时类型。

新增第十种独有效果时，仍只增加该效果的声明、实现和一次客户端绑定，不修改 `GhostDomainDefinition`、`ModuleGhostDomains` 或总渲染管线。影响服务端世界的效果应继续声明为模块能力，通过语义事实或持续能力入口调用，不能藏进客户端效果。

#### 标准着色器前缀到底由谁实现

前缀现在只属于可选效果 `GhostDomainShaderEffect(shaderId, uniformPrefix)`，不再是 `GhostDomainDefinition` 的字段，也不是所有效果必须遵守的约定。

调用链与代码位置是：

1. **模块提出声明**：例如 `new GhostDomainShaderEffect(ID, "GhostEye")`。
2. **客户端绑定声明类型**：`QisPlan2Client` 将 `GhostDomainShaderEffect.class` 绑定到 `StandardDomainShaderAdapter`。
3. **加载资源**：`QisPlan2` 的 Shader 注册事件调用 `GhostDomainClientEffects.registerShaders(event)`，后者委托 `StandardDomainShaderAdapter.registerShaders` 加载声明中的资源 ID。共享的 Shader ID 每次资源重载只加载一次。
4. **安装表现**：`StandardDomainShaderAdapter.install` 创建 `ModuleGhostDomainRenderEffect(moduleId, shaderId, prefix)`；模块 ID 用于筛选鬼域，Shader ID 用于查资源，两者不能混用。
5. **设置参数并绘制**：`GhostDomainRenderPipeline` 准备公共输入，随后调用效果的 `setupSamplers` 和 `setupUniforms`；真正拼接前缀、写参数的是 `ModuleGhostDomainRenderEffect`。管线不识别鬼眼、鬼雾或鬼雨。

以上实现位于 `ghost/domain/client/renderer/`；公共声明位于 `ghost/module/domain/`。标准适配器遵守以下资源契约，其中 `P` 为声明的前缀：

| 输入名称 | 数据来源 | 负责实现 |
| --- | --- | --- |
| `DiffuseSampler` | 场景颜色 | 总渲染管线 |
| `MainDepthSampler` | 复制的主场景深度 | 总渲染管线 |
| `P + RegionIdentity` | 灵异隔绝空间身份纹理 | `ModuleGhostDomainRenderEffect.setupSamplers` |
| `P + ProjMat` / `P + ModelViewMat` | 当前投影矩阵 / 已捕获的世界模型视图矩阵 | `setupUniforms` |
| `P + CameraPos` | 相机世界位置 | `setupUniforms` |
| `P + DomainCenter` / `DomainRadius` | 鬼域中心 / 客户端平滑后的范围 | `setupUniforms` |
| `P + DomainActive` / `DomainLayer` / `DomainStrength` | 启用值 1 / 层数 / 鬼域强度 | `setupUniforms` |
| `P + Time` | 世界时间秒数，按一天循环 | `setupUniforms` |
| `P + SourceRegion` | 来源空间身份；开放空间为 0，否则编码为 `(regionIndex + 1) / 256` | `setupUniforms` |

标准适配器要求 Shader 声明 `P + RegionIdentity` sampler；其他未使用的 uniform 可省略。矩阵、向量与标量的类型和数量必须与代码一致，参照 `ghost_mist.json`。不要通过其他 uniform 是否存在来猜测 sampler。新的专属 sampler 或参数布局应由自己的 `GhostDomainClientEffect` 和 `GhostDomainRenderEffect` 实现，在各自的 `setupSamplers/setupUniforms` 中绑定，不向总管线加入模块名称分支。

#### 从现有 Shader 构建自己的资源

要让上面的示例使用自己的雾气资源，可按以下步骤完成，不需要修改通用 Java 渲染代码：

1. 将 `src/main/resources/assets/qisplan2/shaders/core/ghost_mist.json`、`ghost_mist.vsh`、`ghost_mist.fsh` 复制为同目录下的 `example_mist.json`、`example_mist.vsh`、`example_mist.fsh`。
2. 将 JSON 中 `vertex` 与 `fragment` 的资源 ID 改为 `qisplan2:example_mist`。
3. 在这三个新文件内将 `GhostMist` 参数前缀统一替换为 `ExampleMist`。不要改公共输入 `DiffuseSampler`、`MainDepthSampler` 或顶点属性 `Position`。JSON 的 sampler/uniform 名称必须和 GLSL 完全一致。
4. 将模块声明改为 `new GhostDomainShaderEffect(ID, "ExampleMist")`，然后修改新 `.fsh` 中颜色、噪声等视觉计算。保留已有深度、鬼域边界和空间身份判定，可避免效果穿透隔绝边界或影响全屏。
5. 在资源重载或重新启动客户端后验证。模块、Shader 与语言资源都已接通，不需要额外调用旧的逐鬼域 `register()`。

这种方式复用的是标准输入绑定。若自己的效果完全不使用此参数约定，声明自定义 `GhostDomainEffect` 并实现独立适配器即可，不能为了省事给标准前缀适配器不断增加专属判断。

## 空间对抗与隔绝遮罩

标准效果通过 `DomainConflictGeometry / DomainConflictCuts / DomainConflictCount / DomainConflictIndex` 使用共享的实际领地结果。公共 GLSL 沿球体/圆柱交点分段积分，内域扣除外域区段，同源不削弱彼此。不要在效果中仅按相机所在域关闭整个屏幕的另一种效果。

共享导入必须带资源命名空间，例如 `#moj_import <qisplan2:domain_conflict.glsl>`。无命名空间会被解析到 minecraft，不能依赖“与当前 shader 同目录”推断模组命名空间。

隔绝 Region Identity 与 Stencil 都使用 `isolation_visibility.glsl`：外部观察需有实际可见表面落在区域边界附近，不能只因视线穿过远处未绘制的区域就遮掉天空；相机位于区域内时保持保护。该判断只影响画面，不改服务端隔绝规则，也不强制加载区块。

运行 `gradlew.bat domainShaderTest --offline` 可通过游戏预处理器和 NeoForge 实际导入解析，在隐藏 OpenGL 上下文中验证着色器与隔绝像素。它不能替代游戏内多鬼域、区块卸载与内外视角的实测。


### 公共 GLSL 片段的导入

公共片段放入 `assets/<命名空间>/shaders/include/`，使用绝对导入：

```glsl
#moj_import <qisplan2:domain_conflict.glsl>
```

不要对带命名空间的公共片段使用双引号相对导入。某些兼容层会为相对导入再次补全
当前着色器的命名空间，形成 `qisplan2:qisplan2:...` 的非法资源路径。
绝对导入由资源系统从 `shaders/include/` 查找，无需判断安装了哪些模组。
