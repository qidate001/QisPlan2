package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.domain.CylinderDomainShape;
import com.qidate.qisplan2.ghost.domain.GhostDomain;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record GhostDomainAddPayload(
        UUID id,
        UUID sourceUUID,
        ResourceLocation domainType,
        ResourceLocation dimension,
        double x,
        double y,
        double z,
        double radius
) implements CustomPacketPayload {

    public static final Type<GhostDomainAddPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_domain_add"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostDomainAddPayload
            > STREAM_CODEC = StreamCodec.of(

            (buf, payload) -> {

                buf.writeUUID(payload.id());
                buf.writeUUID(payload.sourceUUID());

                buf.writeResourceLocation(payload.domainType());
                buf.writeResourceLocation(payload.dimension());

                buf.writeDouble(payload.x());
                buf.writeDouble(payload.y());
                buf.writeDouble(payload.z());

                buf.writeDouble(payload.radius());
            },

            buf -> new GhostDomainAddPayload(

                    buf.readUUID(),
                    buf.readUUID(),

                    buf.readResourceLocation(),
                    buf.readResourceLocation(),

                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),

                    buf.readDouble()
            )
    );

    public static GhostDomainAddPayload from(GhostDomain domain) {

        double radius = 0.0D;

        if (domain.getShape() instanceof CylinderDomainShape cylinder) {
            radius = cylinder.getRadius();
        }

        return new GhostDomainAddPayload(
                domain.getId(),
                domain.getSourceUUID(),
                domain.getType(),
                domain.getDimension().location(),
                domain.getX(),
                domain.getY(),
                domain.getZ(),
                radius
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}