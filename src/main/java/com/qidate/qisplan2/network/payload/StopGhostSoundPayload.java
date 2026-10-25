package com.qidate.qisplan2.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record StopGhostSoundPayload(
        UUID sourceId,
        ResourceLocation dimension
) implements CustomPacketPayload {

    public static final Type<StopGhostSoundPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            "qisplan2",
                            "stop_ghost_sound"
                    )
            );

    private static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_CODEC =
            new StreamCodec<>() {
                @Override
                public UUID decode(RegistryFriendlyByteBuf buffer) {
                    return new UUID(buffer.readLong(), buffer.readLong());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, UUID value) {
                    buffer.writeLong(value.getMostSignificantBits());
                    buffer.writeLong(value.getLeastSignificantBits());
                }
            };

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            StopGhostSoundPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    UUID_CODEC,
                    StopGhostSoundPayload::sourceId,
                    ResourceLocation.STREAM_CODEC,
                    StopGhostSoundPayload::dimension,
                    StopGhostSoundPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}