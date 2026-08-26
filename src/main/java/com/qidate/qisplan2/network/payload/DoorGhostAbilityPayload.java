package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DoorGhostAbilityPayload()
        implements CustomPacketPayload {

    public static final Type<DoorGhostAbilityPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "door_ghost_ability"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            DoorGhostAbilityPayload
            > STREAM_CODEC =
            StreamCodec.unit(
                    new DoorGhostAbilityPayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}