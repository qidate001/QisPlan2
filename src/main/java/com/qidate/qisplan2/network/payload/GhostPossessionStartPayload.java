package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.GhostPossessionSession;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GhostPossessionStartPayload(
        int ghostEntityId,
        int totalTicks
) implements CustomPacketPayload {

    public static final Type<GhostPossessionStartPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_possession_start"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostPossessionStartPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    GhostPossessionStartPayload::ghostEntityId,

                    ByteBufCodecs.VAR_INT,
                    GhostPossessionStartPayload::totalTicks,

                    GhostPossessionStartPayload::new
            );

    public static GhostPossessionStartPayload from(
            GhostPossessionSession session
    ) {
        return new GhostPossessionStartPayload(
                session.ghostEntityId(),
                session.remainingTicks()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}