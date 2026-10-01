package com.qidate.qisplan2.ghost.tombstone.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.qidate.qisplan2.block.GhostTombstoneBlock;
import com.qidate.qisplan2.block.entity.GhostTombstoneBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import com.mojang.math.Axis;

public class GhostTombstoneBlockEntityRenderer
        implements BlockEntityRenderer<GhostTombstoneBlockEntity> {

    private final Font font;

    public GhostTombstoneBlockEntityRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        font = context.getFont();
    }

    @Override
    public void render(
            GhostTombstoneBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {

        /*
         * 只有下半部分绘制文字。
         */
        if (blockEntity.getBlockState()
                .getValue(GhostTombstoneBlock.PART)
                != GhostTombstoneBlock.Part.LOWER) {

            return;
        }

        String text =
                blockEntity.getInscription();

        if (text.isEmpty()) {
            return;
        }

        Direction facing =
                blockEntity.getBlockState()
                        .getValue(GhostTombstoneBlock.FACING);

        poseStack.pushPose();

        /*
         * 墓碑模型的方向旋转是以方块中心
         * (0.5, 0, 0.5) 为基准。
         *
         * 这里让文字也使用同一个旋转中心，
         * 从而保证四个方向的偏移完全一致。
         */
        poseStack.translate(
                0.5,
                0.0,
                0.5
        );

        /*
         * 根据墓碑朝向旋转文字。
         *
         * 注意：
         * BlockState 的模型旋转方向与文字坐标系的视觉朝向相反，
         * 因此 EAST / WEST 使用反向角度。
         */
        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        switch (facing) {
                            case NORTH -> 0;
                            case EAST -> -90;
                            case SOUTH -> 180;
                            case WEST -> 90;
                            default -> 0;
                        }
                )
        );

        /*
         * SOUTH 方向当前的位置：
         *
         * X = 0.5
         * Y = 1.30
         * Z = 0.726
         *
         * 转换成相对于方块中心的局部坐标。
         *
         * SOUTH 旋转 180° 后，
         * 局部 Z = -0.226 会变成世界 Z = 0.726。
         *
         * 因此保持 -0.226，
         * 可以保证 SOUTH 当前的视觉位置不变。
         */
        poseStack.translate(
                0.0,
                1.30,
                -0.226
        );

        /*
         * 面向玩家。
         */
        poseStack.scale(
                0.035F,
                -0.035F,
                0.035F
        );

        var glyphs =
                GhostTombstoneTextLayout.layout(
                        text,
                        font
                );

        for (var glyph : glyphs) {

            poseStack.pushPose();

            poseStack.translate(
                    glyph.x(),
                    glyph.y(),
                    0
            );

            poseStack.mulPose(
                    Axis.ZP.rotationDegrees(
                            glyph.rotation()
                    )
            );

            poseStack.scale(
                    glyph.scale(),
                    glyph.scale(),
                    glyph.scale()
            );

            int width =
                    font.width(glyph.text());

            /*
             * 阴影。
             *
             * 向墓碑内部偏移一点，
             * 让阴影和正文字不处于同一个深度。
             */
            poseStack.pushPose();

            poseStack.translate(
                    0.0,
                    0.0,
                    -0.01
            );

            font.drawInBatch(
                    glyph.text(),
                    -width / 2F + 0.45F,
                    0.3F,
                    0xFF5A5A5A,
                    false,
                    poseStack.last().pose(),
                    buffer,
                    Font.DisplayMode.NORMAL,
                    0,
                    packedLight
            );

            poseStack.popPose();

            /*
             * 正文字。
             */
            font.drawInBatch(
                    glyph.text(),
                    -width / 2F + 0.15F,
                    0,
                    0xFF1E1E1E,
                    false,
                    poseStack.last().pose(),
                    buffer,
                    Font.DisplayMode.NORMAL,
                    0,
                    packedLight
            );

            poseStack.popPose();
        }

        poseStack.popPose();
    }
}