package com.qidate.qisplan2.client;

import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomain;
import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomainManager;
import com.qidate.qisplan2.client.renderer.GhostRainRenderer;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.implementation.umbrella.GhostRainEffect;
import net.neoforged.neoforge.common.NeoForge;
import com.qidate.qisplan2.ghost.domain.shape.CylinderDomainShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

/** 鬼雨专属客户端适配器。雨柱与雨声细节属于本效果，不属于通用鬼域框架。 */
public final class GhostRainClient {
    private static boolean installed;
    /** 仅在模块声明鬼雨时安装事件；多个模块共享实现，不重复注册监听器。 */
    public static void install(ResourceLocation moduleId, GhostRainEffect definition) {
        if(installed) return;
        installed=true;
        NeoForge.EVENT_BUS.register(GhostRainClient.class);
    }


    /** 天气表现由模块的鬼域声明选择，不认识具体厉鬼名称。 */
    private static boolean hasRain(ClientGhostDomain domain) {
        var module = GhostModuleRegistry.get(domain.getDomainType());
        return module != null && module.domainDefinition().map(definition -> definition.effects().contains(GhostRainEffect.INSTANCE)).orElse(false);
    }

    private static final List<GhostRainSource> RAIN_SOURCES =
            new ArrayList<>();

    private static boolean insideDomain = false;

    private static int rainSoundTime = 0;

    private GhostRainClient() {
    }

    public static boolean isInsideDomain() {
        return insideDomain;
    }

    public static boolean isPositionInsideDomain(
            double x,
            double y,
            double z
    ) {

        ClientGhostDomain effectiveDomain =
                getEffectiveDomain(
                        x,
                        y,
                        z
                );

        if (effectiveDomain == null) {
            return false;
        }

        return hasRain(effectiveDomain);
    }

    /**
     * 获取指定位置当前实际生效的客户端鬼域。
     *
     * 优先级规则与服务器端 GhostDomainManager 保持一致：
     *
     * 1. 层数高的优先
     * 2. 层数相同，强度高的优先
     */
    private static ClientGhostDomain getEffectiveDomain(
            double x,
            double y,
            double z
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return null;
        }

        ResourceLocation currentDimension =
                minecraft.level.dimension().location();

        ClientGhostDomain effectiveDomain = null;

        for (ClientGhostDomain domain :
                ClientGhostDomainManager.getDomains()) {

            /*
             * 只处理当前维度。
             */
            if (!currentDimension.equals(
                    domain.getDimension())) {
                continue;
            }

            /*
             * 这个鬼域不包含当前位置。
             */
            if (!domain.contains(x, y, z)) {
                continue;
            }

            /*
             * 第一个符合条件的鬼域。
             */
            if (effectiveDomain == null) {
                effectiveDomain = domain;
                continue;
            }

            /*
             * 使用与服务器完全相同的优先级规则。
             */
            if (domain.getLayer()
                    > effectiveDomain.getLayer()) {

                effectiveDomain = domain;

                continue;
            }

            if (domain.getLayer()
                    == effectiveDomain.getLayer()
                    && domain.getStrength()
                    > effectiveDomain.getStrength()) {

                effectiveDomain = domain;
            }
        }

        return effectiveDomain;
    }

    @SubscribeEvent
    public static void onClientTick(
            ClientTickEvent.Post event
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null) {

            RAIN_SOURCES.clear();
            insideDomain = false;
            rainSoundTime = 0;
            return;
        }

        ClientLevel level =
                minecraft.level;

        /*
         * 从客户端鬼域管理器读取
         * 当前维度声明了降雨表现的所有模块鬼域。
         */
        RAIN_SOURCES.clear();

        for (ClientGhostDomain domain :
                ClientGhostDomainManager.getDomains()) {

            if (!hasRain(domain)) {
                continue;
            }

            if (!domain.getDimension().equals(
                    level.dimension().location())) {
                continue;
            }

            if (domain.getShape()
                    instanceof CylinderDomainShape cylinder) {

                RAIN_SOURCES.add(
                        new GhostRainSource(
                                domain.getX(),
                                domain.getZ(),
                                cylinder.getRadius()
                        )
                );
            }
        }

        insideDomain =
                isPositionInsideDomain(
                        minecraft.player.getX(),
                        minecraft.player.getY(),
                        minecraft.player.getZ()
                );

        tickRainSound(minecraft);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(
            RenderLevelStageEvent event
    ) {

        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null) {
            return;
        }

        /*
         * 维度切换过程中，RAIN_SOURCES 可能还残留
         * 上一个维度的数据。
         *
         * 此时不能继续进行鬼雨渲染。
         */
        if (!hasCurrentDimensionDomain(minecraft.level)) {
            RAIN_SOURCES.clear();
            insideDomain = false;
            return;
        }

        if (RAIN_SOURCES.isEmpty()) {
            return;
        }

        if (!insideDomain) {
            return;
        }

        GhostRainRenderer.render(
                event,
                RAIN_SOURCES
        );
    }

    private static boolean hasCurrentDimensionDomain(
            ClientLevel level
    ) {

        ResourceLocation currentDimension =
                level.dimension().location();

        for (ClientGhostDomain domain :
                ClientGhostDomainManager.getDomains()) {

            if (!hasRain(domain)) {
                continue;
            }

            if (!currentDimension.equals(
                    domain.getDimension())) {
                continue;
            }

            return true;
        }

        return false;
    }

    private static void tickRainSound(
            Minecraft minecraft
    ) {

        if (!insideDomain) {
            rainSoundTime = 0;
            return;
        }

        if (rainSoundTime > 0) {
            rainSoundTime--;
            return;
        }

        if (minecraft.player == null
                || minecraft.level == null) {
            return;
        }

        minecraft.level.playLocalSound(
                minecraft.player.getX(),
                minecraft.player.getY(),
                minecraft.player.getZ(),
                SoundEvents.WEATHER_RAIN,
                SoundSource.WEATHER,
                0.30F,
                1.0F,
                false
        );

        rainSoundTime =
                12 + minecraft.level.random.nextInt(12);
    }
}