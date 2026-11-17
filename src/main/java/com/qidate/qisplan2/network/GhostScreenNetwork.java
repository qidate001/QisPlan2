package com.qidate.qisplan2.network;

import com.qidate.qisplan2.network.payload.OpenGhostScreenPayload;
import com.qidate.qisplan2.client.screen.GhostScreenInputs;
import com.qidate.qisplan2.ghost.module.implementation.GhostBookModule;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** 能力界面网络桥；客户端能力单独实现，专用服务器不读取客户端配置。 */
public final class GhostScreenNetwork {
    /** 工具类无需实例化。 */
    private GhostScreenNetwork() {}
    /** 显式白名单，不允许服务器包构造任意客户端页面。 */
    public static boolean supports(ResourceLocation screen) { return GhostBookModule.ID.equals(screen); }
    /** 仅注册服务端到客户端的授权通知。 */
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(OpenGhostScreenPayload.TYPE,OpenGhostScreenPayload.STREAM_CODEC,
                (packet,context)->context.enqueueWork(()->GhostScreenInputs.open(packet.screen())));
    }
    /** 调用方已校验读者与媒介；此处再次过滤界面标识。 */
    public static void open(ServerPlayer player,ResourceLocation screen) {
        if (supports(screen)) PacketDistributor.sendToPlayer(player,new OpenGhostScreenPayload(screen));
    }
}
