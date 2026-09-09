package com.qidate.qisplan2.client;

import com.qidate.qisplan2.client.domain.ClientGhostDomain;
import com.qidate.qisplan2.client.domain.ClientGhostDomainManager;
import com.qidate.qisplan2.client.renderer.GhostRainRenderer;
import com.qidate.qisplan2.QisPlan2;
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

public final class GhostUmbrellaDomainClient {

    private static final ResourceLocation DOMAIN_TYPE =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "ghost_umbrella"
            );

    private static final List<GhostRainSource> RAIN_SOURCES =
            new ArrayList<>();

    private static boolean insideDomain = false;

    private static int rainSoundTime = 0;

    private GhostUmbrellaDomainClient() {
    }

    public static boolean isInsideDomain() {
        return insideDomain;
    }

    public static boolean isPositionInsideDomain(
            double x,
            double y,
            double z
    ) {

        for (ClientGhostDomain domain :
                ClientGhostDomainManager.getDomains()) {

            if (!DOMAIN_TYPE.equals(
                    domain.getDomainType())) {
                continue;
            }

            if (domain.contains(x, y, z)) {
                return true;
            }
        }

        return false;
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
         * 当前维度的所有鬼雨伞鬼域。
         */
        RAIN_SOURCES.clear();

        for (ClientGhostDomain domain :
                ClientGhostDomainManager.getDomains()) {

            if (!DOMAIN_TYPE.equals(
                    domain.getDomainType())) {
                continue;
            }

            if (!domain.getDimension().equals(
                    level.dimension().location())) {
                continue;
            }

            if (domain.getShape()
                    instanceof com.qidate.qisplan2.ghost.domain.CylinderDomainShape cylinder) {

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