package com.qidate.qisplan2.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StopGhostPianoMusicPayload(
        BlockPos pos
) implements CustomPacketPayload {

    public static final Type<StopGhostPianoMusicPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            "qisplan2",
                            "stop_ghost_piano_music"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            StopGhostPianoMusicPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    StopGhostPianoMusicPayload::pos,
                    StopGhostPianoMusicPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}