package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GhostDivinationUsePayload(
        int result
) implements CustomPacketPayload {

    public static final Type<GhostDivinationUsePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_divination_use"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostDivinationUsePayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    GhostDivinationUsePayload::result,

                    GhostDivinationUsePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}