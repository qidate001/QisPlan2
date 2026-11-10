package com.qidate.qisplan2.ghost.module.environment;

/**
 * 模块可见的环境能力。不得返回宿主对象、宿主种类或载体存储。
 * 宿主适配器负责解释暴露、环境压力、表现以及消耗如何落到世界中。
 */
public interface GhostEnvironment {
    /**
     * 判断当前模块是否满足暴露在世界中的条件。
     * 具体的手持、存活或方块有效性由载体适配器解释。
     */
    boolean exposed();

    /**
     * 获取当前环境造成的基础燃烧压力，单位为每 tick 的消耗量。
     * 此值不包含声音屏蔽额外消耗。
     */
    double burnPressure();

    /**
     * 读取被屏蔽的灵异声音强度，供模块计算额外燃烧代价。
     * 没有可计入的声音压力时返回零。
     */
    double blockedSoundIntensity();

    /**
     * 扣除本次能力消耗的燃料。
     * 适配器负责把连续消耗量转换为耐久或持久燃料，模块不处理物品存储细节。
     */
    void consume(double amount);

    /**
     * 发布本次能力产生的环境效果。
     * 世界系统读取效果时仍需检查模块状态和暴露条件。
     */
    void emit(GhostEnvironmentalEffect effect);
}
