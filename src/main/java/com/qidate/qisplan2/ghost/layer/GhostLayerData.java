package com.qidate.qisplan2.ghost.layer;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record GhostLayerData(int layer) {

    public static final GhostLayerData DEFAULT =
            new GhostLayerData(0);

    public static final Codec<GhostLayerData> CODEC =
            Codec.INT.xmap(
                    GhostLayerData::new,
                    GhostLayerData::layer
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, GhostLayerData> STREAM_CODEC =
            StreamCodec.of(
                    (buf, data) -> buf.writeInt(data.layer()),
                    buf -> new GhostLayerData(buf.readInt())
            );
}