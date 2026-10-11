package com.qidate.qisplan2.ghost.module.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

/**
 * 灵异模块的实例数据。
 *
 * 此类保存实体或物品当前持有的模块属性，
 * 不保存模块定义本身。
 */
public final class GhostModuleData {

    private GhostModuleData() {
    }

    /**
     * 单个模块实例的当前数据。
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
         * 兼容旧代码。
         *
         * 未指定复苏值和死机值时，二者默认均为 0。
         */
        public Entry(ResourceLocation id, double intensity) {
            this(id, intensity, 0.0D, 0.0D);
        }

        /**
         * 根据模块定义创建新的模块实例数据。
         *
         * 将定义提供的初始属性复制到新的 Entry 中。
         * 后续修改 Entry 不会改变模块定义的初始属性。
         *
         * @param id 模块标识
         * @param initialData 模块定义提供的初始属性
         * @return 新的模块实例数据
         */
        public static Entry fromDefinition(
                ResourceLocation id,
                GhostModuleInitialData initialData
        ) {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(initialData, "initialData");

            return new Entry(
                    id,
                    initialData.intensity(),
                    initialData.revival(),
                    initialData.shutdown()
            );
        }

        /**
         * 单个模块实例的序列化格式。
         *
         * 当旧存档缺少新属性时，使用兼容默认值。
         */
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
     * 模块实例列表的序列化格式。
     */
    public static final Codec<List<Entry>> CODEC = Entry.CODEC.listOf();
}