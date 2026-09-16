package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GhostEyeSelfLayerChangePayload(
        int delta
) implements CustomPacketPayload {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "ghost_eye_self_layer_change"
            );

    public static final CustomPacketPayload.Type<
            GhostEyeSelfLayerChangePayload
            > TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<
                ByteBuf,
                GhostEyeSelfLayerChangePayload
                > STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    GhostEyeSelfLayerChangePayload::delta,
                    GhostEyeSelfLayerChangePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}