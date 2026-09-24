package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GhostSuppressionMovePayload(
        ResourceLocation sourceGhost,
        ResourceLocation oldTargetGhost,
        ResourceLocation newTargetGhost,
        int slotIndex,
        double x,
        double y
) implements CustomPacketPayload {

    public static final Type<GhostSuppressionMovePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_suppression_move"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostSuppressionMovePayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC,
                    GhostSuppressionMovePayload::sourceGhost,

                    ResourceLocation.STREAM_CODEC,
                    GhostSuppressionMovePayload::oldTargetGhost,

                    ResourceLocation.STREAM_CODEC,
                    GhostSuppressionMovePayload::newTargetGhost,

                    ByteBufCodecs.VAR_INT,
                    GhostSuppressionMovePayload::slotIndex,

                    ByteBufCodecs.DOUBLE,
                    GhostSuppressionMovePayload::x,

                    ByteBufCodecs.DOUBLE,
                    GhostSuppressionMovePayload::y,

                    GhostSuppressionMovePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}