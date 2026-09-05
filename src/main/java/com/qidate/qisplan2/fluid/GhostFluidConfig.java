package com.qidate.qisplan2.fluid;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Function;

public record GhostFluidConfig(
        DeferredHolder<FluidType, FluidType> fluidType,

        /**
         * true：
         *   immersion 表示液体深度（格）
         *
         * false：
         *   immersion 表示实体被液体包裹的比例（0~1）
         */
        boolean useDepth,

        /**
         * 每 1 单位 immersion 对应的灵异攻击强度。
         *
         * 例如：
         *
         * 鬼湖水：
         *   depth = 3
         *   attackPerImmersion = 10 / 3
         *   attack = 10
         *
         * 鬼血：
         *   coverage = 1
         *   attackPerImmersion = 30
         *   attack = 30
         */
        double attackPerImmersion,

        /**
         * 最大灵异攻击强度。
         */
        double maxAttack,

        /**
         * 每 1 单位 immersion 对所有体内鬼复苏度造成的百分比削减。
         *
         * 例如：
         *
         * immersion = 1
         * revivalLossPerUnit = 5
         *
         * 每次处理削减 5%。
         */
        double revivalLossPerUnit,

        /**
         * 生成该液体对应的 DamageSource。
         */
        Function<LivingEntity, DamageSource> damageSourceFactory
) {
}