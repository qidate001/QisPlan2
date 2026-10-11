package com.qidate.qisplan2.block.entity;

import com.mojang.serialization.DataResult;
import com.qidate.qisplan2.core.ModBlocks;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleCustomDataModifier;
import com.qidate.qisplan2.ghost.module.GhostModuleDataModifier;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.implementation.GhostTombstoneModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class GhostTombstoneBlockEntity extends BlockEntity
        implements GhostModuleDataModifier, GhostModuleCustomDataModifier {

    private static final String NBT_INSCRIPTION = "Inscription";
    private static final String NBT_MODULES = "QisPlan2GhostModules";

    /**
     * 墓碑自身的数据：刻字。
     */
    private String inscription = "";

    /**
     * 墓碑承载的灵异模块数据。
     */
    private final List<GhostModuleData.Entry> modules = new ArrayList<>();

    public GhostTombstoneBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlocks.GHOST_TOMBSTONE_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    public String getInscription() {
        return inscription;
    }

    /**
     * 设置墓碑上的刻字。
     */
    public void setInscription(String inscription) {
        this.inscription = inscription == null ? "" : inscription;

        setChanged();
        syncBlockEntity();
    }

    /**
     * 返回当前模块列表的不可变快照。
     */
    @Override
    public List<GhostModuleData.Entry> getModules() {
        return List.copyOf(modules);
    }

    @Override
    public boolean addModule(
            ResourceLocation moduleId,
            double intensity
    ) {
        if (moduleId == null
                || !Double.isFinite(intensity)
                || intensity < 0.0D
                || getModuleEntry(moduleId) != null) {
            return false;
        }

        modules.add(
                new GhostModuleData.Entry(
                        moduleId,
                        intensity
                )
        );

        setChanged();
        syncBlockEntity();
        return true;
    }

    @Override
    public boolean removeModule(ResourceLocation moduleId) {
        int index = findModuleIndex(moduleId);

        if (index < 0) {
            return false;
        }

        modules.remove(index);

        setChanged();
        syncBlockEntity();
        return true;
    }

    @Override
    public boolean setIntensity(
            ResourceLocation moduleId,
            double intensity
    ) {
        if (!Double.isFinite(intensity) || intensity < 0.0D) {
            return false;
        }

        GhostModuleData.Entry current = getModuleEntry(moduleId);

        if (current == null || current.intensity() == intensity) {
            return false;
        }

        replaceEntry(
                current,
                new GhostModuleData.Entry(
                        current.id(),
                        intensity,
                        current.revival(),
                        current.shutdown(),
                        current.customData()
                )
        );

        return true;
    }

    @Override
    public boolean setRevival(
            ResourceLocation moduleId,
            double revival
    ) {
        if (!Double.isFinite(revival) || revival < 0.0D) {
            return false;
        }

        GhostModuleData.Entry current = getModuleEntry(moduleId);

        if (current == null || current.revival() == revival) {
            return false;
        }

        replaceEntry(
                current,
                new GhostModuleData.Entry(
                        current.id(),
                        current.intensity(),
                        revival,
                        current.shutdown(),
                        current.customData()
                )
        );

        return true;
    }

    @Override
    public boolean setShutdown(
            ResourceLocation moduleId,
            double shutdown
    ) {
        if (!Double.isFinite(shutdown) || shutdown < 0.0D) {
            return false;
        }

        GhostModuleData.Entry current = getModuleEntry(moduleId);

        if (current == null || current.shutdown() == shutdown) {
            return false;
        }

        replaceEntry(
                current,
                new GhostModuleData.Entry(
                        current.id(),
                        current.intensity(),
                        current.revival(),
                        shutdown,
                        current.customData()
                )
        );

        return true;
    }

    @Override
    public CompoundTag getModuleCustomData(ResourceLocation moduleId) {
        GhostModuleData.Entry entry = getModuleEntry(moduleId);

        return entry == null
                ? new CompoundTag()
                : entry.customData();
    }

    @Override
    public boolean setModuleCustomData(
            ResourceLocation moduleId,
            CompoundTag customData
    ) {
        GhostModuleData.Entry current = getModuleEntry(moduleId);

        if (current == null) {
            return false;
        }

        CompoundTag newData = customData == null
                ? new CompoundTag()
                : customData.copy();

        if (current.customData().equals(newData)) {
            return false;
        }

        replaceEntry(
                current,
                current.withCustomData(newData)
        );

        return true;
    }

    /**
     * 确保墓碑拥有默认的鬼墓碑模块。
     */
    public void ensureDefaultModule() {
        if (getModuleEntry(GhostTombstoneModule.ID) != null) {
            return;
        }

        GhostModule module =
                GhostModuleRegistry.get(GhostTombstoneModule.ID);

        if (module == null) {
            return;
        }

        addModule(
                module.getId(),
                module.getInitialData().intensity()
        );
    }

    private GhostModuleData.Entry getModuleEntry(ResourceLocation moduleId) {
        if (moduleId == null) {
            return null;
        }

        for (GhostModuleData.Entry entry : modules) {
            if (entry.id().equals(moduleId)) {
                return entry;
            }
        }

        return null;
    }

    private int findModuleIndex(ResourceLocation moduleId) {
        if (moduleId == null) {
            return -1;
        }

        for (int i = 0; i < modules.size(); i++) {
            if (modules.get(i).id().equals(moduleId)) {
                return i;
            }
        }

        return -1;
    }

    private void replaceEntry(
            GhostModuleData.Entry current,
            GhostModuleData.Entry replacement
    ) {
        int index = findModuleIndex(current.id());

        if (index < 0) {
            return;
        }

        modules.set(index, replacement);

        setChanged();
        syncBlockEntity();
    }

    private void syncBlockEntity() {
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

        tag.putString(NBT_INSCRIPTION, inscription);

        DataResult<net.minecraft.nbt.Tag> result =
                GhostModuleData.CODEC.encodeStart(
                        NbtOps.INSTANCE,
                        modules
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

        inscription = tag.contains(NBT_INSCRIPTION)
                ? tag.getString(NBT_INSCRIPTION)
                : "";

        modules.clear();

        if (tag.contains(NBT_MODULES)) {
            GhostModuleData.CODEC.parse(
                    NbtOps.INSTANCE,
                    tag.get(NBT_MODULES)
            ).result().ifPresent(modules::addAll);
        }

        /*
         * 兼容旧存档：
         * 如果没有模块数据，则安装墓碑默认模块。
         */
        if (modules.isEmpty()
                && GhostModuleRegistry.get(GhostTombstoneModule.ID) != null) {
            addModule(
                    GhostTombstoneModule.ID,
                    GhostModuleRegistry.get(GhostTombstoneModule.ID)
                            .getInitialData()
                            .intensity()
            );
        }
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        CompoundTag tag = super.getUpdateTag(registries);

        tag.putString(NBT_INSCRIPTION, inscription);

        GhostModuleData.CODEC.encodeStart(
                NbtOps.INSTANCE,
                modules
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