package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GhostMistTogglePayload()
        implements CustomPacketPayload {

    public static final Type<GhostMistTogglePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_mist_toggle"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostMistTogglePayload
            > STREAM_CODEC =
            StreamCodec.unit(
                    new GhostMistTogglePayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}