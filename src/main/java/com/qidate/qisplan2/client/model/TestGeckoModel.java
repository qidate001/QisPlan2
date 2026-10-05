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
 * 当前测试阶段暂时没有动画，
 * 因此动画资源先返回一个不存在的测试路径。
 */
public class TestGeckoModel extends GeoModel<TestGeckoEntity> {

    /**
     * GeckoLib 模型文件。
     */
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "geo/test_gecko.geo.json"
            );

    /**
     * 实体纹理。
     *
     * 目前如果还没有 PNG，
     * 下一步我们再处理纹理。
     */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "textures/entity/test_gecko.png"
            );

    /**
     * 动画文件。
     *
     * 第一阶段没有动画，因此这里只是提供一个路径。
     */
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "animations/entity/test_gecko.animation.json"
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