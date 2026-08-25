package com.qidate.qisplan2.client.renderer;

import com.qidate.qisplan2.entity.KnockingGhost;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class KnockingGhostRenderer
        extends HumanoidMobRenderer<
        KnockingGhost,
        HumanoidModel<KnockingGhost>
        > {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "qisplan2",
                    "textures/entity/invisible_ghost.png"
            );

    public KnockingGhostRenderer(
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
            KnockingGhost entity
    ) {
        return TEXTURE;
    }
}