package com.qidate.qisplan2.network.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenGhostTombstoneScreenPayload(
        BlockPos pos
) implements CustomPacketPayload {

    public static final Type<
            OpenGhostTombstoneScreenPayload
            > TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            "qisplan2",
                            "open_ghost_tombstone_screen"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            OpenGhostTombstoneScreenPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    OpenGhostTombstoneScreenPayload::pos,
                    OpenGhostTombstoneScreenPayload::new
            );

    @Override
    public Type<? extends OpenGhostTombstoneScreenPayload> type() {
        return TYPE;
    }
}