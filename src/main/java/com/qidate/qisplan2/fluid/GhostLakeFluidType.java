package com.qidate.qisplan2.fluid;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.fluids.FluidType;

public class GhostLakeFluidType
        extends FluidType {

    /**
     * 鬼湖中的强制下沉速度。
     *
     * 1.0 = 每 tick 下沉 1 格。
     */
    private static final double SINK_SPEED =
            1.0D;

    public GhostLakeFluidType(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    public boolean move(
            FluidState state,
            LivingEntity entity,
            Vec3 movementVector,
            double gravity
    ) {

        /*
         * ========================================================
         * 完全禁止玩家输入产生的水平移动。
         * ========================================================
         */

        Vec3 movement =
                new Vec3(
                        0.0D,
                        -SINK_SPEED,
                        0.0D
                );

        /*
         * ========================================================
         * 真正执行移动。
         *
         * 这一步很关键。
         *
         * move() 不只是“设置速度”，
         * 而是要自己完成本 tick 的移动。
         * ========================================================
         */

        entity.move(
                MoverType.SELF,
                movement
        );

        /*
         * ========================================================
         * 返回 true：
         *
         * 告诉 LivingEntity：
         *
         * “鬼湖自己的移动已经完成了，
         * 不要再执行默认水中移动。”
         * ========================================================
         */

        return true;
    }
}