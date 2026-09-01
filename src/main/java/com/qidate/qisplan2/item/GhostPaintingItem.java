package com.qidate.qisplan2.item;

import com.qidate.qisplan2.entity.GhostPaintingEntity;
import com.qidate.qisplan2.core.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class GhostPaintingItem extends Item {

    public GhostPaintingItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {

        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }

        Direction face = context.getClickedFace();

        /*
         * 不能贴地板和天花板。
         */
        if (face.getAxis() == Direction.Axis.Y) {
            return InteractionResult.FAIL;
        }

        BlockPos wallPos = context.getClickedPos();
        BlockPos entityPos = wallPos.relative(face);

        BlockState wallState = level.getBlockState(wallPos);

        /*
         * 必须点到实体方块。
         */
        if (!wallState.isSolidRender(level, wallPos)) {
            return InteractionResult.FAIL;
        }

        GhostPaintingEntity painting =
                new GhostPaintingEntity(
                        ModEntities.GHOST_PAINTING.get(),
                        level
                );

        /*
         * 设置朝向。
         *
         * 玩家点的是墙面，
         * 所以画朝向应该反过来。
         */
        painting.setFacing(face);

        /*
         * 放到墙前一点。
         */
        Vec3 center = Vec3.atCenterOf(entityPos);

        double offset = 0.49D;

        Vec3 normal = new Vec3(
                face.getStepX(),
                face.getStepY(),
                face.getStepZ()
        );

        Vec3 spawn = center.subtract(
                normal.scale(offset)
        );

        painting.setPos(
                spawn.x,
                spawn.y,
                spawn.z
        );

        level.addFreshEntity(painting);

        if (context.getPlayer() != null
                && !context.getPlayer().isCreative()) {

            context.getItemInHand().shrink(1);
        }

        return InteractionResult.SUCCESS;
    }
}