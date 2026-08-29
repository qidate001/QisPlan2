package com.qidate.qisplan2.network.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenGhostDoorPlateScreenPayload(
        BlockPos pos
) implements CustomPacketPayload {

    public static final Type<OpenGhostDoorPlateScreenPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            "qisplan2",
                            "open_ghost_door_plate_screen"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            OpenGhostDoorPlateScreenPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    OpenGhostDoorPlateScreenPayload::pos,
                    OpenGhostDoorPlateScreenPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}