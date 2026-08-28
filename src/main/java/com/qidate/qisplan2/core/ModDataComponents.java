package com.qidate.qisplan2.core;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;

import static com.qidate.qisplan2.core.ModRegistries.DATA_COMPONENTS;

public class ModDataComponents {

    private ModDataComponents() {}

    public static void init() {
        /*
         * 故意留空。
         *
         * 调用这个方法本身，就会强制 JVM
         * 在正确的时机完成 ModDataComponents 的静态初始化。
         */
    }


    // 鬼皮箱ID
    public static final DeferredHolder<
            DataComponentType<?>,
            DataComponentType<Long>
            > GHOST_LEATHER_BOX_REGION_ID =
            DATA_COMPONENTS.register(
                    "ghost_leather_box_region_id",
                    () ->
                            DataComponentType
                                    .<Long>builder()
                                    .persistent(
                                            Codec.LONG
                                    )
                                    .networkSynchronized(
                                            ByteBufCodecs.VAR_LONG
                                    )
                                    .build()
            );

    // 鬼雨伞开关状态
    public static final DeferredHolder<
            DataComponentType<?>,
            DataComponentType<Long>
            > GHOST_UMBRELLA_OPENED_AT =
            DATA_COMPONENTS.register(
                    "ghost_umbrella_opened_at",
                    () -> DataComponentType
                            .<Long>builder()
                            .persistent(Codec.LONG)
                            .networkSynchronized(
                                    ByteBufCodecs.VAR_LONG
                            )
                            .build()
            );
}
