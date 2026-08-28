package com.qidate.qisplan2.block;

import com.mojang.serialization.MapCodec;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModDimensions;
import com.qidate.qisplan2.ghost.partition.PartitionRoomPos;
import com.qidate.qisplan2.ghost.partition.PartitionSpaceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

public class GhostLeatherWallBlock
        extends Block {

    public static final MapCodec<GhostLeatherWallBlock> CODEC =
            simpleCodec(
                    GhostLeatherWallBlock::new
            );

    public GhostLeatherWallBlock(
            BlockBehaviour.Properties properties
    ) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {

        /*
         * ========================================================
         * 只接受熟羊排。
         * ========================================================
         */

        if (!stack.is(
                Items.COOKED_MUTTON
        )) {

            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        /*
         * ========================================================
         * 必须服务端。
         * ========================================================
         */

        if (level.isClientSide()) {

            return ItemInteractionResult.SUCCESS;
        }

        /*
         * ========================================================
         * 必须是 ServerPlayer。
         * ========================================================
         */

        if (!(player instanceof ServerPlayer serverPlayer)) {

            return ItemInteractionResult.FAIL;
        }

        /*
         * ========================================================
         * 必须处于划分维度。
         * ========================================================
         */

        if (!level.dimension().equals(
                ModDimensions.PARTITION_DIMENSION
        )) {

            return ItemInteractionResult.FAIL;
        }

        if (!(level instanceof ServerLevel serverLevelWorld)) {

            return ItemInteractionResult.FAIL;
        }

        /*
         * ========================================================
         * 找到当前房间。
         * ========================================================
         */

        PartitionRoomPos room =
                PartitionSpaceManager.findRoomContaining(
                        serverLevelWorld,
                        pos
                );

        if (room == null) {

            return ItemInteractionResult.FAIL;
        }

        /*
         * ========================================================
         * 得到所属 Region。
         * ========================================================
         */

        long regionId =
                PartitionSpaceManager.findRegionId(
                        pos
                );

        /*
         * ========================================================
         * 获取点击的墙面方向。
         * ========================================================
         */

        Direction direction =
                hitResult.getDirection()
                        .getOpposite();

        /*
         * ========================================================
         * 检查扩展。
         * ========================================================
         */

        boolean expanded =
                PartitionSpaceManager.expandRoom(
                        serverLevelWorld,
                        regionId,
                        room,
                        direction
                );

        if (!expanded) {

            return ItemInteractionResult.FAIL;
        }

        /*
         * ========================================================
         * 消耗 1 个熟羊排。
         * ========================================================
         */

        if (!serverPlayer.isCreative()) {

            stack.shrink(1);
        }

        QisPlan2.LOGGER.info(
                "[QisPlan2] 鬼皮箱空间扩展：pos={}，room={}，点击面={}，扩展方向={}",
                pos,
                room,
                hitResult.getDirection(),
                direction
        );
        return ItemInteractionResult.SUCCESS;
    }
}