package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.ghost.module.sound.GhostSoundSourceId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record PlayGhostSoundPayload(
        UUID sourceId,
        ResourceLocation dimension,
        ResourceLocation soundEvent,
        double x,
        double y,
        double z
) implements CustomPacketPayload {

    public static final Type<PlayGhostSoundPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            "qisplan2",
                            "play_ghost_sound"
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
            PlayGhostSoundPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    UUID_CODEC,
                    PlayGhostSoundPayload::sourceId,
                    ResourceLocation.STREAM_CODEC,
                    PlayGhostSoundPayload::dimension,
                    ResourceLocation.STREAM_CODEC,
                    PlayGhostSoundPayload::soundEvent,
                    net.minecraft.network.codec.ByteBufCodecs.DOUBLE,
                    PlayGhostSoundPayload::x,
                    net.minecraft.network.codec.ByteBufCodecs.DOUBLE,
                    PlayGhostSoundPayload::y,
                    net.minecraft.network.codec.ByteBufCodecs.DOUBLE,
                    PlayGhostSoundPayload::z,
                    PlayGhostSoundPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}