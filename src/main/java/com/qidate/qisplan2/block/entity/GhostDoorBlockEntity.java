package com.qidate.qisplan2.block.entity;

import com.qidate.qisplan2.core.ModBlocks;
import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import com.qidate.qisplan2.ghost.module.implementation.GhostDoorModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/** 鬼门下半部保存模块数据，计时与袭击全部属于模块。 */
public final class GhostDoorBlockEntity extends AbstractModularGhostBlockEntity {
    /** 新放置的门默认装载完整鬼门模块。 */
    public GhostDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.GHOST_DOOR_BLOCK_ENTITY.get(), pos, state);
        getGhostModuleContainer().addModuleIfAbsent(GhostDoorModule.ID, 20);
    }
    /** 旧世界没有模块标签时补默认值；显式空模块列表不会被重新填回。 */
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (!tag.contains("QisPlan2GhostModules")) getGhostModuleContainer().addModuleIfAbsent(GhostDoorModule.ID, 20);
    }
    /** 服务端周期入口只分发语义事件。 */
    public void tickModules() { GhostModuleRuntime.dispatch(this, new ModuleTickEvent()); }
}
