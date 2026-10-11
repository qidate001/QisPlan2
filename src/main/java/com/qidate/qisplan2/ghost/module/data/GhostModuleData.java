package com.qidate.qisplan2.ghost.module.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 灵异模块的数据定义。
 *
 * Entry 保存某个宿主中模块的当前属性值，
 * 不保存模块定义本身。
 *
 * 初始值由模块定义提供，创建模块实例时再复制进去。
 */
public final class GhostModuleData {

    private GhostModuleData() {
    }

    /**
     * 单个模块实例的数据。
     *
     * id：模块标识。
     * intensity：当前灵异强度。
     * revival：当前复苏值。
     * shutdown：当前死机值。
     */
    public record Entry(
            ResourceLocation id,
            double intensity,
            double revival,
            double shutdown
    ) {

        /**
         * 兼容旧代码：尚未指定复苏值和死机值时，默认均为 0。
         */
        public Entry(ResourceLocation id, double intensity) {
            this(id, intensity, 0.0D, 0.0D);
        }

        public static final Codec<Entry> CODEC =
                RecordCodecBuilder.create(instance ->
                        instance.group(
                                ResourceLocation.CODEC
                                        .fieldOf("id")
                                        .forGetter(Entry::id),

                                Codec.DOUBLE
                                        .optionalFieldOf("intensity", 1.0D)
                                        .forGetter(Entry::intensity),

                                Codec.DOUBLE
                                        .optionalFieldOf("revival", 0.0D)
                                        .forGetter(Entry::revival),

                                Codec.DOUBLE
                                        .optionalFieldOf("shutdown", 0.0D)
                                        .forGetter(Entry::shutdown)
                        ).apply(instance, Entry::new)
                );
    }

    /**
     * 一份数据中包含的全部模块实例。
     */
    public static final Codec<List<Entry>> CODEC =
            Entry.CODEC.listOf();
}