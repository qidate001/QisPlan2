package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class GhostPaintingVariants {

    private GhostPaintingVariants() {}

    private static final Map<ResourceLocation, GhostPaintingVariant> VARIANTS =
            new HashMap<>();

    public static final ResourceLocation LANDSCAPE =
            register(
                    "landscape",
                    13,
                    7,
                    "entity/ghost_painting/landscape"
            );

    public static final ResourceLocation OROKIN =
            register(
                    "orokin",
                    3,
                    4,
                    "entity/ghost_painting/orokin"
            );

    private static ResourceLocation register(
            String id,
            int width,
            int height,
            String texture
    ) {

        ResourceLocation key =
                ResourceLocation.fromNamespaceAndPath(
                        QisPlan2.MODID,
                        id
                );

        VARIANTS.put(
                key,
                new GhostPaintingVariant(
                        ResourceLocation.fromNamespaceAndPath(
                                QisPlan2.MODID,
                                "textures/" + texture + ".png"
                        ),
                        width,
                        height
                )
        );

        return key;
    }

    public static GhostPaintingVariant get(
            ResourceLocation id
    ) {

        return VARIANTS.getOrDefault(
                id,
                VARIANTS.get(LANDSCAPE)
        );
    }

    public static boolean contains(ResourceLocation id) {
        return VARIANTS.containsKey(id);
    }
}