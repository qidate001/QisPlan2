package com.qidate.qisplan2.ghost.possession.target;

import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 从方块宿主转走选中的完整模块，不破坏方块上其他模块。 */
public final class BlockGhostPossessionTarget implements GhostPossessionTarget {
    private final BlockEntity block;
    private final ResourceLocation moduleId;
    public BlockGhostPossessionTarget(BlockEntity block, ResourceLocation moduleId) {
        this.block = block;
        this.moduleId = moduleId;
    }
    @Override public boolean onSuccess(ServerPlayer player) {
        if (block.isRemoved() || block.getLevel() != player.level()
                || player.serverLevel().getBlockEntity(block.getBlockPos()) != block
                || player.distanceToSqr(block.getBlockPos().getCenter()) > 64
                || !(block instanceof GhostModuleDataModifier host)) return false;
        var entry = host.getModules().stream().filter(e -> e.id().equals(moduleId)).findFirst().orElse(null);
        if (entry == null || GhostModuleCombatState.available(entry)) return false;
        if (!PossessionHandler.possess(player, entry)) return false;
        host.removeModule(moduleId);
        block.setChanged();
        return true;
    }
    @Override public void onFailure(ServerPlayer player) {}
}
