package com.qidate.qisplan2.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 服务端授权的界面消息，仅传递标识，不包含客户端 API Key、配置或任意 URL。 */
public record OpenGhostScreenPayload(ResourceLocation screen) implements CustomPacketPayload {
    public static final Type<OpenGhostScreenPayload> TYPE=new Type<>(ResourceLocation.parse("qisplan2:open_ghost_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf,OpenGhostScreenPayload> STREAM_CODEC=StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,OpenGhostScreenPayload::screen,OpenGhostScreenPayload::new);
    /** 返回网络消息类型。 */
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
