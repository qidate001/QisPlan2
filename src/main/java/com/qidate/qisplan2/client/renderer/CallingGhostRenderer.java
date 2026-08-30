package com.qidate.qisplan2.client.renderer;

import com.qidate.qisplan2.entity.CallingGhost;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class CallingGhostRenderer
        extends EntityRenderer<CallingGhost> {

    public CallingGhostRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context);
    }

    /**
     * 喊人鬼完全隐形。
     *
     * 故意什么都不渲染。
     */
    @Override
    public void render(
            CallingGhost entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        // 什么都不渲染
    }

    @Override
    public ResourceLocation getTextureLocation(
            CallingGhost entity
    ) {
        /*
         * EntityRenderer 要求提供纹理。
         *
         * 由于 render() 完全为空，
         * 这里的纹理实际上不会被绘制。
         */
        return ResourceLocation.fromNamespaceAndPath(
                "qisplan2",
                "textures/entity/invisible_ghost.png"
        );
    }
}