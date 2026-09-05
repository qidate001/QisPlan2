package com.qidate.qisplan2.core;

import com.qidate.qisplan2.fluid.GhostLakeFluidType;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class ModFluids {

    private ModFluids() {
    }

    public static void init() {
        /*
         * 故意留空。
         *
         * 调用这个方法本身，
         * 可以确保 JVM 在正确时机完成
         * ModFluids 的静态初始化。
         */
    }

    /*
     * ============================================================
     * FluidType
     * ============================================================
     */

    public static final DeferredHolder<
            FluidType,
            FluidType
            > GHOST_LAKE_WATER_TYPE =
            ModRegistries.FLUID_TYPES.register(
                    "ghost_lake_water",
                    () ->
                            new GhostLakeFluidType(
                                    FluidType.Properties.create()
                                            .descriptionId(
                                                    "fluid_type.qisplan2.ghost_lake_water"
                                            )
                                            .density(1000)
                                            .viscosity(1000)
                                            .temperature(300)
                                            .canPushEntity(false)
                                            .canSwim(false)
                                            .canDrown(false)
                            )
            );

    /*
     * ============================================================
     * 源流体
     * ============================================================
     */

    public static final DeferredHolder<
            Fluid,
            BaseFlowingFluid
            > GHOST_LAKE_WATER =
            ModRegistries.FLUIDS.register(
                    "ghost_lake_water",
                    () ->
                            new BaseFlowingFluid.Source(
                                    createFluidProperties()
                            )
            );

    /*
     * ============================================================
     * 流动流体
     * ============================================================
     */

    public static final DeferredHolder<
            Fluid,
            BaseFlowingFluid
            > FLOWING_GHOST_LAKE_WATER =
            ModRegistries.FLUIDS.register(
                    "flowing_ghost_lake_water",
                    () ->
                            new BaseFlowingFluid.Flowing(
                                    createFluidProperties()
                            )
            );

    /*
     * ============================================================
     * 水桶
     * ============================================================
     */

    public static final DeferredItem<BucketItem>
            GHOST_LAKE_WATER_BUCKET =
            ModRegistries.ITEMS.registerItem(
                    "ghost_lake_water_bucket",
                    properties ->
                            new BucketItem(
                                    GHOST_LAKE_WATER.get(),
                                    properties.stacksTo(1)
                            )
            );

    /*
     * ============================================================
     * 流体方块
     * ============================================================
     */

    public static final DeferredBlock<LiquidBlock>
            GHOST_LAKE_WATER_BLOCK =
            ModRegistries.BLOCKS.registerBlock(
                    "ghost_lake_water",
                    properties ->
                            new LiquidBlock(
                                    GHOST_LAKE_WATER.get(),
                                    properties
                            )
            );

    /*
     * ============================================================
     * BaseFlowingFluid.Properties
     * ============================================================
     */

    private static BaseFlowingFluid.Properties
    createFluidProperties() {

        return new BaseFlowingFluid.Properties(
                GHOST_LAKE_WATER_TYPE,
                GHOST_LAKE_WATER,
                FLOWING_GHOST_LAKE_WATER
        )
                .bucket(
                        () ->
                                GHOST_LAKE_WATER_BUCKET.get()
                )
                .block(
                        () ->
                                GHOST_LAKE_WATER_BLOCK.get()
                );
    }
}