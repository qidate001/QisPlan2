package com.qidate.qisplan2.network;

import com.qidate.qisplan2.ghost.module.ability.GhostAbilityCatalog;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityExecutor;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import com.qidate.qisplan2.network.payload.GhostAbilityUsePayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.Map;
import java.util.WeakHashMap;

/** 所有主动技能共用的服务端入口；UI 数量限制不影响未来其他合法输入渠道。 */
public final class GhostAbilityNetwork {
    private static final Map<ServerPlayer, Long> LAST_REQUEST = new WeakHashMap<>();
    /** 网络工具无需实例化。 */
    private GhostAbilityNetwork() {}

    /** 在主线程验证归属与主动声明，再交给统一执行器校验媒介。 */
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(GhostAbilityUsePayload.TYPE, GhostAbilityUsePayload.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) return;
                    long now = player.serverLevel().getGameTime();
                    Long previous = LAST_REQUEST.put(player, now);
                    if (previous != null && now >= previous && now - previous < 2) return;
                    var host = new PlayerGhostModuleHost(player);
                    boolean known = GhostAbilityCatalog.all(host).stream().anyMatch(entry ->
                            entry.moduleId().equals(packet.moduleId()) && entry.ability().getId().equals(packet.abilityId()));
                    if (!known) return;
                    boolean success = GhostAbilityExecutor.tryExecuteWithResult(new GhostAbilityContext(host,
                            packet.moduleId(), packet.abilityId(), GhostAbilityContext.Source.POSSESSION, null));
                    if (!success) player.displayClientMessage(Component.translatable("hud.qisplan2.ability_wheel.unavailable"), true);
                }));
    }
}
