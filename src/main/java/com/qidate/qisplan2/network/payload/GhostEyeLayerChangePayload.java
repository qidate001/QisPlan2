package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GhostEyeLayerChangePayload(
        int delta
) implements CustomPacketPayload {

    public static final Type<GhostEyeLayerChangePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_eye_layer_change"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostEyeLayerChangePayload
            > STREAM_CODEC = StreamCodec.of(

            (buf, payload) -> {
                buf.writeInt(payload.delta());
            },

            buf -> new GhostEyeLayerChangePayload(
                    buf.readInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}