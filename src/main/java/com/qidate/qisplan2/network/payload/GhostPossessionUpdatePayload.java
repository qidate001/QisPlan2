package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.GhostPossessionSession;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GhostPossessionUpdatePayload(
        int remainingTicks,
        double cursorPosition,
        double targetPosition,
        double success
) implements CustomPacketPayload {

    public static final Type<GhostPossessionUpdatePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_possession_update"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostPossessionUpdatePayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    GhostPossessionUpdatePayload::remainingTicks,

                    ByteBufCodecs.DOUBLE,
                    GhostPossessionUpdatePayload::cursorPosition,

                    ByteBufCodecs.DOUBLE,
                    GhostPossessionUpdatePayload::targetPosition,

                    ByteBufCodecs.DOUBLE,
                    GhostPossessionUpdatePayload::success,

                    GhostPossessionUpdatePayload::new
            );

    public static GhostPossessionUpdatePayload from(
            GhostPossessionSession session
    ) {
        return new GhostPossessionUpdatePayload(
                session.remainingTicks(),
                session.cursorPosition(),
                session.targetPosition(),
                session.success()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}