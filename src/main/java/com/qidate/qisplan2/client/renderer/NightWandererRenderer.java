package com.qidate.qisplan2.client.renderer;

import com.qidate.qisplan2.client.model.NightWandererModel;
import com.qidate.qisplan2.entity.NightWanderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class NightWandererRenderer
        extends HumanoidMobRenderer<
        NightWanderer,
        NightWandererModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "qisplan2",
                    "textures/entity/night_wanderer.png"
            );

    public NightWandererRenderer(
            EntityRendererProvider.Context context
    ) {
        super(
                context,
                new NightWandererModel(
                        context.bakeLayer(
                                NightWandererModel.LAYER
                        )
                ),
                0.5F
        );
    }

    @Override
    public ResourceLocation getTextureLocation(
            NightWanderer entity
    ) {
        return TEXTURE;
    }
}