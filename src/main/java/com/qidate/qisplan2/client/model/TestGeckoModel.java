package com.qidate.qisplan2.client.model;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.entity.TestGeckoEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * ============================================================
 * QisPlan2 - GeckoLib 测试实体模型
 * ============================================================
 *
 * 负责告诉 GeckoLib：
 *
 * 1. 模型 JSON 在哪里
 * 2. 纹理 PNG 在哪里
 * 3. 动画 JSON 在哪里
 *
 * 使用通用尸体厉鬼模型、贴图及基础动画。
 */
public class TestGeckoModel extends GeoModel<TestGeckoEntity> {

    /**
     * GeckoLib 模型文件。
     */
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "geo/corpse_ghost.geo.json"
            );

    /**
     * 实体纹理。
     */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "textures/entity/corpse_ghost.png"
            );

    /**
     * 动画文件。
     */
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "animations/entity/corpse_ghost.animation.json"
            );

    @Override
    public ResourceLocation getModelResource(
            TestGeckoEntity animatable
    ) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(
            TestGeckoEntity animatable
    ) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(
            TestGeckoEntity animatable
    ) {
        return ANIMATION;
    }
}