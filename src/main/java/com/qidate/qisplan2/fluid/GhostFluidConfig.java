package com.qidate.qisplan2.fluid;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Function;

/**
 * 灵异液体的统一配置。
 *
 * 每一种灵异液体只需要提供自己的配置，
 * GhostFluidHandler 负责统一处理。
 */
public record GhostFluidConfig(

        /*
         * 灵异液体对应的 FluidType。
         *
         * 注意：
         *
         * 这里保存 DeferredHolder，
         * 不要在静态初始化阶段调用 .get()。
         */
        DeferredHolder<FluidType, FluidType> fluidType,

        /*
         * 是否使用“沉入深度”计算压制。
         *
         * true：
         *     使用液面高度 - 实体脚部高度。
         *
         * false：
         *     使用实体被液体包裹的程度。
         */
        boolean useDepth,

        /*
         * 每 3 单位 immersion 增加多少灵异袭击强度。
         *
         * 对鬼湖水来说：
         *
         * 3 格深度 = 10 强度。
         */
        double attackPerThreeUnits,

        /*
         * 最大灵异袭击强度。
         */
        double maxAttack,

        /*
         * 每单位 immersion 对复苏值造成的百分比削减。
         *
         * 例如：
         *
         * 5.0 + 深度 3
         *
         * = 每秒减少 15%。
         */
        double revivalLossPerUnit,

        /*
         * 该液体对应的 DamageSource。
         */
        Function<LivingEntity, DamageSource> damageSourceFactory

) {
}