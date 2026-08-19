package com.qidate.qisplan2;

import com.qidate.qisplan2.client.model.NightWandererModel;
import com.qidate.qisplan2.client.renderer.NightWandererRenderer;
import com.qidate.qisplan2.event.DeathCurseHudOverlay;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = QisPlan2.MODID, dist = Dist.CLIENT)
public class QisPlan2Client {

    public QisPlan2Client(
            IEventBus modEventBus,
            ModContainer modContainer
    ) {

        // 注册配置界面工厂
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (mc, parent) ->
                        new ConfigurationScreen(
                                modContainer,
                                parent
                        )
        );

        // 注册客户端 HUD
        modEventBus.addListener(
                DeathCurseHudOverlay::registerDeathCurseLayer
        );

        // 注册夜游鬼模型 Layer
        modEventBus.addListener(
                QisPlan2Client::registerLayerDefinitions
        );

        // 注册夜游鬼 Renderer
        modEventBus.addListener(
                QisPlan2Client::registerEntityRenderers
        );
    }

    /**
     * 注册夜游鬼模型 Layer
     */
    private static void registerLayerDefinitions(
            EntityRenderersEvent.RegisterLayerDefinitions event
    ) {
        QisPlan2.LOGGER.info(
                "[QisPlan2] 注册 NightWanderer Model Layer"
        );

        event.registerLayerDefinition(
                NightWandererModel.LAYER,
                () -> LayerDefinition.create(
                        HumanoidModel.createMesh(
                                CubeDeformation.NONE,
                                0.0F
                        ),
                        64,
                        64
                )
        );
    }

    /**
     * 注册夜游鬼 Renderer
     */
    private static void registerEntityRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        QisPlan2.LOGGER.info(
                "[QisPlan2] 注册 NightWanderer Renderer"
        );

        event.registerEntityRenderer(
                QisPlan2.NIGHT_WANDERER.get(),
                NightWandererRenderer::new
        );
    }
}