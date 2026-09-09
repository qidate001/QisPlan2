package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record GhostDomainRemovePayload(
        UUID id
) implements CustomPacketPayload {

    public static final Type<GhostDomainRemovePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_domain_remove"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostDomainRemovePayload
            > STREAM_CODEC = StreamCodec.of(

            (buf, payload) -> {
                buf.writeUUID(payload.id());
            },

            buf -> new GhostDomainRemovePayload(
                    buf.readUUID()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}