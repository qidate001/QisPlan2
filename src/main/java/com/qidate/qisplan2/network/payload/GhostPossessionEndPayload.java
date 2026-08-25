package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GhostPossessionEndPayload(
        boolean success,
        double finalSuccess
) implements CustomPacketPayload {

    public static final Type<GhostPossessionEndPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_possession_end"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostPossessionEndPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    GhostPossessionEndPayload::success,

                    ByteBufCodecs.DOUBLE,
                    GhostPossessionEndPayload::finalSuccess,

                    GhostPossessionEndPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}