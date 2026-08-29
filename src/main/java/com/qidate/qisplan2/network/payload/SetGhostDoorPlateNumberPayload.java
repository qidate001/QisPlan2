package com.qidate.qisplan2.network.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetGhostDoorPlateNumberPayload(
        BlockPos pos,
        int number
) implements CustomPacketPayload {

    public static final Type<SetGhostDoorPlateNumberPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            "qisplan2",
                            "set_ghost_door_plate_number"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            SetGhostDoorPlateNumberPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    SetGhostDoorPlateNumberPayload::pos,

                    net.minecraft.network.codec.ByteBufCodecs.INT,
                    SetGhostDoorPlateNumberPayload::number,

                    SetGhostDoorPlateNumberPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}