package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GhostDivinationResultPayload(
        int result
) implements CustomPacketPayload {

    /*
     * 0 = 生签
     * 1 = 死签
     * 2 = 鬼签
     */

    public static final Type<GhostDivinationResultPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_divination_result"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostDivinationResultPayload
            > STREAM_CODEC = StreamCodec.of(

            (buf, payload) -> {
                buf.writeVarInt(payload.result());
            },

            buf -> new GhostDivinationResultPayload(
                    buf.readVarInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}