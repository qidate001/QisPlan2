package com.qidate.qisplan2.item;

import com.qidate.qisplan2.core.ModEntities;
import com.qidate.qisplan2.entity.GhostPaintingEntity;
import com.qidate.qisplan2.entity.GhostPaintingVariant;
import com.qidate.qisplan2.entity.GhostPaintingVariants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class GhostPaintingItem extends Item {

    /*
     * ========================================
     * 放置结果
     * ========================================
     */

    private enum PlacementResult {

        SUCCESS,

        NOT_WALL,

        WALL_TOO_SMALL,

        SPACE_BLOCKED,

        ENTITY_BLOCKED
    }

    public GhostPaintingItem(Properties properties) {
        super(properties);
    }

    /*
     * ========================================
     * 右键放置
     * ========================================
     */

    @Override
    public InteractionResult useOn(UseOnContext context) {

        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }

        Direction face =
                context.getClickedFace();

        /*
         * ========================================
         * 本次放置的鬼画类型
         * ========================================
         */

        GhostPaintingVariant variant =
                GhostPaintingVariants.get(
                        GhostPaintingVariants.LANDSCAPE
                );

        /*
         * 必须点击墙面。
         */
        if (face.getAxis() == Direction.Axis.Y) {

            sendPlacementMessage(
                    context,
                    PlacementResult.NOT_WALL
            );

            return InteractionResult.FAIL;
        }

        /*
         * 画的中心。
         *
         * 位于墙前一格。
         */
        BlockPos center =
                context.getClickedPos()
                        .relative(face);

        PlacementResult result =
                canPlacePainting(
                        level,
                        center,
                        face,
                        context.getPlayer(),
                        variant
                );

        if (result != PlacementResult.SUCCESS) {

            sendPlacementMessage(
                    context,
                    result
            );

            return InteractionResult.FAIL;
        }

        GhostPaintingEntity painting =
                new GhostPaintingEntity(
                        ModEntities.GHOST_PAINTING.get(),
                        level
                );

        painting.setPaintingId(
                GhostPaintingVariants.LANDSCAPE
        );

        painting.setFacing(face);

        /*
         * 稍微离墙一点。
         */
        Vec3 spawn =
                Vec3.atCenterOf(center)
                        .subtract(
                                face.getStepX() * 0.49D,
                                face.getStepY() * 0.49D,
                                face.getStepZ() * 0.49D
                        );

        painting.setPos(
                spawn.x,
                spawn.y,
                spawn.z
        );

        level.addFreshEntity(
                painting
        );

        if (context.getPlayer() != null
                && !context.getPlayer().isCreative()) {

            context.getItemInHand().shrink(1);
        }

        return InteractionResult.SUCCESS;
    }

    /*
     * ========================================
     * 检查能否放置
     * ========================================
     */

    private PlacementResult canPlacePainting(
            ServerLevel level,
            BlockPos center,
            Direction face,
            Entity placer,
            GhostPaintingVariant variant
    ) {

        int halfWidth =
                variant.width() / 2;

        int halfHeight =
                variant.height() / 2;

        /*
         * 墙上的水平轴。
         */
        Direction right =
                face.getClockWise();

        /*
         * 墙上的竖直轴。
         */
        Direction up =
                Direction.UP;

        /*
         * 整个 13×7。
         */
        for (int x = -halfWidth; x <= halfWidth; x++) {

            for (int y = -halfHeight; y <= halfHeight; y++) {

                BlockPos paintingPos =
                        center.relative(
                                        right,
                                        x
                                )
                                .relative(
                                        up,
                                        y
                                );

                /*
                 * 墙后。
                 */
                BlockPos support =
                        paintingPos.relative(
                                face.getOpposite()
                        );

                /*
                 * 墙前。
                 */
                BlockPos front =
                        paintingPos;

                BlockState supportState =
                        level.getBlockState(
                                support
                        );

                /*
                 * 墙必须完整。
                 */
                if (!supportState.isSolidRender(
                        level,
                        support
                )) {

                    return PlacementResult.WALL_TOO_SMALL;
                }

                /*
                 * 前方必须为空。
                 */
                if (!level.getBlockState(
                        front
                ).canBeReplaced()) {

                    return PlacementResult.SPACE_BLOCKED;
                }
            }
        }

        /*
         * ========================================
         * 实体占位检测
         * ========================================
         */
        double halfW = halfWidth + 0.5D;
        double halfH = halfHeight + 0.5D;
        double thickness = 0.15D;

        Vec3 c = Vec3.atCenterOf(center);

        AABB box;

        if (face.getAxis() == Direction.Axis.Z) {

            box = new AABB(
                    c.x - halfW,
                    c.y - halfH,
                    c.z - thickness,
                    c.x + halfW,
                    c.y + halfH,
                    c.z + thickness
            );

        } else {

            box = new AABB(
                    c.x - thickness,
                    c.y - halfH,
                    c.z - halfW,
                    c.x + thickness,
                    c.y + halfH,
                    c.z + halfW
            );
        }

        for (Entity entity : level.getEntities(null, box)) {

            /*
             * 自己已经存在的鬼画不算。
             */
            if (entity instanceof GhostPaintingEntity) {
                continue;
            }

            /*
             * 玩家自己也不应该阻止放置。
             */
            if (entity == placer) {
                continue;
            }

            return PlacementResult.ENTITY_BLOCKED;
        }

        return PlacementResult.SUCCESS;
    }

    /*
     * ========================================
     * 提示
     * ========================================
     */

    private void sendPlacementMessage(
            UseOnContext context,
            PlacementResult result
    ) {

        if (context.getPlayer() == null) {
            return;
        }

        Component message =
                switch (result) {

                    case NOT_WALL ->
                            Component.literal(
                                    "§c鬼画只能悬挂在墙上。"
                            );

                    case WALL_TOO_SMALL ->
                            Component.literal(
                                    "§c墙面不足 13×7。"
                            );

                    case SPACE_BLOCKED ->
                            Component.literal(
                                    "§c鬼画前方空间不足。"
                            );

                    case ENTITY_BLOCKED ->
                            Component.literal(
                                    "§c鬼画位置被实体占据。"
                            );

                    default ->
                            Component.empty();
                };

        context.getPlayer().displayClientMessage(
                message,
                true
        );
    }
}