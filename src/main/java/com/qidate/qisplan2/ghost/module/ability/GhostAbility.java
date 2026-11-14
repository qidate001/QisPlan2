package com.qidate.qisplan2.ghost.module.ability;

import com.qidate.qisplan2.ghost.module.GhostMedium;
import java.util.List;

import net.minecraft.resources.ResourceLocation;

/**
 * 灵异能力定义。
 *
 * 能力属于 GhostModule。
 *
 * 能力描述厉鬼能够产生的具体灵异效果，
 * 不依赖某个特定的 GhostEvent 才能执行。
 */
public interface GhostAbility {
    /** 名称由能力 ID 推导语言键，显示层不再把整个厉鬼名称当成唯一技能名称。 */
    default String translationKey() {
        return "ability." + getId().getNamespace() + "." + getId().getPath().replace('/', '.');
    }

    /** 能力可以声明独立图标；未指定时使用通用图标，保证新能力可直接进入界面。 */
    default ResourceLocation iconTexture() {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/nether_star.png");
    }


    /** 能力自己的硬性媒介，统一执行器在每次调用和延迟执行前重新检查。 */
    default List<GhostMedium> media(GhostAbilityContext context) { return List.of(); }

    /** 默认同时继承模块媒介；独立主动操作可完整声明自身媒介。 */
    default boolean inheritsModuleMedia() { return true; }


    /**
     * 获取能力在所属模块中的唯一标识。
     */
    ResourceLocation getId();

    /**
     * 检查本次能力调用是否满足执行条件。
     *
     * 这里只进行判断，不应产生游戏效果。
     */
    boolean canExecute(GhostAbilityContext context);

    /**
     * 执行能力效果。
     *
     * 调用方应先检查能力是否满足执行条件。
     */
    void execute(GhostAbilityContext context);

    /**
     * 执行能力并返回效果是否成功。
     *
     * 旧能力默认认为执行完成即成功。
     * 需要精确判断效果成功与否的能力可以重写此方法。
     */
    default boolean executeWithResult(GhostAbilityContext context) {
        execute(context);
        return true;
    }

    /**
     * 获取本次能力调用的复苏刺激声明。
     *
     * 默认不产生复苏刺激。
     * 返回的声明只描述刺激数值与结算时机，
     * 不会直接修改模块实例的复苏值。
     */
    default GhostAbilityStimulation getStimulation(
            GhostAbilityContext context
    ) {
        return GhostAbilityStimulation.none();
    }
}