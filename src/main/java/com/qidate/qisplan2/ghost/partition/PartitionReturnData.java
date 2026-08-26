package com.qidate.qisplan2.ghost.partition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public record PartitionReturnData(
        boolean valid,
        ResourceKey<Level> dimension,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {

    public static final PartitionReturnData EMPTY =
            new PartitionReturnData(
                    false,
                    Level.OVERWORLD,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0F,
                    0.0F
            );

    public static final Codec<PartitionReturnData> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(

                            Codec.BOOL
                                    .fieldOf("valid")
                                    .forGetter(
                                            PartitionReturnData::valid
                                    ),

                            ResourceKey.codec(
                                            Registries.DIMENSION
                                    )
                                    .fieldOf("dimension")
                                    .forGetter(
                                            PartitionReturnData::dimension
                                    ),

                            Codec.DOUBLE
                                    .fieldOf("x")
                                    .forGetter(
                                            PartitionReturnData::x
                                    ),

                            Codec.DOUBLE
                                    .fieldOf("y")
                                    .forGetter(
                                            PartitionReturnData::y
                                    ),

                            Codec.DOUBLE
                                    .fieldOf("z")
                                    .forGetter(
                                            PartitionReturnData::z
                                    ),

                            Codec.FLOAT
                                    .fieldOf("yaw")
                                    .forGetter(
                                            PartitionReturnData::yaw
                                    ),

                            Codec.FLOAT
                                    .fieldOf("pitch")
                                    .forGetter(
                                            PartitionReturnData::pitch
                                    )

                    ).apply(
                            instance,
                            PartitionReturnData::new
                    )
            );
}