# 齐计划 II

> 一个以“规则即恐怖”为核心的我的世界模组。  
> 设定大量借鉴小说《神秘复苏》

![Java](https://img.shields.io/badge/Java-21-blue?style=for-the-badge)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green?style=for-the-badge)
![NeoForge](https://img.shields.io/badge/NeoForge-%E2%89%A521.1.248-orange?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)

<details>
<summary>关于 Contributors 里那个 Claude 的来历</summary>

（我并没有使用 Claude Code 写代码，Contributors 有是我朋友莫名其妙用 Claude 写了一个 README，然后就有了）
（现在去不掉了，也就算了吧，不管了）

</details>

---

## 📖 目录

- [简介](#简介)
- [提示](#提示)
- [厉鬼模块](#厉鬼模块)
- [当前实现内容](#当前实现内容)
- [路线规划](#路线规划)
- [开发中内容](#开发中内容)
- [技术信息](#技术信息)
- [鸣谢](#鸣谢)

---

## 简介

**齐计划 II** 是“齐计划”的续作，但与前作无关。

模组包含两条路线：

| 🩸 灵异路线（主要开发） | 🔬 唯物路线（纯粹画饼） |
| :--- | :--- |
| 驾驭鬼，平衡鬼与鬼 | 灵异空间、研究科研 |
| 用规则和代价换命 | 用工程与空间技术在鬼的世界里建立秩序 |

本模组不是恐怖吓人模组，也不是单纯的打怪模组。  
它是一个以 **“规则”** 为核心的生存模组。  
本模组不是纯粹将《神秘复苏》搬进《我的世界》，我会加入大量原创内容！

> 如果您只想体验小说中的内容，或许 [进击的蟹某人](https://space.bilibili.com/488712064) 的 [神秘复苏 mod](https://space.bilibili.com/488712064/lists/6230842) 是更好的选择。

---

## 提示

本模组的鬼并非传统怪物。  
它们 **没有血条**，也不一定会主动攻击。  
如果你把它当普通刷怪模组玩，会死得很快，而且不知道自己怎么死的。

请保持冷静，注意观察，记住规则。

> **鬼无法被杀死**  
> **能对付鬼的只有鬼**  
> **洞察它们的规律，活下来**

---

## 厉鬼模块

> 📚 开发说明：[从零构建厉鬼模块](docs/ghost-module-development.md) · [宿主灵异承载](docs/ghost-bearing.md)

本模组所有厉鬼都是厉鬼模块构成，厉鬼模块主要有三个部分构成：

| 模块 | 说明 |
| :---: | :--- |
| **规则** | 描述厉鬼的行为逻辑 |
| **能力** | 描述厉鬼如何影响事件 |
| **媒介** | 描述厉鬼能力执行的硬性前提 |

---

## 当前实现内容

###  厉鬼模块开发状态
> ✅ 已完成   🚧 待重构   🔧 待完善

|  厉鬼模块  | 状态 |  厉鬼模块  | 状态 |   厉鬼模块   | 状态 |
|:----------:|:----:|:----------:|:----:|:------------:|:----:|
|    鬼眼    |  🔧  |    鬼雾    |  ✅  |     鬼签     |  ✅  |
|    鬼门    |  ✅  |   鬼门牌   |  ✅  |    夜游鬼    |  ✅  |
|   开门鬼   |  ✅  |   关门鬼   |  ✅  |    敲门鬼    |  ✅  |
|   喊人鬼   |  ✅  |   鬼灶台   |  ✅  |    鬼墓碑    |  ✅  |
|   鬼钢琴   |  ✅  |   鬼石砖   |  ✅  |    鬼地毯    |  ✅  |
|   鬼草丛   |  ✅  |   鬼寿衣   |  ✅  |     鬼书     |  🔧  |
| 不可视之鬼 |  ✅  | 不可触之鬼 |  ✅  |     鬼伞     |  ✅  |
|   鬼皮箱   |  🚧  |    鬼画    |  🚧  |    鬼湖水    |  🚧  |
|    鬼血    |  🚧  |   棺材钉   |  ✅  | 必死诅咒之剑 |  🔧  |

### 🧩 已实装灵异材料

| 鬼金币 | 鬼石指 | 香火灰 |
| :---: | :---: | :---: |

### 🖼️ 已实装鬼画

| 一幅诡异的风景画 | 奥罗金帝国 |
| :---: | :---: |

### 🌫️ 已实装鬼域

| 鬼眼鬼域 | 鬼伞鬼域 | 鬼雾鬼域 |
| :---: | :---: | :---: |

---

## 路线规划

| 🩸 灵异路线 | 🔬 唯物路线 |
| :--- | :--- |
| 驾驭鬼 | 口袋空间发展 |
| 利用灵异道具 | 招募幸存者、驭鬼者与异类 |
| 承担诅咒与代价 | 建立自己的势力与秩序 |
| 追求高风险高回报 | 研究灵异力量的使用 |

---

## 开发中内容

- [ ] 鬼门系鬼
- [ ] 灵异坟场
- [ ] 庄园多层结构
- [ ] 鬼书（皮皮纸）
- [ ] 鬼新娘体系
- [ ] 鬼女工作品
- [ ] 鬼画体系

> 以上内容处于设计或开发阶段，后续版本逐步加入。

---

## 技术信息

| 项目 | 内容 |
| :--- | :--- |
| **开发语言** | Java |
| **模组加载器** | NeoForge（需 ≥ 21.1.248） |
| **运行版本** | Minecraft 1.21.1 |
| **开源协议** | MIT License |

---

## 鸣谢

> 没有你们的帮助与陪伴，就没有今天的 **Qis Plan 2**。

| 🙌 贡献者 | 🏷️ 身份 | 📝 贡献内容 |
| :---: | :---: | :--- |
| [**一只屑System32**](https://github.com/uzjhf-836/) | 🎨 🛠️ 🧪 | 绘制「必死诅咒之剑」纹理；制作剑的 HUD 与材质；参与模组测试与反馈 |
| [**文森 / 亿森**](https://www.qidate001.com/) | 🎨 💡 | 绘制「喊人鬼」「敲门鬼」模型与纹理；参与开发探讨与建议 |
| [**何喻逸林**](https://space.bilibili.com/275630606/) | 🏯 | 鬼寺庙 [建筑原型](https://www.bilibili.com/video/BV1uH4y157X4/) |
| [**Pixel轩**](https://space.bilibili.com/275630606/) | 🏯 | 鬼灶台木屋 [建筑原型](https://www.bilibili.com/video/BV1F8Nu6GECy/) |
| [**天涯共磁石**](https://space.bilibili.com/525276096) | 🏯 | 鬼庄园 [建筑原型](https://www.bilibili.com/video/BV1F8Nu6GECy/) |
| [**mysticat**](https://www.youtube.com/@mysticat) | 🏯 | 鬼坟场 [墙壁建筑原型](https://www.youtube.com/watch?v=3ILX78Bc00E) |
| [**卯未**](https://www.douyin.com/user/MS4wLjABAAAAMEa0gKD6lwmdbVtaNam39gfozSKVdglng_46zjLmOcg) | 💡 | 提出「鬼雨伞」副作用建议 |
| [**执掌83个圈子之人**](https://space.bilibili.com/3690999347939733) | 💡 | 模组实现方案的众多建议与陪伴 |
| [**小迪之**](https://space.bilibili.com/3546981603478187) | 💡 | 参与开发探讨，提出众多建议与陪伴 |
| [**牢大 / 红尘仙**](https://www.qidate001.com/) | 💡 | 参与开发探讨，提出众多建议与陪伴 |
| [**我叫小鸟awa**](https://space.bilibili.com/3546846593026382) | 💡 | 参与开发探讨与陪伴 |
| [**《神秘复苏》**](https://www.qidian.com/book/1012584111/) · 佛前献花 | 📖 | 为模组的灵异世界观提供了深厚的土壤 |
| ❤️ **每一位陪伴的朋友** | ❤️ | 陪我慢慢完成这个模组，是你们让 Qis Plan 2 走到今天 |

<sub>图例：🎨 美术 · 🏯 建筑 · 🛠️ 贡献 · 🧪 测试 · 💡 灵感 · 📖 世界观 · ❤️ 陪伴</sub>

---

## 开发人员

<div align="center">
  <a href="https://github.com/qidate001">
    <img src="https://github.com/qidate001.png?size=200" width="110" alt="齐" />
  </a>
  <h3>齐</h3>
  <a href="https://github.com/qidate001">
    <img src="https://img.shields.io/badge/GitHub-@qidate001-181717?style=flat-square&logo=github&logoColor=white" alt="GitHub" />
  </a>
  <br />
  <sub>✨ 想把 <b>Qis Plan 2</b> 变得更好？欢迎提交 Issue / PR，或进入 <a href="https://qm.qq.com/q/4OGrCC2gpO">粉丝群</a> 探讨哝。</sub>
</div>

---

<div align="center">

**鬼无法被杀死 · 能对付鬼的只有鬼**

</div>