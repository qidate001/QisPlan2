package com.qidate.qisplan2.ghost.module;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 灵异物品中保存的厉鬼模块数据。
 *
 * 这里只保存数据，不负责执行厉鬼规则。
 */
public final class GhostModuleData {

    private GhostModuleData() {
    }

    /**
     * 单个厉鬼模块的数据。
     */
    public record Entry(
            ResourceLocation id,
            double intensity
    ) {
        public static final Codec<Entry> CODEC =
                RecordCodecBuilder.create(instance ->
                        instance.group(
                                ResourceLocation.CODEC
                                        .fieldOf("id")
                                        .forGetter(Entry::id),

                                Codec.DOUBLE
                                        .optionalFieldOf("intensity", 1.0D)
                                        .forGetter(Entry::intensity)
                        ).apply(instance, Entry::new)
                );
    }

    /**
     * 一件物品所携带的全部厉鬼模块。
     */
    public static final Codec<List<Entry>> CODEC =
            Entry.CODEC.listOf();
}