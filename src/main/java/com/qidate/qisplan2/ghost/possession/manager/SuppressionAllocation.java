package com.qidate.qisplan2.ghost.possession.manager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record SuppressionAllocation(
        int slotIndex,
        double x,
        double y
) {

    public static final Codec<SuppressionAllocation> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            Codec.INT
                                                    .fieldOf("slot")
                                                    .forGetter(
                                                            SuppressionAllocation::slotIndex
                                                    ),

                                            Codec.DOUBLE
                                                    .fieldOf("x")
                                                    .forGetter(
                                                            SuppressionAllocation::x
                                                    ),

                                            Codec.DOUBLE
                                                    .fieldOf("y")
                                                    .forGetter(
                                                            SuppressionAllocation::y
                                                    )
                                    )
                                    .apply(
                                            instance,
                                            SuppressionAllocation::new
                                    )
            );

    public static final StreamCodec<
            net.minecraft.network.FriendlyByteBuf,
            SuppressionAllocation
            > STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    SuppressionAllocation::slotIndex,

                    ByteBufCodecs.DOUBLE,
                    SuppressionAllocation::x,

                    ByteBufCodecs.DOUBLE,
                    SuppressionAllocation::y,

                    SuppressionAllocation::new
            );
}