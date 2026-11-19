package com.qidate.qisplan2.network;

import com.qidate.qisplan2.ghost.module.host.attachment.GhostAttachments;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** 附着来源只在建立、解除和开始跟踪时同步，不发送每 tick 心跳。 */
public final class GhostAttachmentNetwork {
    private GhostAttachmentNetwork() {}
    public record Payload(int entityId, UUID entityUuid, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(ResourceLocation.parse("qisplan2:ghost_attachments"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.of(
                (buffer, value) -> { buffer.writeVarInt(value.entityId()); buffer.writeUUID(value.entityUuid()); buffer.writeNbt(value.data()); },
                buffer -> new Payload(buffer.readVarInt(), buffer.readUUID(), buffer.readNbt()));
        /** 返回唯一消息类型。 */
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    /** 只有服务端到客户端方向，客户端不能伪造插入物品。 */
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Payload.TYPE, Payload.CODEC, (packet, context) -> context.enqueueWork(() -> {
            var entity = context.player().level().getEntity(packet.entityId());
            if (entity instanceof LivingEntity living && living.getUUID().equals(packet.entityUuid()) && packet.data() != null)
                GhostAttachments.receive(living, packet.data());
        }));
    }
    /** 同步周围观察者以及被钉住的玩家本人。 */
    public static void sync(LivingEntity target) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target,
                new Payload(target.getId(), target.getUUID(), GhostAttachments.snapshot(target)));
    }
    /** 新加入跟踪范围时补发当前状态。 */
    public static void syncTo(LivingEntity target, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new Payload(target.getId(), target.getUUID(), GhostAttachments.snapshot(target)));
    }
}
