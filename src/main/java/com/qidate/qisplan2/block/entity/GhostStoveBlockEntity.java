package com.qidate.qisplan2.block.entity;

import com.qidate.qisplan2.core.ModBlocks;
import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import com.qidate.qisplan2.ghost.module.implementation.GhostStoveModule;
import com.qidate.qisplan2.ghost.module.data.GhostWorkstationData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** 灶台方块只负责承载、存档兼容与周期分发；规则、制作与菜单不再绑定此类。 */
public class GhostStoveBlockEntity extends AbstractModularGhostBlockEntity {
    /** 新灶台默认承载完整模块，强度沿用旧版的 1。 */
    public GhostStoveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.GHOST_STOVE_BLOCK_ENTITY.get(),pos,state);
        getGhostModuleContainer().addModuleIfAbsent(GhostStoveModule.ID,1);
    }
    /** 旧版六格库存和炼制进度迁入模块；显式空模块列表不会重新生成厉鬼。 */
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries);
        if (!tag.contains("QisPlan2GhostModules")) {
            getGhostModuleContainer().addModuleIfAbsent(GhostStoveModule.ID,1);
            getGhostModuleContainer().setCustomData(GhostStoveModule.ID,GhostWorkstationData.fromLegacy(tag));
        }
    }
    /** 即使没有匹配配方也派发周期事件，袭击规则不会被工作台空闲状态短路。 */
    public static void serverTick(Level level, BlockPos pos, BlockState state, GhostStoveBlockEntity stove) {
        if (!level.isClientSide()) GhostModuleRuntime.dispatch(stove,new ModuleTickEvent());
    }
}
