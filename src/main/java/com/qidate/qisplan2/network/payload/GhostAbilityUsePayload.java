package com.qidate.qisplan2.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 通用技能输入只携带标识与可选数字，强度、目标、媒介仍由服务端决定。 */
public record GhostAbilityUsePayload(ResourceLocation moduleId, ResourceLocation abilityId, Integer number) implements CustomPacketPayload {
    public static final Type<GhostAbilityUsePayload> TYPE = new Type<>(ResourceLocation.parse("qisplan2:ability_use"));
    /** 普通无参数能力保持原来的构造方式。 */
    public GhostAbilityUsePayload(ResourceLocation moduleId, ResourceLocation abilityId) { this(moduleId, abilityId, null); }
    public static final StreamCodec<RegistryFriendlyByteBuf, GhostAbilityUsePayload> STREAM_CODEC = new StreamCodec<>() {
        /** 按固定字段读取，数字有独立的存在标记。 */
        public GhostAbilityUsePayload decode(RegistryFriendlyByteBuf buf) {
            return new GhostAbilityUsePayload(buf.readResourceLocation(), buf.readResourceLocation(), buf.readBoolean() ? buf.readVarInt() : null);
        }
        /** 不序列化任何客户端推断的目标或强度。 */
        public void encode(RegistryFriendlyByteBuf buf, GhostAbilityUsePayload value) {
            buf.writeResourceLocation(value.moduleId()); buf.writeResourceLocation(value.abilityId());
            buf.writeBoolean(value.number() != null); if (value.number() != null) buf.writeVarInt(value.number());
        }
    };
    /** 返回消息类型。 */
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
