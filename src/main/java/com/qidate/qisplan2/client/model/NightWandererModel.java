package com.qidate.qisplan2.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import com.qidate.qisplan2.entity.NightWanderer;

public class NightWandererModel
        extends HumanoidModel<NightWanderer> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(
                    ResourceLocation.fromNamespaceAndPath(
                            "qisplan2",
                            "night_wanderer"
                    ),
                    "main"
            );

    public NightWandererModel(ModelPart root) {
        super(root);
    }
}