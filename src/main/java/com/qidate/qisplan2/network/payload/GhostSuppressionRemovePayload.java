package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GhostSuppressionRemovePayload(
        ResourceLocation sourceGhost,
        ResourceLocation targetGhost,
        int slotIndex
) implements CustomPacketPayload {

    public static final Type<GhostSuppressionRemovePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_suppression_remove"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostSuppressionRemovePayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC,
                    GhostSuppressionRemovePayload::sourceGhost,

                    ResourceLocation.STREAM_CODEC,
                    GhostSuppressionRemovePayload::targetGhost,

                    ByteBufCodecs.VAR_INT,
                    GhostSuppressionRemovePayload::slotIndex,

                    GhostSuppressionRemovePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}