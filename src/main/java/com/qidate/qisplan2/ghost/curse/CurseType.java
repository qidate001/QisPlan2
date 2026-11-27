package com.qidate.qisplan2.ghost.curse;

import com.qidate.qisplan2.ghost.module.component.GhostComponentProgram;
import com.qidate.qisplan2.ghost.module.component.GhostSuppressionAura;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * 一个诅咒类型。
 *
 * CurseType 不是一个具体诅咒实例。
 *
 * 它代表：
 *
 * “这种诅咒应该如何创建，以及应该如何从存档恢复。”
 */
public interface CurseType {
    /** 图标属于定义；null 表示无专属图标。 */
    default ResourceLocation icon(){return null;}
    /** 玩家是否能察觉此持续影响。 */
    default boolean detectable(){return true;}

    /**
     * 目标死亡后是否保留此诅咒，默认不保留。
     * 保留的是同一实例及其状态，不是重生时重新施加；寿命和来源失效规则仍正常生效。
     * 此声明对所有生物目标一致，由框架处理死亡清理，诅咒无需识别目标宿主类型。
     */
    default boolean persistsAfterDeath() { return false; }

    /** 诅咒可选择对所缠绕目标维持压制；现有三类诅咒均不声明。 */
    default GhostSuppressionAura suppression(){return GhostSuppressionAura.NONE;}

    /** 持续能力的规则和媒介随类型声明；默认周期能力沿用该实例的状态推进。 */
    default GhostComponentProgram<CurseAbilityContext> program() { return CursePrograms.TICK; }
    /** 运行时的复苏倍率，沉寂、移除或失效后不再参与。 */
    default double revivalMultiplier() { return 1; }


    /**
     * 获取诅咒类型 ID。
     */
    ResourceLocation id();

    /**
     * 创建一个新的诅咒实例。
     *
     * @param target 诅咒目标
     * @param source 诅咒来源
     * @param initialState 创建时的初始状态
     */
    Curse create(
            UUID target,
            CurseSource source,
            CompoundTag initialState
    );

    /**
     * 从存档数据恢复一个诅咒实例。
     */
    Curse load(
            CompoundTag tag
    );
}
