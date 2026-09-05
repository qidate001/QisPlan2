package com.qidate.qisplan2.fluid;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;

public class GhostBloodFluidType extends FluidType {

    public GhostBloodFluidType(Properties properties) {
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
         * 鬼血目前不额外修改实体移动。
         *
         * 鬼血的特殊效果由 GhostFluidHandler
         * 统一处理。
         */
        return super.move(
                state,
                entity,
                movementVector,
                gravity
        );
    }
}