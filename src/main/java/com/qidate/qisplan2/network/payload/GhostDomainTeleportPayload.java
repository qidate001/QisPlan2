package com.qidate.qisplan2.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.qidate.qisplan2.QisPlan2;

public record GhostDomainTeleportPayload()
        implements CustomPacketPayload {

    public static final Type<GhostDomainTeleportPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_domain_teleport"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostDomainTeleportPayload
            > STREAM_CODEC =
            StreamCodec.unit(
                    new GhostDomainTeleportPayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}