package com.qidate.qisplan2.client.renderer;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/** 只在客户端加载模型和渲染层，专用服务器不会加载 Minecraft 客户端类。 */
@EventBusSubscriber(modid = QisPlan2.MODID, value = Dist.CLIENT)
public final class GhostAttachmentRenderEvents {
    private GhostAttachmentRenderEvents() {}
    /** 附着模型由模块声明，渲染器不维护另一份厉鬼到模型的硬编码映射。 */
    @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event) {
        for (var module : GhostModuleRegistry.getAll())
            if (module.attachmentModel() != null) event.register(ModelResourceLocation.standalone(module.attachmentModel()));
    }
    /** 同时覆盖通用厉鬼实体、普通生物和两种玩家皮肤。 */
    @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers event) {
        for (var type : event.getEntityTypes()) install(event.getRenderer(type));
        for (var skin : event.getSkins()) install(event.getSkin(skin));
    }
    /** 泛型擦除仅限原版渲染注册边界，不侵入模块实现。 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void install(Object renderer) {
        if (renderer instanceof LivingEntityRenderer living) living.addLayer(new GhostAttachmentLayer(living));
    }
}
