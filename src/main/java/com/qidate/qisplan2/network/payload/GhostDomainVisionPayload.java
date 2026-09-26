package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

public record GhostDomainVisionPayload(
        UUID domainId,
        List<VisionEntry> entries
) implements CustomPacketPayload {

    public static final Type<GhostDomainVisionPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_domain_vision"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostDomainVisionPayload
            > STREAM_CODEC = StreamCodec.of(

            (buf, payload) -> {

                buf.writeUUID(
                        payload.domainId()
                );

                buf.writeVarInt(
                        payload.entries().size()
                );

                for (VisionEntry entry :
                        payload.entries()) {

                    buf.writeUUID(
                            entry.entityUUID()
                    );

                    buf.writeInt(
                            entry.color()
                    );
                }
            },

            buf -> {

                UUID domainId =
                        buf.readUUID();

                int size =
                        buf.readVarInt();

                List<VisionEntry> entries =
                        new java.util.ArrayList<>(
                                size
                        );

                for (int i = 0; i < size; i++) {

                    entries.add(
                            new VisionEntry(
                                    buf.readUUID(),
                                    buf.readInt()
                            )
                    );
                }

                return new GhostDomainVisionPayload(
                        domainId,
                        entries
                );
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record VisionEntry(
            UUID entityUUID,
            int color
    ) {
    }
}