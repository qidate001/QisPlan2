package com.qidate.qisplan2.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.entity.GhostPaintingEntity;
import com.qidate.qisplan2.entity.GhostPaintingVariant;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public class GhostPaintingRenderer
        extends EntityRenderer<GhostPaintingEntity> {

    public GhostPaintingRenderer(
            EntityRendererProvider.Context context
    ) {

        super(context);

        shadowRadius = 0.0F;
    }

    @Override
    public void render(
            GhostPaintingEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {

        poseStack.pushPose();

        /*
         * ========================================
         * 当前鬼画 Variant
         * ========================================
         */

        GhostPaintingVariant variant =
                entity.getVariant();

        float halfWidth =
                variant.width() / 2.0F;

        float halfHeight =
                variant.height() / 2.0F;

        /*
         * ========================================
         * 根据墙面方向旋转
         * ========================================
         */

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        entity.getYRot()
                )
        );

        /*
         * ========================================
         * 稍微往墙外移动
         *
         * 防止 Z-Fighting
         * ========================================
         */

        float z;

        if (entity.getDirection().getAxis()
                == Direction.Axis.Z) {

            z = 0.01F;

        } else {

            z = -0.01F;
        }

        /*
         * ========================================
         * 使用当前 Variant 的纹理
         * ========================================
         */

        VertexConsumer consumer =
                buffer.getBuffer(
                        RenderType.entityCutoutNoCull(
                                variant.texture()
                        )
                );

        /*
         * ========================================
         * 东西方向需要翻转 U
         * ========================================
         */

        boolean flipU =
                entity.getDirection().getAxis()
                        == Direction.Axis.X;

        float uLeft =
                flipU ? 1.0F : 0.0F;

        float uRight =
                flipU ? 0.0F : 1.0F;

        /*
         * ========================================
         * 正面
         * ========================================
         */

        consumer.addVertex(
                        poseStack.last().pose(),
                        -halfWidth,
                        -halfHeight,
                        z
                )
                .setColor(
                        255,
                        255,
                        255,
                        255
                )
                .setUv(
                        uLeft,
                        1.0F
                )
                .setUv1(
                        0,
                        10
                )
                .setLight(
                        packedLight
                )
                .setNormal(
                        poseStack.last(),
                        0.0F,
                        0.0F,
                        1.0F
                );

        consumer.addVertex(
                        poseStack.last().pose(),
                        halfWidth,
                        -halfHeight,
                        z
                )
                .setColor(
                        255,
                        255,
                        255,
                        255
                )
                .setUv(
                        uRight,
                        1.0F
                )
                .setUv1(
                        0,
                        10
                )
                .setLight(
                        packedLight
                )
                .setNormal(
                        poseStack.last(),
                        0.0F,
                        0.0F,
                        1.0F
                );

        consumer.addVertex(
                        poseStack.last().pose(),
                        halfWidth,
                        halfHeight,
                        z
                )
                .setColor(
                        255,
                        255,
                        255,
                        255
                )
                .setUv(
                        uRight,
                        0.0F
                )
                .setUv1(
                        0,
                        10
                )
                .setLight(
                        packedLight
                )
                .setNormal(
                        poseStack.last(),
                        0.0F,
                        0.0F,
                        1.0F
                );

        consumer.addVertex(
                        poseStack.last().pose(),
                        -halfWidth,
                        halfHeight,
                        z
                )
                .setColor(
                        255,
                        255,
                        255,
                        255
                )
                .setUv(
                        uLeft,
                        0.0F
                )
                .setUv1(
                        0,
                        10
                )
                .setLight(
                        packedLight
                )
                .setNormal(
                        poseStack.last(),
                        0.0F,
                        0.0F,
                        1.0F
                );

        poseStack.popPose();

        super.render(
                entity,
                entityYaw,
                partialTick,
                poseStack,
                buffer,
                packedLight
        );
    }

    @Override
    public ResourceLocation getTextureLocation(
            GhostPaintingEntity entity
    ) {
        return entity.getVariant().texture();
    }
}