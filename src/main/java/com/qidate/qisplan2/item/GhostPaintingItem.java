package com.qidate.qisplan2.item;

import com.qidate.qisplan2.core.ModEntities;
import com.qidate.qisplan2.entity.GhostPaintingEntity;
import com.qidate.qisplan2.entity.GhostPaintingVariant;
import com.qidate.qisplan2.entity.GhostPaintingVariants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
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
         * 获取本次鬼画的 Variant
         * ========================================
         *
         * 普通鬼画：
         *     → landscape
         *
         * 带有：
         *     PaintingVariant:"qisplan2:orokin"
         *
         * 的鬼画：
         *     → orokin
         */

        ResourceLocation variantId =
                getPaintingVariant(
                        context.getItemInHand()
                );

        GhostPaintingVariant variant =
                GhostPaintingVariants.get(
                        variantId
                );

        /*
         * 必须点击墙面。
         */
        if (face.getAxis() == Direction.Axis.Y) {

            sendPlacementMessage(
                    context,
                    PlacementResult.NOT_WALL,
                    variant
            );

            return InteractionResult.FAIL;
        }

        /*
         * 画的中心锚点。
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
                    result,
                    variant
            );

            return InteractionResult.FAIL;
        }

        /*
         * ========================================
         * 创建实体
         * ========================================
         */

        GhostPaintingEntity painting =
                new GhostPaintingEntity(
                        ModEntities.GHOST_PAINTING.get(),
                        level
                );

        /*
         * ★ 关键：
         * 把物品上的 Variant 传给实体。
         */
        painting.setPaintingId(
                variantId
        );

        painting.setFacing(face);

        /*
         * ========================================
         * 计算实体中心
         * ========================================
         *
         * 偶数尺寸的画没有正好位于一个方块中心。
         *
         * 例如：
         *
         * 13×7：
         *     不需要偏移
         *
         * 3×4：
         *     高度为偶数
         *     中心需要向上偏移 0.5
         */

        double verticalOffset =
                variant.height() % 2 == 0
                        ? 0.5D
                        : 0.0D;

        double horizontalOffset =
                variant.width() % 2 == 0
                        ? 0.5D
                        : 0.0D;

        Direction right =
                face.getClockWise();

        /*
         * 稍微离墙一点。
         */
        Vec3 spawn =
                Vec3.atCenterOf(center)
                        .add(
                                right.getStepX()
                                        * horizontalOffset,
                                verticalOffset,
                                right.getStepZ()
                                        * horizontalOffset
                        )
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

        /*
         * 消耗物品。
         */
        if (context.getPlayer() != null
                && !context.getPlayer().isCreative()) {

            context.getItemInHand().shrink(1);
        }

        return InteractionResult.SUCCESS;
    }

    /*
     * ========================================
     * 获取物品中的 Variant
     * ========================================
     */

    private ResourceLocation getPaintingVariant(ItemStack stack) {

        CustomData customData =
                stack.get(DataComponents.CUSTOM_DATA);

        if (customData != null) {

            CompoundTag tag =
                    customData.copyTag();

            String value =
                    tag.getString("PaintingVariant");

            if (!value.isEmpty()) {

                try {

                    ResourceLocation id =
                            ResourceLocation.parse(value);

                    if (GhostPaintingVariants.contains(id)) {
                        return id;
                    }

                } catch (Exception ignored) {

                }
            }
        }

        return GhostPaintingVariants.LANDSCAPE;
    }

    /*
     * ========================================
     * 判断 Variant 是否注册
     * ========================================
     */

    private boolean isRegisteredVariant(
            ResourceLocation id
    ) {

        /*
         * GhostPaintingVariants.get()
         * 在找不到的时候会返回 LANDSCAPE，
         * 所以这里不能直接拿 get() 判断。
         *
         * 目前你的 Variants 类没有暴露 contains，
         * 因此通过返回值判断 ID 是否一致。
         */

        GhostPaintingVariant variant =
                GhostPaintingVariants.get(id);

        GhostPaintingVariant fallback =
                GhostPaintingVariants.get(
                        GhostPaintingVariants.LANDSCAPE
                );

        /*
         * 如果 ID 本身就是 LANDSCAPE，
         * 或者 get() 返回的 Variant 不是 fallback，
         * 就认为存在。
         *
         * 这个判断方式要求不同 Variant 的对象实例不同。
         */
        return id.equals(
                GhostPaintingVariants.LANDSCAPE
        ) || variant != fallback;
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

        int width =
                variant.width();

        int height =
                variant.height();

        /*
         * ========================================
         * 计算画覆盖的方块范围
         * ========================================
         *
         * 这种写法同时支持：
         *
         * 13×7
         * 3×4
         * 以后 2×2
         * 以后 4×6
         * 等偶数尺寸。
         */

        int minX =
                -((width - 1) / 2);

        int maxX =
                width / 2;

        int minY =
                -((height - 1) / 2);

        int maxY =
                height / 2;

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
         * ========================================
         * 检查墙面
         * ========================================
         */

        for (int x = minX; x <= maxX; x++) {

            for (int y = minY; y <= maxY; y++) {

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
                 * 墙必须完整。
                 */
                BlockState supportState =
                        level.getBlockState(
                                support
                        );

                if (!supportState.isSolidRender(
                        level,
                        support
                )) {

                    return PlacementResult.WALL_TOO_SMALL;
                }

                /*
                 * 画前方必须可以被替换。
                 */
                if (!level.getBlockState(
                        paintingPos
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

        double halfW =
                width / 2.0D;

        double halfH =
                height / 2.0D;

        double thickness =
                0.15D;

        /*
         * 实际几何中心。
         */
        Vec3 c =
                Vec3.atCenterOf(center)
                        .add(
                                right.getStepX()
                                        * horizontalOffset(width),
                                verticalOffset(height),
                                right.getStepZ()
                                        * horizontalOffset(width)
                        );

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

        for (Entity entity :
                level.getEntities(null, box)) {

            /*
             * 已经存在的鬼画不算。
             */
            if (entity instanceof GhostPaintingEntity) {
                continue;
            }

            /*
             * 玩家自己不阻止放置。
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
     * 尺寸偏移
     * ========================================
     */

    private static double horizontalOffset(
            int width
    ) {

        return width % 2 == 0
                ? 0.5D
                : 0.0D;
    }

    private static double verticalOffset(
            int height
    ) {

        return height % 2 == 0
                ? 0.5D
                : 0.0D;
    }

    /*
     * ========================================
     * 提示
     * ========================================
     */

    private void sendPlacementMessage(
            UseOnContext context,
            PlacementResult result,
            GhostPaintingVariant variant
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
                                    "§c墙面不足 "
                                            + variant.width()
                                            + "×"
                                            + variant.height()
                                            + "。"
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

        context.getPlayer()
                .displayClientMessage(
                        message,
                        true
                );
    }
}