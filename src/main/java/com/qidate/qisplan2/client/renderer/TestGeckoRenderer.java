package com.qidate.qisplan2.client.renderer;

import com.qidate.qisplan2.client.model.TestGeckoModel;
import com.qidate.qisplan2.entity.TestGeckoEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * ============================================================
 * QisPlan2 - GeckoLib 测试实体 Renderer
 * ============================================================
 *
 * 当前仅负责：
 *
 * TestGeckoEntity
 *        ↓
 * TestGeckoModel
 *        ↓
 * GeckoLib GeoEntityRenderer
 *
 * 暂时不添加：
 * - RenderLayer
 * - 特殊缩放
 * - 特殊姿态
 * - 特殊 Shader
 * - 灵异效果
 */
public class TestGeckoRenderer
        extends GeoEntityRenderer<TestGeckoEntity> {

    public TestGeckoRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context, new TestGeckoModel());
    }
}