package com.qidate.qisplan2.network.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetGhostTombstoneInscriptionPayload(
        BlockPos pos,
        String inscription
) implements CustomPacketPayload {

    public static final Type<
            SetGhostTombstoneInscriptionPayload
            > TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            "qisplan2",
                            "set_ghost_tombstone_inscription"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            SetGhostTombstoneInscriptionPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    SetGhostTombstoneInscriptionPayload::pos,

                    ByteBufCodecs.STRING_UTF8,
                    SetGhostTombstoneInscriptionPayload::inscription,

                    SetGhostTombstoneInscriptionPayload::new
            );

    @Override
    public Type<? extends SetGhostTombstoneInscriptionPayload> type() {
        return TYPE;
    }
}