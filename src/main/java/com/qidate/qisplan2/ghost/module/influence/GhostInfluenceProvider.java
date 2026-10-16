package com.qidate.qisplan2.ghost.module.influence;

/**
 * 声明一个对象能够向灵异影响系统提供影响。
 *
 * <p>
 * 影响类型由提供者自行声明；
 * 影响强度在获取时动态计算。
 */
public interface GhostInfluenceProvider {

    /**
     * 获取当前提供的影响类型。
     */
    GhostInfluenceType getInfluenceType();

    /**
     * 动态获取当前影响强度。
     *
     * <p>
     * 每次收集影响时调用，不缓存返回值。
     */
    double getInfluenceIntensity();
}
