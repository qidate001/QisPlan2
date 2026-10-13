package com.qidate.qisplan2.block.entity;

import com.qidate.qisplan2.core.ModBlocks;
import com.qidate.qisplan2.ghost.module.GhostInscriptionCapability;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.data.GhostModuleContainer;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.implementation.GhostTombstoneModule;
import com.qidate.qisplan2.network.ghosttombstone.GhostTombstoneNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class GhostTombstoneBlockEntity
        extends AbstractModularGhostBlockEntity
        implements GhostInscriptionCapability {

    private static final String NBT_LEGACY_INSCRIPTION = "Inscription";
    private static final String DATA_INSCRIPTION = "Inscription";

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
     * 确保墓碑拥有默认的鬼墓碑模块。
     */
    public void ensureDefaultModule() {
        GhostModuleContainer container = getGhostModuleContainer();

        if (container.hasModule(GhostTombstoneModule.ID)) {
            return;
        }

        GhostModule module =
                GhostModuleRegistry.get(GhostTombstoneModule.ID);

        if (module == null) {
            return;
        }

        GhostModuleInitialData initial = module.getInitialData();

        if (container.addModuleIfAbsent(
                module.getId(),
                initial.intensity()
        )) {
            markModuleDataChanged();
        }
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {
        super.loadAdditional(tag, provider);

        if (!tag.contains(NBT_LEGACY_INSCRIPTION)) {
            return;
        }

        GhostModuleData.Entry entry =
                getModuleEntry(GhostTombstoneModule.ID);

        if (entry == null) {
            return;
        }

        CompoundTag customData = entry.customData().copy();

        customData.putString(
                DATA_INSCRIPTION,
                tag.getString(NBT_LEGACY_INSCRIPTION)
        );

        // 通过基类统一处理数据修改与同步。
        setModuleCustomData(
                GhostTombstoneModule.ID,
                customData
        );
    }
}