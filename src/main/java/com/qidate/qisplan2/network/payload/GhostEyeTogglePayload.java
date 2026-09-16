package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GhostEyeTogglePayload()
        implements CustomPacketPayload {

    public static final Type<GhostEyeTogglePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_eye_toggle"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostEyeTogglePayload
            > STREAM_CODEC =
            StreamCodec.unit(
                    new GhostEyeTogglePayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}