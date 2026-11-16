package com.qidate.qisplan2.block;

import com.qidate.qisplan2.block.entity.GhostDoorBlockEntity;
import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.event.DoorChangedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

/** 鬼门外观与原版开关交互；自动行为由下半部模块宿主驱动。 */
public class GhostDoorBlock extends DoorBlock implements EntityBlock {
    /** 保持现有方块注册参数。 */
    public GhostDoorBlock(BlockSetType type, Properties properties) { super(type, properties); }
    /** 双格门仅下半部保存模块，避免两次计时与两次袭击。 */
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new GhostDoorBlockEntity(pos, state) : null;
    }
    /** 仅服务端分发周期事件。 */
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (world, pos, current, entity) -> {
            if (entity instanceof GhostDoorBlockEntity door) door.tickModules();
        };
    }
    /** 保留只能从正面打开的交互，并为旧存档首次交互补建模块宿主。 */
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        var facing = state.getValue(FACING);
        double dot = (player.getX()-pos.getX()-.5)*facing.getStepX() + (player.getZ()-pos.getZ()-.5)*facing.getStepZ();
        if (dot >= 0) return InteractionResult.PASS;
        var lower = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        if (!level.isClientSide() && !(level.getBlockEntity(lower) instanceof GhostDoorBlockEntity))
            level.setBlockEntity(new GhostDoorBlockEntity(lower, level.getBlockState(lower)));
        var result = super.useWithoutItem(state, level, pos, player, hit);
        if (!level.isClientSide() && level.getBlockEntity(lower) instanceof GhostDoorBlockEntity door) {
            var current = level.getBlockState(lower);
            if (current.getBlock() instanceof DoorBlock && current.getValue(OPEN) != state.getValue(OPEN))
                GhostModuleRuntime.dispatch(door, new DoorChangedEvent(player, lower, current, current.getValue(OPEN)));
        }
        return result;
    }
}
