package com.qidate.qisplan2.ghost.module.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

/**
 * 灵异模块的实例数据。
 *
 * 保存模块实例的通用属性与模块专属数据，
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
     * sleeping：当前是否处于沉睡状态。
     * customData：当前模块实例的专属数据。
     */
    public record Entry(
            ResourceLocation id,
            double intensity,
            double revival,
            boolean sleeping,
            CompoundTag customData
    ) {

        public Entry {
            Objects.requireNonNull(id, "id");

            customData = customData == null
                    ? new CompoundTag()
                    : customData.copy();
        }

        /**
         * 兼容只提供模块 ID 和强度的调用。
         */
        public Entry(ResourceLocation id, double intensity) {
            this(
                    id,
                    intensity,
                    0.0D,
                    false,
                    new CompoundTag()
            );
        }

        /**
         * 兼容只提供模块 ID、强度和复苏值的调用。
         */
        public Entry(
                ResourceLocation id,
                double intensity,
                double revival
        ) {
            this(
                    id,
                    intensity,
                    revival,
                    false,
                    new CompoundTag()
            );
        }

        /**
         * 兼容尚未提供沉睡状态的调用。
         */
        public Entry(
                ResourceLocation id,
                double intensity,
                double revival,
                CompoundTag customData
        ) {
            this(
                    id,
                    intensity,
                    revival,
                    false,
                    customData
            );
        }

        /**
         * 返回专属数据的副本，避免外部直接修改内部状态。
         */
        @Override
        public CompoundTag customData() {
            return customData.copy();
        }

        /**
         * 创建一份专属数据已更新的新实例数据。
         * 其他通用属性保持不变。
         */
        public Entry withCustomData(CompoundTag newCustomData) {
            return new Entry(
                    id,
                    intensity,
                    revival,
                    sleeping,
                    newCustomData
            );
        }

        /**
         * 创建一份沉睡状态已更新的新实例数据。
         * 其他通用属性保持不变。
         */
        public Entry withSleeping(boolean newSleeping) {
            return new Entry(
                    id,
                    intensity,
                    revival,
                    newSleeping,
                    customData
            );
        }

        /**
         * 根据模块定义创建新的模块实例数据。
         *
         * 新实例默认不处于沉睡状态。
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
                    false,
                    new CompoundTag()
            );
        }

        /**
         * 单个模块实例的序列化格式。
         *
         * 旧存档缺少 sleeping 字段时，默认不处于沉睡状态。
         * 已废弃的 shutdown 字段不会参与新状态的恢复。
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

                                Codec.BOOL
                                        .optionalFieldOf("sleeping", false)
                                        .forGetter(Entry::sleeping),

                                CompoundTag.CODEC
                                        .optionalFieldOf(
                                                "customData",
                                                new CompoundTag()
                                        )
                                        .forGetter(Entry::customData)
                        ).apply(instance, Entry::new)
                );
    }

    /**
     * 模块实例列表的序列化格式。
     */
    public static final Codec<List<Entry>> CODEC = Entry.CODEC.listOf();
}