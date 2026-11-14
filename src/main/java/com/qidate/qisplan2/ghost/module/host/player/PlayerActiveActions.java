package com.qidate.qisplan2.ghost.module.host.player;

import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.environment.GhostActiveAction;
import com.qidate.qisplan2.ghost.module.host.player.binding.GhostPlayerBindings;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import com.qidate.qisplan2.ghost.possession.ability.ghostdoor.GhostDoorAbilityHandler;
import com.qidate.qisplan2.ghost.domain.GhostDomainManager;
import com.qidate.qisplan2.ghost.domain.type.mist.GhostMistDomainController;
import com.qidate.qisplan2.ghost.module.implementation.mist.GhostMistModule;
import com.qidate.qisplan2.ghost.domain.GhostDomainLayerHandler;
import com.qidate.qisplan2.ghost.domain.GhostDomainTeleportHandler;
import com.qidate.qisplan2.ghost.layer.GhostLayerHandler;
import com.qidate.qisplan2.ghost.reboot.GhostRebootManager;
import com.qidate.qisplan2.ghost.reboot.GhostRebootSources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.util.Mth;

/** 玩家世界端口：解析视线和物理目标，模块定义无需认识玩家或客户端。 */
public final class PlayerActiveActions {
    /** 工具类不保存模块状态。 */
    private PlayerActiveActions() {}

    /** 校验宿主可用性及鬼域类操作的硬性前提，不修改世界。 */
    public static boolean canActivate(GhostModuleHost host, ResourceLocation id, GhostActiveAction action) {
        if (!(host instanceof PlayerGhostModuleHost ph) || !(ph.player() instanceof ServerPlayer p)
                || !p.isAlive() || p.isSpectator() || !host.carriesModule(id)) return false;
        var domain = GhostDomainManager.get(p.serverLevel()).getBySourceAndType(p.getUUID(), id);
        return switch (action) {
            case DOMAIN_UP, DOMAIN_DOWN, SELF_UP, SELF_DOWN, TELEPORT, RAISE_TARGET, LOWER_TARGET -> domain != null;
            case REBOOT -> domain != null && domain.getLayer() >= 6;
            case MELEE -> target(p) != null;
            case KNOCK -> p.pick(8, 0, false).getType() == HitResult.Type.BLOCK;
            default -> true;
        };
    }

    /** 在服务端重新解析目标并执行操作，客户端只能提交两个资源 ID。 */
    public static boolean activate(GhostModuleHost host, ResourceLocation id, GhostActiveAction action) {
        if (!canActivate(host, id, action)) return false;
        var p = (ServerPlayer)((PlayerGhostModuleHost)host).player();
        var view = new PlayerGhostModuleContext(p, id, PossessionHandler.getData(p, id), action == GhostActiveAction.MELEE ? target(p) : null);
        var binding = GhostPlayerBindings.get(GhostModuleRegistry.get(id));
        var manager = GhostDomainManager.get(p.serverLevel());
        var domain = manager.getBySourceAndType(p.getUUID(), id);
        switch (action) {
            case DIVINATION_LIFE: return DivinationSlipInput.useResult(p, 0);
            case DIVINATION_DEATH: return DivinationSlipInput.useResult(p, 1);
            case DIVINATION_GHOST: return DivinationSlipInput.useResult(p, 2);
            case TOGGLE_DOMAIN:
                if (id.equals(GhostMistModule.ID)) {
                    boolean before = GhostMistDomainController.isOpen(p);
                    GhostMistDomainController.toggle(p);
                    return before != GhostMistDomainController.isOpen(p);
                }
                return binding.use(view);
            case MELEE: return binding.use(view);
            case DOOR_HUNT: return GhostDoorAbilityHandler.use(p, id);
            case KNOCK:
                var hit = p.pick(8, 0, false);
                return hit instanceof BlockHitResult block && p.serverLevel().hasChunkAt(block.getBlockPos())
                        && binding.useOnBlock(view, block.getBlockPos());
            case TELEPORT: return GhostDomainTeleportHandler.teleport(p, domain);
            case RAISE_TARGET: return GhostDomainLayerHandler.changeLayer(p, 1, domain);
            case LOWER_TARGET: return GhostDomainLayerHandler.changeLayer(p, -1, domain);
            case REBOOT:
                GhostRebootManager.toggleContinuousReboot(p, GhostRebootSources.ghost(p.getUUID(), id));
                return true;
            case DOMAIN_UP, DOMAIN_DOWN:
                int layer = Mth.clamp(domain.getLayer() + (action == GhostActiveAction.DOMAIN_UP ? 1 : -1), 1, 6);
                if (layer == domain.getLayer()) return false;
                manager.updateLayer(domain.getId(), layer);
                GhostLayerHandler.setLayer(p, layer);
                return true;
            case SELF_UP, SELF_DOWN:
                int current = GhostLayerHandler.getLayer(p);
                int next = Mth.clamp(current + (action == GhostActiveAction.SELF_UP ? 1 : -1), 1, domain.getLayer());
                if (next == current) return false;
                GhostLayerHandler.setLayer(p, next);
                return true;
            default: return false;
        }
    }

    /** 近战目标仅来自服务端视线，墙后及射程外实体均不能被客户端指定。 */
    private static LivingEntity target(ServerPlayer p) {
        var start = p.getEyePosition();
        var end = start.add(p.getLookAngle().scale(3));
        LivingEntity best = null;
        double nearest = 9;
        for (var entity : p.serverLevel().getEntitiesOfClass(LivingEntity.class,
                p.getBoundingBox().expandTowards(p.getLookAngle().scale(3)).inflate(1),
                e -> e != p && e.isAlive() && !e.isSpectator())) {
            var hit = entity.getBoundingBox().inflate(0.1).clip(start, end);
            if (hit.isPresent() && p.hasLineOfSight(entity) && start.distanceToSqr(hit.get()) < nearest) {
                nearest = start.distanceToSqr(hit.get());
                best = entity;
            }
        }
        return best;
    }
}
