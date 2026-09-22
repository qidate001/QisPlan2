package com.qidate.qisplan2.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.qidate.qisplan2.QisPlan2;

public record GhostDomainLowerLayerPayload()
        implements CustomPacketPayload {

    public static final Type<GhostDomainLowerLayerPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_domain_lower_layer"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostDomainLowerLayerPayload
            > STREAM_CODEC =
            StreamCodec.unit(
                    new GhostDomainLowerLayerPayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}