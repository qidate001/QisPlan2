package com.qidate.qisplan2.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StartGhostPianoMusicPayload(
        BlockPos pos
) implements CustomPacketPayload {

    public static final Type<StartGhostPianoMusicPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            "qisplan2",
                            "start_ghost_piano_music"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            StartGhostPianoMusicPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    StartGhostPianoMusicPayload::pos,
                    StartGhostPianoMusicPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}