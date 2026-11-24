package com.qidate.qisplan2.ghost.possession.target;

import com.qidate.qisplan2.ghost.module.host.attachment.GhostAttachments;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;

import com.qidate.qisplan2.death.SupernaturalEntity;
import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.module.GhostModuleCombatState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class EntityGhostPossessionTarget
        implements GhostPossessionTarget {

    private final Entity ghost;
    private final ResourceLocation moduleId;

    public EntityGhostPossessionTarget(
            Entity ghost
    ) {
        this(ghost, ghost instanceof ModularGhostEntity host ? host.getGhostId() : null);
    }

    public EntityGhostPossessionTarget(Entity ghost, ResourceLocation moduleId) {
        this.ghost = ghost;
        this.moduleId = moduleId;
    }

    public Entity ghost() {
        return ghost;
    }

    /** 新宿主缺少部位、部位已满或已有同名模块时，拒绝开始驾驭。 */
    @Override public boolean canStart(ServerPlayer player) {
        var destination = new PlayerGhostModuleHost(player);
        return moduleId != null && !destination.hasModule(moduleId)
                && destination.canInstallModule(moduleId);
    }

    @Override
    public boolean onSuccess(
            ServerPlayer player
    ) {

        if (!(ghost instanceof ModularGhostEntity host) || !host.isAlive()
                || host.level() != player.level() || host.distanceToSqr(player) > 64 || moduleId == null) return false;
        var entry = host.getModules().stream().filter(e -> e.id().equals(moduleId)).findFirst().orElse(null);
        if (entry == null || (host.suppression(moduleId).canRunRules() && GhostModuleCombatState.available(entry))) return false;
        if (!new PlayerGhostModuleHost(player).possess( entry)) return false;
        host.removeGhostModule(moduleId);
        // 其他模块仍由原宿主承载，不消耗它们，也不提前返还仍插在实体上的棺材钉。
        if (!host.getModules().isEmpty()) return true;

        // 最后一个原宿主模块已转移；外来附着物独立返还，不随模块进入玩家体内。
        GhostAttachments.recover(host, player);

        /*
         * 鬼消失。
         */
        ghost.discard();

        return true;
    }


    @Override
    public void onFailure(
            ServerPlayer player
    ) {

        if (ghost instanceof SupernaturalEntity supernatural) {

            // 失败不清除沉睡或外来压制，状态仍由各自生命周期管理。
        }
    }
}
