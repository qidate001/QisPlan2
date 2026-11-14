package com.qidate.qisplan2.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 统一技能输入仅携带模块与能力 ID，不接收客户端提供的强度、目标或媒介结论。 */
public record GhostAbilityUsePayload(ResourceLocation moduleId, ResourceLocation abilityId) implements CustomPacketPayload {
    public static final Type<GhostAbilityUsePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("qisplan2", "ability_use"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GhostAbilityUsePayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, GhostAbilityUsePayload::moduleId,
            ResourceLocation.STREAM_CODEC, GhostAbilityUsePayload::abilityId, GhostAbilityUsePayload::new);
    /** 返回统一释放消息类型。 */
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
