# 厉鬼模块开发指南：从零构建一只完整的厉鬼

一个模块是一只完整的厉鬼。模块声明规则、媒介、能力和特殊效果；宿主适配层负责物理执行。模块不能读取或判断自己是玩家、实体、物品还是方块。

本指南按主题拆分，以下文档描述当前实现与开发约定。建议从模块基础开始，再阅读规则、能力与完整示例，最后按需选择鬼域、行为或对抗扩展。

| 文档 | 内容 |
|---|---|
| [模块基础](ghost-module/basics.md) | ID、初始强度、复苏、承载形式、注册、语言、图标与实例状态 |
| [规则与语义事件订阅](ghost-module/rules.md) | 语义事实、订阅声明、按需派发、触发条件与沉睡观察 |
| [能力与媒介](ghost-module/abilities-media.md) | 公开/私有能力、输入、持续能力、硬性前提与统一执行器 |
| [灵异裁决与对抗](ghost-module/adjudication-combat.md) | 共享影响批次、抵挡、压制、沉睡、附着与复苏代价 |
| [鬼域声明与能力](ghost-module/domain.md) | 几何、等级/分层、成长、空间归属、鬼域对抗与同源豁免 |
| [鬼域面板](ghost-module/domain-panel.md) | 自定义控件、公共按钮、进出权限和自动化 |
| [鬼域渲染与效果](ghost-module/domain-rendering.md) | 效果适配器、标准参数前缀、着色器资源和空间裁剪 |
| [自主行为（Goal）](ghost-module/autonomous-behavior.md) | 行为工厂、动作接口、资源冲突与共享调度 |
| [宿主适配](ghost-module/hosts-state.md) | 实体/物品/方块载体、外观、生命周期和共享身体效果 |
| [世界接口与声音](ghost-module/world-services.md) | 通用世界端口、声音、蔓延与服务边界 |
| [完整模块示例](ghost-module/example.md) | 从定义到规则、媒介和能力的完整触响鬼 |
| [调试与测试](ghost-module/testing.md) | 构建命令、回归场景、常见问题和源码阅读索引 |

可编译示例：[ContactEchoModule.java](examples/ContactEchoModule.java)。承载专题：[宿主灵异承载](ghost-bearing.md)。