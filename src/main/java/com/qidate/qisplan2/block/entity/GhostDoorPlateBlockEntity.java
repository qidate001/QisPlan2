package com.qidate.qisplan2.block.entity;

import com.qidate.qisplan2.core.ModBlocks;
import com.qidate.qisplan2.ghost.doorplate.GhostDoorPlateRegistry;
import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import com.qidate.qisplan2.ghost.module.implementation.GhostDoorPlateModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** 门牌是模块数据的方块宿主；号码与绑定门都存放在模块 customData 中。 */
public class GhostDoorPlateBlockEntity extends AbstractModularGhostBlockEntity {
    public static final int DEFAULT_NUMBER = GhostDoorPlateModule.DEFAULT_NUMBER;
    private Integer registeredNumber;
    /** 初始化新方块的默认模块。 */
    public GhostDoorPlateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.GHOST_DOOR_PLATE_BLOCK_ENTITY.get(), pos, state);
        getGhostModuleContainer().addModuleIfAbsent(GhostDoorPlateModule.ID, 20);
    }
    /** 供渲染与路由索引读取号码，没有模块时仅返回显示默认值。 */
    public int getNumber() {
        var d = getModuleCustomData(GhostDoorPlateModule.ID); return d.contains("number") ? d.getInt("number") : DEFAULT_NUMBER;
    }
    /** 世界交互更新模块号码，范围与公共技能一致。 */
    public void setNumber(int number) {
        if (!hasModule(GhostDoorPlateModule.ID) || number < 0 || number > 999999999) return;
        var d = getModuleCustomData(GhostDoorPlateModule.ID); d.putInt("number", number);
        setModuleCustomData(GhostDoorPlateModule.ID, d); syncIndex();
    }
    /** 读取模块保存的门坐标，返回不可变位置。 */
    public BlockPos getLinkedDoorPos() {
        var d = getModuleCustomData(GhostDoorPlateModule.ID); return d.contains("linkedDoor") ? BlockPos.of(d.getLong("linkedDoor")) : null;
    }
    /** 放置阶段由适配层保存空间绑定，不在模块中寻找具体方块。 */
    public void setLinkedDoorPos(BlockPos pos) {
        if (!hasModule(GhostDoorPlateModule.ID)) return;
        var d = getModuleCustomData(GhostDoorPlateModule.ID);
        if (pos == null) d.remove("linkedDoor"); else d.putLong("linkedDoor", pos.asLong());
        setModuleCustomData(GhostDoorPlateModule.ID, d);
    }
    /** 兼容旧 Number/LinkedDoorPos 标签；一旦存在新模块数据便以它为准。 */
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (!tag.contains("QisPlan2GhostModules")) {
            getGhostModuleContainer().addModuleIfAbsent(GhostDoorPlateModule.ID, 20);
            var d = new CompoundTag(); d.putInt("number", tag.contains("Number") ? tag.getInt("Number") : DEFAULT_NUMBER);
            if (tag.contains("LinkedDoorPos")) d.putLong("linkedDoor", tag.getLong("LinkedDoorPos"));
            getGhostModuleContainer().setCustomData(GhostDoorPlateModule.ID, d);
        }
        registeredNumber = null;
    }
    /** 加载后登记实际模块号码，不把构造默认值写入全局索引。 */
    @Override public void onLoad() { super.onLoad(); syncIndex(); }
    /** 只有号码改变或模块移除时才改写索引，避免每 tick 写存档。 */
    private void syncIndex() {
        if (!(level instanceof ServerLevel server)) return;
        if (!hasModule(GhostDoorPlateModule.ID)) {
            if (registeredNumber != null) GhostDoorPlateRegistry.unregisterPosition(server, worldPosition);
            registeredNumber = null; return;
        }
        int number = getNumber();
        if (registeredNumber == null || registeredNumber != number) {
            GhostDoorPlateRegistry.register(number, server, worldPosition); registeredNumber = number;
        }
    }
    /** 分发通用周期语义；传送规则由模块执行。 */
    public void tickModules() { syncIndex(); GhostModuleRuntime.dispatch(this, new ModuleTickEvent()); }
}
