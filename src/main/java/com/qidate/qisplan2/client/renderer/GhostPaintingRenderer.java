package com.qidate.qisplan2.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.entity.GhostPaintingEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class GhostPaintingRenderer
        extends EntityRenderer<GhostPaintingEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "textures/entity/ghost_painting.png"
            );


    public GhostPaintingRenderer(
            EntityRendererProvider.Context context
    ) {

        super(context);

        shadowRadius =
                0.0F;
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
         * 根据墙面方向旋转
         * ========================================
         */

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        180.0F
                                - entity.getYRot()
                )
        );


        /*
         * ========================================
         * 画面中心
         * ========================================
         */

        float halfWidth =
                GhostPaintingEntity.WIDTH
                        / 2.0F;

        float halfHeight =
                GhostPaintingEntity.HEIGHT
                        / 2.0F;


        /*
         * 稍微往墙外移动。
         *
         * 防止 Z-Fighting。
         */
        float z =
                -0.01F;


        VertexConsumer consumer =
                buffer.getBuffer(
                        RenderType.entityCutoutNoCull(
                                TEXTURE
                        )
                );


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
                .setColor(255, 255, 255, 255)
                .setUv(0.0F, 1.0F)
                .setUv1(0, 10)
                .setLight(packedLight)
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
                .setColor(255, 255, 255, 255)
                .setUv(1.0F, 1.0F)
                .setUv1(0, 10)
                .setLight(packedLight)
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
                .setColor(255, 255, 255, 255)
                .setUv(1.0F, 0.0F)
                .setUv1(0, 10)
                .setLight(packedLight)
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
                .setColor(255, 255, 255, 255)
                .setUv(0.0F, 0.0F)
                .setUv1(0, 10)
                .setLight(packedLight)
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

        return TEXTURE;
    }
}