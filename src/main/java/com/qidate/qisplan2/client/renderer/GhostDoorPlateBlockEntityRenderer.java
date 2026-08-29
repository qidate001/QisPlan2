package com.qidate.qisplan2.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.qidate.qisplan2.block.entity.GhostDoorPlateBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.mojang.math.Axis;

/**
 * 鬼门牌门牌号渲染器。
 *
 * 将门牌号直接显示在鬼门牌模型表面。
 */
public class GhostDoorPlateBlockEntityRenderer
        implements BlockEntityRenderer<GhostDoorPlateBlockEntity> {

    private final Font font;

    public GhostDoorPlateBlockEntityRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        this.font =
                Minecraft.getInstance().font;
    }

    @Override
    public void render(
            GhostDoorPlateBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        Font font =
                Minecraft.getInstance().font;

        String text =
                String.valueOf(
                        blockEntity.getNumber()
                );

        Direction facing =
                blockEntity.getBlockState().getValue(
                        BlockStateProperties.HORIZONTAL_FACING
                );

        poseStack.pushPose();

        /*
         * ========================================================
         * 1. 移动到方块中心
         *
         * 非常重要！
         *
         * 后面的旋转全部围绕方块中心进行。
         * ========================================================
         */
        poseStack.translate(
                0.5D,
                0.5D,
                0.5D
        );

        /*
         * ========================================================
         * 2. 根据门牌朝向旋转
         *
         * 门牌模型统一以 SOUTH 方向作为基础方向。
         * ========================================================
         */
        switch (facing) {

            case SOUTH -> {
                // 基准方向
            }

            case NORTH ->
                    poseStack.mulPose(
                            Axis.YP.rotationDegrees(180)
                    );

            case EAST ->
                    poseStack.mulPose(
                            Axis.YP.rotationDegrees(270)
                    );

            case WEST ->
                    poseStack.mulPose(
                            Axis.YP.rotationDegrees(90)
                    );
        }

        /*
         * ========================================================
         * 3. 移动到门牌表面
         *
         * 这里的坐标已经是“门牌自己的局部坐标”。
         *
         * X = 水平
         * Y = 上下
         * Z = 朝门牌表面的方向
         * ========================================================
         */
        switch (facing) {

            case SOUTH, NORTH ->
                    poseStack.translate(
                            0.015D,
                            0.15D,
                            -0.425D
                    );

            case EAST, WEST ->
                    poseStack.translate(
                            -0.015D,
                            0.15D,
                            0.425D
                    );
        }

        /*
         * ========================================================
         * 4. Font 坐标系翻转
         * ========================================================
         */
        poseStack.mulPose(
                Axis.XP.rotationDegrees(180)
        );

        /*
         * EAST / WEST 的字体正反面需要翻转。
         */
        if (facing == Direction.EAST
                || facing == Direction.WEST) {

            poseStack.mulPose(
                    Axis.YP.rotationDegrees(180)
            );
        }

        /*
         * ========================================================
         * 5. 字体大小
         * ========================================================
         */
        float scale = 0.035F;

        poseStack.scale(
                scale,
                scale,
                scale
        );

        /*
         * ========================================================
         * 6. 水平居中
         * ========================================================
         */
        float width =
                font.width(text);

        /*
         * ========================================================
         * 7. 绘制门牌号
         * ========================================================
         */
        font.drawInBatch(
                text,
                -width / 2.0F,
                -4.0F,
                0xFFFFFFFF,
                false,
                poseStack.last().pose(),
                buffer,
                Font.DisplayMode.NORMAL,
                0,
                packedLight
        );

        poseStack.popPose();
    }
}