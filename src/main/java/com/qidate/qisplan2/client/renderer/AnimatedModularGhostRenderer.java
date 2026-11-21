package com.qidate.qisplan2.client.renderer;

import com.qidate.qisplan2.client.model.AnimatedModularGhostModel;
import com.qidate.qisplan2.entity.AnimatedModularGhostEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 独特外观宿主共用的渲染器；所有灵异效果仍由模块与已有框架负责。 */
public final class AnimatedModularGhostRenderer<T extends AnimatedModularGhostEntity> extends GeoEntityRenderer<T> {
    /** 使用实体类型对应的模型、材质和动画，阴影不参与碰撞或能力判定。 */
    public AnimatedModularGhostRenderer(EntityRendererProvider.Context context) {
        super(context,new AnimatedModularGhostModel<>());
        this.shadowRadius=0.35F;
        addRenderLayer(new GhostGeoAttachmentLayer<>(this));
    }
}
