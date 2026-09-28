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
         * 墓碑正面中心。
         */
        poseStack.translate(
                0.5,
                1.30,
                0.726
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        switch (facing) {

                            case SOUTH -> 180;

                            case WEST -> 90;

                            case EAST -> -90;

                            default -> 0;
                        }
                )
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
                GhostTombstoneTextLayout.layout(text);

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
                    -width / 2F + 0.3F,
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
                    -width / 2F,
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