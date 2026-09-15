package com.qidate.qisplan2.ghost.layer;

import com.mojang.serialization.Codec;

public record GhostLayerData(
        int layer
) {

    public static final GhostLayerData DEFAULT =
            new GhostLayerData(0);

    public static final Codec<GhostLayerData> CODEC =
            Codec.INT.xmap(
                    GhostLayerData::new,
                    GhostLayerData::layer
            );
}