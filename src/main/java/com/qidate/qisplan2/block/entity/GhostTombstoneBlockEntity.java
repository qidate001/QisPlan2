package com.qidate.qisplan2.block.entity;

import com.mojang.serialization.DataResult;
import com.qidate.qisplan2.core.ModBlocks;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.implementation.GhostTombstoneModule;
import com.qidate.qisplan2.network.ghosttombstone.GhostTombstoneNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class GhostTombstoneBlockEntity extends BlockEntity
        implements GhostModuleDataModifier,
        GhostModuleCustomDataModifier,
        GhostInscriptionCapability {

    private static final String NBT_MODULES = "QisPlan2GhostModules";
    private static final String NBT_LEGACY_INSCRIPTION = "Inscription";
    private static final String DATA_INSCRIPTION = "Inscription";

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

    @Override
    public boolean requestInscription(LivingEntity user) {
        if (!(user instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        if (serverPlayer.serverLevel() != serverLevel) {
            return false;
        }

        GhostTombstoneNetwork.sendOpenScreen(
                serverPlayer,
                worldPosition
        );

        return true;
    }

    /**
     * 获取鬼墓碑模块保存的碑文。
     */
    public String getInscription() {
        CompoundTag data =
                getModuleCustomData(GhostTombstoneModule.ID);

        return data.getString(DATA_INSCRIPTION);
    }

    /**
     * 设置鬼墓碑模块保存的碑文。
     */
    public void setInscription(String inscription) {
        GhostModuleData.Entry entry =
                getModuleEntry(GhostTombstoneModule.ID);

        // 模块不存在时，不读写碑文，也不自动恢复模块。
        if (entry == null) {
            return;
        }

        CompoundTag data = entry.customData().copy();
        data.putString(
                DATA_INSCRIPTION,
                inscription == null ? "" : inscription
        );

        if (setModuleCustomData(GhostTombstoneModule.ID, data)) {
            setChanged();
            syncBlockEntity();
        }
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

        modules.clear();

        // 先加载已有模块数据。
        if (tag.contains(NBT_MODULES)) {
            GhostModuleData.CODEC.parse(
                    NbtOps.INSTANCE,
                    tag.get(NBT_MODULES)
            ).result().ifPresent(modules::addAll);
        } else {
            /*
             * 兼容没有模块数据的旧存档。
             * 只有模块字段不存在时才添加默认模块。
             * 如果存档明确保存了空模块列表，则尊重空列表。
             */
            GhostModule module =
                    GhostModuleRegistry.get(GhostTombstoneModule.ID);

            if (module != null) {
                GhostModuleInitialData initial = module.getInitialData();

                modules.add(
                        new GhostModuleData.Entry(
                                module.getId(),
                                initial.intensity(),
                                initial.revival(),
                                initial.shutdown(),
                                new CompoundTag()
                        )
                );
            }
        }

        /*
         * 迁移旧碑文：
         * 旧版本将碑文单独保存为 Inscription。
         * 新版本将其放入鬼墓碑模块的自定义数据。
         */
        if (tag.contains(NBT_LEGACY_INSCRIPTION)) {
            int index = findModuleIndex(GhostTombstoneModule.ID);

            if (index >= 0) {
                GhostModuleData.Entry current = modules.get(index);
                CompoundTag customData = current.customData().copy();

                // 旧字段存在时，以旧字段为准完成一次性迁移。
                customData.putString(
                        DATA_INSCRIPTION,
                        tag.getString(NBT_LEGACY_INSCRIPTION)
                );

                modules.set(
                        index,
                        current.withCustomData(customData)
                );
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        CompoundTag tag = super.getUpdateTag(registries);

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