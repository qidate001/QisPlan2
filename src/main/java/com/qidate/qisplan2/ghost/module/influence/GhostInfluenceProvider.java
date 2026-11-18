package com.qidate.qisplan2.ghost.module.influence;

import net.minecraft.resources.ResourceLocation;

/**
 * 声明一个对象能够向灵异影响系统提供影响。
 *
 * <p>
 * 影响类型由提供者自行声明；
 * 影响强度在获取时动态计算。
 */
public interface GhostInfluenceProvider {
    /** 持续影响可以声明自己的对抗响应；非空时不再重复作为静态强度保护收集。 */
    default GhostAttackResponses.Response attackResponse(Runnable remove) { return null; }


    /** 提供影响的模块类型，避免把其他厉鬼留下的保护误认成本次袭击的同源影响。 */
    default ResourceLocation influenceModuleId() { return null; }


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
