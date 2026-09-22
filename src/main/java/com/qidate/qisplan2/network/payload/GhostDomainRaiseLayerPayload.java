package com.qidate.qisplan2.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.qidate.qisplan2.QisPlan2;

public record GhostDomainRaiseLayerPayload()
        implements CustomPacketPayload {

    public static final Type<GhostDomainRaiseLayerPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_domain_raise_layer"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostDomainRaiseLayerPayload
            > STREAM_CODEC =
            StreamCodec.unit(
                    new GhostDomainRaiseLayerPayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}