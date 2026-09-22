package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GhostDoorAbilityPayload()
        implements CustomPacketPayload {

    public static final Type<GhostDoorAbilityPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "door_ghost_ability"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostDoorAbilityPayload
            > STREAM_CODEC =
            StreamCodec.unit(
                    new GhostDoorAbilityPayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}