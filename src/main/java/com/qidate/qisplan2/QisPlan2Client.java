package com.qidate.qisplan2;

import com.mojang.blaze3d.platform.InputConstants;
import com.qidate.qisplan2.client.BlackRainParticle;
import com.qidate.qisplan2.client.GhostUmbrellaClient;
import com.qidate.qisplan2.client.screen.GhostStoveScreen;
import com.qidate.qisplan2.client.GhostUmbrellaDomainClient;
import com.qidate.qisplan2.client.model.NightWandererModel;
import com.qidate.qisplan2.client.renderer.*;
import com.qidate.qisplan2.core.*;
import com.qidate.qisplan2.event.DeathCurseHudOverlay;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;

import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = QisPlan2.MODID, dist = Dist.CLIENT)
public class QisPlan2Client {

    public static final KeyMapping OPEN_POSSESSION_SCREEN =
            new KeyMapping(
                    "key.qisplan2.possession_screen",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_H,
                    "key.categories.qisplan2"
            );

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

        // 注册 必死诅咒之剑 HUD
        modEventBus.addListener(
                DeathCurseHudOverlay::registerDeathCurseLayer
        );

        // 注册 GUI
        modEventBus.addListener(
                QisPlan2Client::registerMenuScreens
        );

        // 注册 Layer
        modEventBus.addListener(
                QisPlan2Client::registerLayerDefinitions
        );

        // 注册 Renderer
        modEventBus.addListener(
                QisPlan2Client::registerEntityRenderers
        );

        // 鬼湖水
        modEventBus.addListener(
                QisPlan2Client::registerFluidClientExtensions
        );

        // 鬼雨伞
        modEventBus.register(
                GhostUmbrellaClient.class
        );

        // 鬼黑雨
        modEventBus.addListener(
                QisPlan2Client::registerParticleProviders
        );

        // 注册按键
        modEventBus.addListener(
                QisPlan2Client::registerKeyMappings
        );

        // 鬼雨领域客户端逻辑
        NeoForge.EVENT_BUS.register(
                GhostUmbrellaDomainClient.class
        );

        NeoForge.EVENT_BUS.addListener(QisPlan2Client::clientTick);
    }

    private static void registerParticleProviders(
            RegisterParticleProvidersEvent event
    ) {
        event.registerSpriteSet(
                ModParticles.BLACK_RAIN.get(),
                BlackRainParticle.Provider::new
        );
    }

    private static void registerKeyMappings(
            RegisterKeyMappingsEvent event
    ) {
        event.register(OPEN_POSSESSION_SCREEN);
    }

    /**
     * 注册夜游鬼模型 Layer
     */
    private static void registerLayerDefinitions(
            EntityRenderersEvent.RegisterLayerDefinitions event
    ) {
//        QisPlan2.LOGGER.info(
//                "[QisPlan2] 注册 NightWanderer Model Layer"
//        );

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

    private static void registerFluidClientExtensions(
            RegisterClientExtensionsEvent event
    ) {

        /*
         * ============================================================
         * 鬼湖水
         * ============================================================
         */

        event.registerFluidType(
                new IClientFluidTypeExtensions() {

                    private static final ResourceLocation STILL =
                            ResourceLocation.fromNamespaceAndPath(
                                    QisPlan2.MODID,
                                    "block/ghost_lake_water_still"
                            );

                    private static final ResourceLocation FLOWING =
                            ResourceLocation.fromNamespaceAndPath(
                                    QisPlan2.MODID,
                                    "block/ghost_lake_water_flowing"
                            );

                    @Override
                    public ResourceLocation getStillTexture() {
                        return STILL;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return FLOWING;
                    }

                    @Override
                    public int getTintColor() {
                        return 0xFFFFFFFF;
                    }
                },
                ModFluids.GHOST_LAKE_WATER_TYPE.get()
        );


        /*
         * ============================================================
         * 鬼血
         * ============================================================
         */

        event.registerFluidType(
                new IClientFluidTypeExtensions() {

                    private static final ResourceLocation STILL =
                            ResourceLocation.fromNamespaceAndPath(
                                    QisPlan2.MODID,
                                    "block/ghost_blood_still"
                            );

                    private static final ResourceLocation FLOWING =
                            ResourceLocation.fromNamespaceAndPath(
                                    QisPlan2.MODID,
                                    "block/ghost_blood_flowing"
                            );

                    @Override
                    public ResourceLocation getStillTexture() {
                        return STILL;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return FLOWING;
                    }

                    @Override
                    public int getTintColor() {
                        return 0xFFFFFFFF;
                    }
                },
                ModFluids.GHOST_BLOOD_TYPE.get()
        );
    }

    /**
     * 注册 Renderer
     */
    private static void registerEntityRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        // 夜游鬼
        event.registerEntityRenderer(
                ModEntities.NIGHT_WANDERER.get(),
                NightWandererRenderer::new
        );

        // 敲门鬼
        event.registerEntityRenderer(
                ModEntities.KNOCKING_GHOST.get(),
                KnockingGhostRenderer::new
        );

        // 开门鬼
        event.registerEntityRenderer(
                ModEntities.OPENING_GHOST.get(),
                OpeningGhostRenderer::new
        );

        // 关门鬼
        event.registerEntityRenderer(
                ModEntities.CLOSING_GHOST.get(),
                ClosingGhostRenderer::new
        );

        // 不可视之鬼
        event.registerEntityRenderer(
                ModEntities.INVISIBLE_GHOST.get(),
                InvisibleGhostRenderer::new
        );

        // 喊人鬼
        event.registerEntityRenderer(
                ModEntities.CALLING_GHOST.get(),
                CallingGhostRenderer::new
        );

        // 鬼画
        event.registerEntityRenderer(
                ModEntities.GHOST_PAINTING.get(),
                GhostPaintingRenderer::new
        );

        BlockEntityRenderers.register(
                ModBlocks.GHOST_DOOR_PLATE_BLOCK_ENTITY.get(),
                GhostDoorPlateBlockEntityRenderer::new
        );
    }


    /**
     * 注册GUI
     */
    public static void registerMenuScreens(
            RegisterMenuScreensEvent event
    ) {
        event.register(
                ModMenus.GHOST_STOVE_MENU.get(),
                GhostStoveScreen::new
        );
    }

    @SubscribeEvent
    public static void clientTick(
            ClientTickEvent.Post event
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        while (
                QisPlan2Client
                        .OPEN_POSSESSION_SCREEN
                        .consumeClick()
        ) {

            if (minecraft.screen == null) {

                minecraft.setScreen(
                        new com.qidate.qisplan2.client.gui.PossessionScreen()
                );
            }
        }
    }
}