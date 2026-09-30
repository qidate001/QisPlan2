package com.qidate.qisplan2.network.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StartGhostTombstoneInscriptionPayload(
        BlockPos pos,
        String inscription
) implements CustomPacketPayload {

    public static final Type<
            StartGhostTombstoneInscriptionPayload
            > TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            "qisplan2",
                            "start_ghost_tombstone_inscription"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            StartGhostTombstoneInscriptionPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    StartGhostTombstoneInscriptionPayload::pos,

                    ByteBufCodecs.STRING_UTF8,
                    StartGhostTombstoneInscriptionPayload::inscription,

                    StartGhostTombstoneInscriptionPayload::new
            );

    @Override
    public Type<? extends StartGhostTombstoneInscriptionPayload>
    type() {
        return TYPE;
    }
}