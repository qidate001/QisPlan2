package com.qidate.qisplan2.fluid;

public class GhostBloodFluidType extends AbstractGhostFluidType {

    private static final double SINK_SPEED = 0.25D;

    public GhostBloodFluidType(Properties properties) {
        super(properties, SINK_SPEED);
    }
}