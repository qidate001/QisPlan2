package com.qidate.qisplan2.block;

import com.qidate.qisplan2.menu.GhostStoveMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GhostStoveBlock extends Block {

    public GhostStoveBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getOcclusionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return Shapes.empty();
    }

    @Override
    public InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer) {

            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (containerId, inventory, p) ->
                                    new GhostStoveMenu(
                                            containerId,
                                            inventory
                                    ),
                            Component.translatable(
                                    "container.qisplan2.ghost_stove"
                            )
                    )
            );
        }

        return InteractionResult.sidedSuccess(
                level.isClientSide()
        );
    }
}