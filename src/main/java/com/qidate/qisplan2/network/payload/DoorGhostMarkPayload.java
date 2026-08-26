package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DoorGhostMarkPayload(
        int entityId,
        boolean marked
) implements CustomPacketPayload {

    public static final Type<DoorGhostMarkPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "door_ghost_mark"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            DoorGhostMarkPayload
            > STREAM_CODEC =
            StreamCodec.composite(

                    ByteBufCodecs.VAR_INT,
                    DoorGhostMarkPayload::entityId,

                    ByteBufCodecs.BOOL,
                    DoorGhostMarkPayload::marked,

                    DoorGhostMarkPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}