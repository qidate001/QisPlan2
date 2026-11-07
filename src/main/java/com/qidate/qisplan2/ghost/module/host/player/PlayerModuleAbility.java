package com.qidate.qisplan2.ghost.module.host.player;

import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluence;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceSession;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 通用玩家调用适配器，没有任何具体厉鬼规则或独立数据。
 * 实际效果与元数据来自所属 GhostModule，同样经过统一执行器的状态和媒介检查。
 */
public final class PlayerModuleAbility implements GhostAbility {
    private final GhostModule module;
    public PlayerModuleAbility(GhostModule module) { this.module = module; }
    public static ResourceLocation id(GhostModule module) {
        return ResourceLocation.fromNamespaceAndPath(module.getId().getNamespace(), module.getId().getPath() + "/player_action");
    }
    public static final GhostMedium MEDIUM = new GhostMedium() {
        @Override public ResourceLocation getId() { return ResourceLocation.fromNamespaceAndPath("qisplan2", "player_host"); }
        @Override public boolean allows(GhostAbilityContext context) {
            if (!(context.host() instanceof PlayerGhostModuleHost host)
                    || !(host.player() instanceof ServerPlayer player) || !player.isAlive()
                    || !(context.parameters() instanceof PlayerAbilityParameters input)) return false;
            if (input.target() != null && (!input.target().isAlive() || input.target().level() != player.level())) return false;
            if (input.block() != null && (!player.serverLevel().hasChunkAt(input.block())
                    || player.distanceToSqr(input.block().getCenter()) > 64)) return false;
            // 玩家主动使用能力不检查规则死机；模块归属、目标及距离仍是硬性前提。
            return host.hasModule(context.moduleId());
        }
    };
    @Override public ResourceLocation getId() { return id(module); }
    @Override public boolean canExecute(GhostAbilityContext context) {
        return context.host() instanceof PlayerGhostModuleHost && context.parameters() instanceof PlayerAbilityParameters;
    }
    @Override public void execute(GhostAbilityContext context) { executeWithResult(context); }
    @Override public boolean executeWithResult(GhostAbilityContext context) {
        var host = (PlayerGhostModuleHost)context.host();
        var player = (ServerPlayer)host.player();
        var input = (PlayerAbilityParameters)context.parameters();
        var view = new PlayerGhostModuleContext(player, module.getId(), PossessionHandler.getData(player, module.getId()), input.target());
        if (input.target() != null && module.playerActionInfluence() != null) {
            GhostInfluenceSession session = new GhostInfluenceSession();
            boolean[] success = {false};
            session.submit(new GhostInfluence(host, module.getId(), getId(), input.target().getUUID(),
                    module.playerActionInfluence(), GhostModuleHosts.strength(host, module.getId()),
                    session.getSourceId(host, module.getId()), session.getBatchId()),
                    () -> success[0] = module.use(view));
            session.resolveAndApply();
            return success[0];
        }
        return input.block() == null ? module.use(view) : module.useOnBlock(view, input.block());
    }
}
