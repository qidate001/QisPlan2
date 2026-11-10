package com.qidate.qisplan2.ghost.module.host.player;
import com.qidate.qisplan2.ghost.module.host.player.binding.GhostPlayerBindings;

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

    /**
     * 绑定目标模块定义，为其提供通用玩家输入桥。
     * 本对象不复制模块持久状态，也不拥有独立的厉鬼身份。
     */
    public PlayerModuleAbility(GhostModule module) { this.module = module; }

    /**
     * 构造或返回本定义使用的资源标识，保持注册与调用入口一致。
     */
    public static ResourceLocation id(GhostModule module) {
        return ResourceLocation.fromNamespaceAndPath(module.getId().getNamespace(), module.getId().getPath() + "/player_action");
    }
    public static final GhostMedium MEDIUM = new GhostMedium() {
        /**
         * 获取当前定义的唯一标识。
         * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
         */
        @Override public ResourceLocation getId() { return ResourceLocation.fromNamespaceAndPath("qisplan2", "player_host"); }

        /**
         * 校验本媒介要求的环境、事件或输入条件。
         * 返回 false 会阻止本次能力执行，主动使用也不能绕过该检查。
         */
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

    /**
     * 获取当前定义的唯一标识。
     * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
     */
    @Override public ResourceLocation getId() { return id(module); }

    /**
     * 检查本次能力调用的事件、参数或环境是否可用于执行。
     * 模块可用性与媒介由统一执行器共同校验。
     */
    @Override public boolean canExecute(GhostAbilityContext context) {
        return context.host() instanceof PlayerGhostModuleHost && context.parameters() instanceof PlayerAbilityParameters;
    }

    /**
     * 执行当前能力的效果。
     * 调用方应使用统一执行器，以确保模块状态和媒介已经检查。
     */
    @Override public void execute(GhostAbilityContext context) { executeWithResult(context); }

    /**
     * 执行能力并返回实际效果是否成功。
     * 返回值供主动交互及成功后结算使用，不能等同于仅接受了调用。
     */
    @Override public boolean executeWithResult(GhostAbilityContext context) {
        var host = (PlayerGhostModuleHost)context.host();
        var player = (ServerPlayer)host.player();
        var input = (PlayerAbilityParameters)context.parameters();
        var view = new PlayerGhostModuleContext(player, module.getId(), PossessionHandler.getData(player, module.getId()), input.target());
        if (input.target() != null && module.activeInfluence() != null) {
            GhostInfluenceSession session = new GhostInfluenceSession();
            boolean[] success = {false};
            session.submit(new GhostInfluence(host, module.getId(), getId(), input.target().getUUID(),
                    module.activeInfluence(), GhostModuleHosts.strength(host, module.getId()),
                    session.getSourceId(host, module.getId()), session.getBatchId()),
                    () -> success[0] = GhostPlayerBindings.get(module).use(view));
            session.resolveAndApply();
            return success[0];
        }
        return input.block() == null ? GhostPlayerBindings.get(module).use(view) : GhostPlayerBindings.get(module).useOnBlock(view, input.block());
    }
}
