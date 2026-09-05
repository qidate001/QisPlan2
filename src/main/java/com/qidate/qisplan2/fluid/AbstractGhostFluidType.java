package com.qidate.qisplan2.fluid;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;

public abstract class AbstractGhostFluidType extends FluidType {

    private final double sinkSpeed;

    protected AbstractGhostFluidType(
            Properties properties,
            double sinkSpeed
    ) {
        super(properties);
        this.sinkSpeed = sinkSpeed;
    }

    @Override
    public boolean move(
            FluidState state,
            LivingEntity entity,
            Vec3 movementVector,
            double gravity
    ) {

        entity.move(
                MoverType.SELF,
                new Vec3(
                        0.0D,
                        -sinkSpeed,
                        0.0D
                )
        );

        return true;
    }
}