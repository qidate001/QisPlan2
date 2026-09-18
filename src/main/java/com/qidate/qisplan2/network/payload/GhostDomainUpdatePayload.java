package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record GhostDomainUpdatePayload(
        UUID id,
        double x,
        double y,
        double z,
        double strength,
        int layer,
        double radius,
        boolean immediate
) implements CustomPacketPayload {

    public static final Type<GhostDomainUpdatePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_domain_update"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostDomainUpdatePayload
            > STREAM_CODEC = StreamCodec.of(

            (buf, payload) -> {
                buf.writeUUID(payload.id());

                buf.writeDouble(payload.x());
                buf.writeDouble(payload.y());
                buf.writeDouble(payload.z());

                buf.writeDouble(payload.strength());
                buf.writeInt(payload.layer());
                buf.writeDouble(payload.radius());

                buf.writeBoolean(payload.immediate());
            },

            buf -> new GhostDomainUpdatePayload(
                    buf.readUUID(),

                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),

                    buf.readDouble(),
                    buf.readInt(),
                    buf.readDouble(),

                    buf.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}