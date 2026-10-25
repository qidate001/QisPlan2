package com.qidate.qisplan2.block.entity;

import com.qidate.qisplan2.block.GhostPianoBlock;
import com.qidate.qisplan2.core.ModBlocks;
import com.qidate.qisplan2.event.GhostPianoMusicHandler;
import com.qidate.qisplan2.ghost.isolation.GhostIsolationSystem;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.data.GhostModuleContainer;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.implementation.GhostPianoModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * 鬼钢琴方块实体。
 *
 * 作为统一模块化灵异宿主：
 * 1. 管理默认鬼钢琴模块。
 * 2. 在放置时初始化隔绝信息。
 * 3. 负责旧有的钢琴注册与音符粒子表现。
 */
public class GhostPianoBlockEntity
        extends AbstractModularGhostBlockEntity {

    public GhostPianoBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlocks.GHOST_PIANO_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    /**
     * 确保钢琴宿主拥有默认模块。
     */
    public void ensureDefaultModule() {
        GhostModuleContainer container =
                getGhostModuleContainer();

        if (container.hasModule(GhostPianoModule.ID)) {
            return;
        }

        GhostModule module =
                GhostModuleRegistry.get(GhostPianoModule.ID);

        if (module == null) {
            return;
        }

        GhostModuleInitialData initial =
                module.getInitialData();

        container.addModuleIfAbsent(
                module.getId(),
                initial.intensity()
        );

        markModuleDataChanged();
    }

    /**
     * 初始化放置时的隔绝信息。
     *
     * isIsolated() 只在本方法中执行，并且由方块放置流程调用。
     * 通过 customData 中的标记避免重复初始化。
     */
    public void initializeIsolationOnPlacement() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        ensureDefaultModule();

        if (!hasModule(GhostPianoModule.ID)) {
            return;
        }

        CompoundTag customData =
                getModuleCustomData(GhostPianoModule.ID);

        if (customData.getBoolean(
                GhostPianoModule.DATA_ISOLATION_CHECKED
        )) {
            return;
        }

        /*
         * 放置时唯一一次昂贵的空间检测。
         */
        boolean isolated =
                GhostIsolationSystem.isIsolated(
                        serverLevel,
                        worldPosition
                );

        /*
         * isIsolated() 可能已经建立 Region。
         * 这里只查询现存 Region，不再次触发空间检测。
         */
        UUID regionId =
                GhostIsolationSystem.getRegionId(
                        serverLevel,
                        worldPosition
                );

        CompoundTag updated =
                customData.copy();

        updated.putBoolean(
                GhostPianoModule.DATA_ISOLATION_CHECKED,
                true
        );

        updated.putBoolean(
                GhostPianoModule.DATA_WAS_ISOLATED,
                isolated
        );

        updated.putString(
                GhostPianoModule.DATA_INITIAL_REGION_ID,
                regionId == null ? "" : regionId.toString()
        );

        setModuleCustomData(
                GhostPianoModule.ID,
                updated
        );
    }

    /**
     * 服务端 Tick。
     *
     * 只有左半边负责注册钢琴与生成音符粒子。
     * 隔绝初始化不放在 Tick 中。
     */
    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            GhostPianoBlockEntity blockEntity
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (state.getValue(GhostPianoBlock.PART)
                != GhostPianoBlock.Part.LEFT) {
            return;
        }

        /*
         * 保留旧有音乐管理器的钢琴注册行为。
         */
        GhostPianoMusicHandler.registerPiano(
                serverLevel,
                pos
        );

        /*
         * 平均约每 5 tick 生成一次音符粒子。
         */
        if (serverLevel.random.nextInt(5) != 0) {
            return;
        }

        Direction facing =
                state.getValue(GhostPianoBlock.FACING);

        Direction right =
                facing.getClockWise();

        BlockPos rightPos =
                pos.relative(right);

        double x1 =
                Math.min(pos.getX(), rightPos.getX());

        double x2 =
                Math.max(pos.getX(), rightPos.getX());

        double z1 =
                Math.min(pos.getZ(), rightPos.getZ());

        double z2 =
                Math.max(pos.getZ(), rightPos.getZ());

        double x =
                x1 + 0.2D
                        + (x2 - x1 + 1.0D)
                        * serverLevel.random.nextDouble();

        double z =
                z1 + 0.2D
                        + (z2 - z1 + 1.0D)
                        * serverLevel.random.nextDouble();

        double y =
                pos.getY()
                        + 1.4D
                        + serverLevel.random.nextDouble() * 0.7D;

        serverLevel.sendParticles(
                ParticleTypes.NOTE,
                x,
                y,
                z,
                1,
                0.0D,
                0.15D,
                0.0D,
                0.0D
        );
    }
}