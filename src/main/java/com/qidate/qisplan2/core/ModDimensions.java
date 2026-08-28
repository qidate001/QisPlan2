package com.qidate.qisplan2.core;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class ModDimensions {

    private ModDimensions() {}

    public static void init() {
        /*
         * 故意留空。
         *
         * 调用这个方法本身，就会强制 JVM
         * 在正确的时机完成 ModDimensions 的静态初始化。
         */
    }


    // 划分维度
    public static final ResourceKey<Level> PARTITION_DIMENSION =
            ResourceKey.create(
                    Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "partition"
                    )
            );
}
