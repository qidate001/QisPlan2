package com.qidate.qisplan2.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 有界面板操作请求。服务端只接受本人已承载模块声明的控件、能力与自动化。 */
public record GhostDomainPanelPayload(ResourceLocation module,String action,String key,int value,String text,String extra) implements CustomPacketPayload {
    public static final Type<GhostDomainPanelPayload> TYPE=new Type<>(ResourceLocation.parse("qisplan2:domain_panel"));
    public static final StreamCodec<RegistryFriendlyByteBuf,GhostDomainPanelPayload> STREAM_CODEC=new StreamCodec<>(){
        /** 解码时限制所有字符串，避免超长配置占用服务器资源。 */
        public GhostDomainPanelPayload decode(RegistryFriendlyByteBuf b){return new GhostDomainPanelPayload(b.readResourceLocation(),b.readUtf(32),b.readUtf(256),b.readVarInt(),b.readUtf(512),b.readUtf(512));}
        /** 只传操作所需数据，不允许客户端上传任意模块 NBT。 */
        public void encode(RegistryFriendlyByteBuf b,GhostDomainPanelPayload p){b.writeResourceLocation(p.module);b.writeUtf(p.action,32);b.writeUtf(p.key,256);b.writeVarInt(p.value);b.writeUtf(p.text,512);b.writeUtf(p.extra,512);}
    };
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
