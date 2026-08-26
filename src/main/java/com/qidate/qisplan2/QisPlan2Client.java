package com.qidate.qisplan2;

import com.qidate.qisplan2.client.BlackRainParticle;
import com.qidate.qisplan2.client.DoorGhostMarkClient;
import com.qidate.qisplan2.client.GhostUmbrellaDomainClient;
import com.qidate.qisplan2.client.model.NightWandererModel;
import com.qidate.qisplan2.client.renderer.ClosingGhostRenderer;
import com.qidate.qisplan2.client.renderer.KnockingGhostRenderer;
import com.qidate.qisplan2.client.renderer.NightWandererRenderer;
import com.qidate.qisplan2.client.renderer.OpeningGhostRenderer;
import com.qidate.qisplan2.event.DeathCurseHudOverlay;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

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

        // 鬼黑雨
        modEventBus.addListener(
                QisPlan2Client::registerParticleProviders
        );

        /*
         * 鬼雨领域客户端逻辑
         */
        NeoForge.EVENT_BUS.register(
                GhostUmbrellaDomainClient.class
        );
    }

    private static void registerParticleProviders(
            RegisterParticleProvidersEvent event
    ) {
        event.registerSpriteSet(
                QisPlan2.BLACK_RAIN.get(),
                BlackRainParticle.Provider::new
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
     * 注册 Renderer
     */
    /**
     * 注册 Renderer
     */
    private static void registerEntityRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerEntityRenderer(
                QisPlan2.NIGHT_WANDERER.get(),
                NightWandererRenderer::new
        );

        event.registerEntityRenderer(
                QisPlan2.KNOCKING_GHOST.get(),
                KnockingGhostRenderer::new
        );

        event.registerEntityRenderer(
                QisPlan2.OPENING_GHOST.get(),
                OpeningGhostRenderer::new
        );

        event.registerEntityRenderer(
                QisPlan2.CLOSING_GHOST.get(),
                ClosingGhostRenderer::new
        );
    }
}