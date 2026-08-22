package com.qidate.qisplan2.client.renderer;

import com.qidate.qisplan2.entity.InvisibleGhost;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class InvisibleGhostRenderer
        extends HumanoidMobRenderer<
        InvisibleGhost,
        HumanoidModel<InvisibleGhost>
        > {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "qisplan2",
                    "textures/entity/invisible_ghost.png"
            );

    public InvisibleGhostRenderer(
            EntityRendererProvider.Context context
    ) {
        super(
                context,
                new HumanoidModel<>(
                        context.bakeLayer(
                                ModelLayers.PLAYER
                        )
                ),
                0.5F
        );
    }

    @Override
    public ResourceLocation getTextureLocation(
            InvisibleGhost entity
    ) {
        return TEXTURE;
    }
}