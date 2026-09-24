package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GhostSuppressionAllocationPayload(
        ResourceLocation sourceGhost,
        ResourceLocation targetGhost,
        int slotIndex,
        double x,
        double y
) implements CustomPacketPayload {

    public static final Type<GhostSuppressionAllocationPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_suppression_allocation"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostSuppressionAllocationPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC,
                    GhostSuppressionAllocationPayload::sourceGhost,

                    ResourceLocation.STREAM_CODEC,
                    GhostSuppressionAllocationPayload::targetGhost,

                    ByteBufCodecs.VAR_INT,
                    GhostSuppressionAllocationPayload::slotIndex,

                    ByteBufCodecs.DOUBLE,
                    GhostSuppressionAllocationPayload::x,

                    ByteBufCodecs.DOUBLE,
                    GhostSuppressionAllocationPayload::y,

                    GhostSuppressionAllocationPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}