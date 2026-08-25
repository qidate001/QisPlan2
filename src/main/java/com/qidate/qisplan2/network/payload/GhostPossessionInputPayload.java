package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GhostPossessionInputPayload(
        boolean left,
        boolean right,
        boolean attempt
) implements CustomPacketPayload {

    public static final Type<GhostPossessionInputPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_possession_input"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostPossessionInputPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    GhostPossessionInputPayload::left,

                    ByteBufCodecs.BOOL,
                    GhostPossessionInputPayload::right,

                    ByteBufCodecs.BOOL,
                    GhostPossessionInputPayload::attempt,

                    GhostPossessionInputPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}