package com.qidate.qisplan2.block.entity;

import com.mojang.serialization.DataResult;
import com.qidate.qisplan2.ghost.module.GhostModuleCustomDataModifier;
import com.qidate.qisplan2.ghost.module.GhostModuleDataModifier;
import com.qidate.qisplan2.ghost.module.data.GhostModuleContainer;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Objects;

/**
 * 通用模块化灵异方块实体基类。
 *
 * 统一管理模块数据、NBT 持久化和方块实体更新同步。
 * 具体方块的特殊行为由子类负责。
 */
public abstract class AbstractModularGhostBlockEntity
        extends BlockEntity
        implements GhostModuleDataModifier,
        GhostModuleCustomDataModifier {

    private static final String NBT_MODULES = "QisPlan2GhostModules";

    private final GhostModuleContainer ghostModules =
            new GhostModuleContainer();

    protected AbstractModularGhostBlockEntity(
            net.minecraft.world.level.block.entity.BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
    }

    /**
     * 获取模块数据容器。
     *
     * 子类可以通过此方法完成默认模块初始化等特殊操作。
     */
    protected final GhostModuleContainer getGhostModuleContainer() {
        return ghostModules;
    }

    @Override
    public final List<GhostModuleData.Entry> getModules() {
        return ghostModules.getModules();
    }

    protected final GhostModuleData.Entry getModuleEntry(
            ResourceLocation moduleId
    ) {
        if (moduleId == null) {
            return null;
        }

        return ghostModules.getModule(moduleId);
    }

    @Override
    public boolean addModule(
            ResourceLocation moduleId,
            double intensity
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        // 保留方块实体原有语义：已有模块时不重复添加。
        if (!ghostModules.addModuleIfAbsent(moduleId, intensity)) {
            return false;
        }

        markModuleDataChanged();
        return true;
    }

    @Override
    public boolean removeModule(ResourceLocation moduleId) {
        Objects.requireNonNull(moduleId, "moduleId");

        if (!ghostModules.removeModule(moduleId)) {
            return false;
        }

        markModuleDataChanged();
        return true;
    }

    @Override
    public boolean setIntensity(
            ResourceLocation moduleId,
            double intensity
    ) {
        if (!ghostModules.setIntensity(moduleId, intensity)) {
            return false;
        }

        markModuleDataChanged();
        return true;
    }

    @Override
    public boolean setRevival(
            ResourceLocation moduleId,
            double revival
    ) {
        if (!ghostModules.setRevival(moduleId, revival)) {
            return false;
        }

        markModuleDataChanged();
        return true;
    }

    /**
     * 修改模块沉睡状态，保留其他属性。
     */
    @Override
    public boolean setSleeping(
            ResourceLocation moduleId,
            boolean sleeping
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        if (!ghostModules.setSleeping(moduleId, sleeping)) {
            return false;
        }

        markModuleDataChanged();
        return true;
    }

    @Override
    public CompoundTag getModuleCustomData(
            ResourceLocation moduleId
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        return ghostModules.getCustomData(moduleId);
    }

    @Override
    public boolean setModuleCustomData(
            ResourceLocation moduleId,
            CompoundTag customData
    ) {
        Objects.requireNonNull(moduleId, "moduleId");
        Objects.requireNonNull(customData, "customData");

        if (!ghostModules.setCustomData(moduleId, customData)) {
            return false;
        }

        markModuleDataChanged();
        return true;
    }

    /**
     * 模块数据发生变化后统一标记存档并同步方块实体。
     */
    protected final void markModuleDataChanged() {
        setChanged();
        syncBlockEntity();
    }

    protected final void syncBlockEntity() {
        if (level instanceof ServerLevel) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    3
            );
        }
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {
        super.saveAdditional(tag, provider);

        DataResult<net.minecraft.nbt.Tag> result =
                GhostModuleData.CODEC.encodeStart(
                        NbtOps.INSTANCE,
                        ghostModules.getModules()
                );

        result.result().ifPresent(
                moduleTag -> tag.put(NBT_MODULES, moduleTag)
        );
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {
        super.loadAdditional(tag, provider);

        ghostModules.clear();

        if (!tag.contains(NBT_MODULES)) {
            return;
        }

        GhostModuleData.CODEC.parse(
                NbtOps.INSTANCE,
                tag.get(NBT_MODULES)
        ).result().ifPresent(ghostModules::loadEntries);
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        CompoundTag tag = super.getUpdateTag(registries);

        GhostModuleData.CODEC.encodeStart(
                NbtOps.INSTANCE,
                ghostModules.getModules()
        ).result().ifPresent(
                moduleTag -> tag.put(NBT_MODULES, moduleTag)
        );

        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}