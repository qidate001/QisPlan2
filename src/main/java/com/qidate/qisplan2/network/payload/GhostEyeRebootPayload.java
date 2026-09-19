package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GhostEyeRebootPayload()
        implements CustomPacketPayload {

    public static final Type<GhostEyeRebootPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_eye_reboot"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostEyeRebootPayload
            > STREAM_CODEC =
            StreamCodec.unit(
                    new GhostEyeRebootPayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}