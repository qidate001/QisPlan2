package com.qidate.qisplan2.client;

import com.mojang.blaze3d.vertex.*;
import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.item.GhostUmbrellaItem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

public final class GhostUmbrellaDomainClient {

    /**
     * 鬼雨领域半径。
     */
    public static final double DOMAIN_RADIUS = 50.0D;

    private static final double DOMAIN_RADIUS_SQR =
            DOMAIN_RADIUS * DOMAIN_RADIUS;

    /**
     * 当前玩家是否处于鬼雨领域。
     */
    private static boolean insideDomain = false;

    private GhostUmbrellaDomainClient() {
    }

    /**
     * 每 tick 检查一次玩家是否进入鬼雨领域。
     */
    @SubscribeEvent
    public static void onClientTick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null) {

            insideDomain = false;
            return;
        }

        ClientLevel level =
                minecraft.level;

        Player localPlayer =
                minecraft.player;

        insideDomain =
                isInsideGhostRainDomain(
                        level,
                        localPlayer
                );

        if (!insideDomain) {
            return;
        }

        if (level.getGameTime() % 20 == 0) {
            QisPlan2.LOGGER.info(
                    "[QisPlan2] 玩家处于鬼雨领域，开始生成黑雨"
            );
        }

        spawnBlackRain(
                level,
                localPlayer
        );
    }

    /**
     * 判断玩家是否位于任意打开的鬼雨伞领域内。
     *
     * 当前使用水平 X/Z 半径 50。
     */
    private static boolean isInsideGhostRainDomain(
            ClientLevel level,
            Player localPlayer
    ) {
        var players =
                level.players();

        for (Player player : players) {

            ItemStack mainHand =
                    player.getMainHandItem();

            ItemStack offHand =
                    player.getOffhandItem();

            boolean open =
                    isOpenUmbrella(mainHand)
                            || isOpenUmbrella(offHand);

            if (!open) {
                continue;
            }

            double dx =
                    localPlayer.getX()
                            - player.getX();

            double dz =
                    localPlayer.getZ()
                            - player.getZ();

            double distanceSqr =
                    dx * dx
                            + dz * dz;

            if (distanceSqr
                    <= DOMAIN_RADIUS_SQR) {

                return true;
            }
        }

        return false;
    }

    private static boolean isOpenUmbrella(
            ItemStack stack
    ) {
        return stack.getItem()
                instanceof GhostUmbrellaItem
                && GhostUmbrellaItem.isOpen(stack);
    }

    /**
     * 黑雨生成。
     */
    private static final double RAIN_VISUAL_RADIUS = 4.0D;
    private static final int RAIN_PARTICLES_PER_TICK = 120;

    private static void spawnBlackRain(
            ClientLevel level,
            Player player
    ) {

        for (int i = 0; i < RAIN_PARTICLES_PER_TICK; i++) {

            /*
             * 玩家周围 4 格范围内随机。
             */
            double x =
                    player.getX()
                            + (level.random.nextDouble() - 0.5D)
                            * RAIN_VISUAL_RADIUS * 2.0D;

            double z =
                    player.getZ()
                            + (level.random.nextDouble() - 0.5D)
                            * RAIN_VISUAL_RADIUS * 2.0D;

            /*
             * 从玩家头顶 6~18 格开始下落。
             */
            double y =
                    player.getY()
                            + 6.0D
                            + level.random.nextDouble() * 12.0D;

            level.addParticle(
                    QisPlan2.BLACK_RAIN.get(),
                    x,
                    y,
                    z,
                    0.0D,
                    -0.75D,
                    0.0D
            );
        }
    }

    /**
     * 在天空阶段压暗天空。
     *
     * AFTER_SKY 发生在天空盒绘制完成之后，
     * 而方块/实体等世界内容还没有覆盖上来。
     */
    @SubscribeEvent
    public static void onRenderLevelStage(
            RenderLevelStageEvent event
    ) {
        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_SKY) {

            return;
        }

        if (!insideDomain) {
            return;
        }

        renderBlackSky(
                event.getPoseStack()
        );
    }

    private static void renderBlackSky(
            PoseStack poseStack
    ) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        RenderSystem.setShader(
                GameRenderer::getPositionColorShader
        );

        /*
         * 以摄像机为中心画一个超大的盒子。
         *
         * 因为现在处于 AFTER_SKY：
         *
         * 天空已经绘制
         * ↓
         * 画半透明黑盒
         * ↓
         * 后面的地形/实体继续正常绘制
         *
         * 所以最终效果主要压暗天空。
         */
        float size = 1000.0F;

        BufferBuilder buffer =
                Tesselator.getInstance()
                        .begin(
                                VertexFormat.Mode.QUADS,
                                DefaultVertexFormat.POSITION_COLOR
                        );

        int color =
                80 << 24;

        /*
         * 前
         */
        vertex(
                buffer,
                -size,
                -size,
                -size,
                color
        );
        vertex(
                buffer,
                size,
                -size,
                -size,
                color
        );
        vertex(
                buffer,
                size,
                size,
                -size,
                color
        );
        vertex(
                buffer,
                -size,
                size,
                -size,
                color
        );

        /*
         * 后
         */
        vertex(
                buffer,
                size,
                -size,
                size,
                color
        );
        vertex(
                buffer,
                -size,
                -size,
                size,
                color
        );
        vertex(
                buffer,
                -size,
                size,
                size,
                color
        );
        vertex(
                buffer,
                size,
                size,
                size,
                color
        );

        /*
         * 左
         */
        vertex(
                buffer,
                -size,
                -size,
                size,
                color
        );
        vertex(
                buffer,
                -size,
                -size,
                -size,
                color
        );
        vertex(
                buffer,
                -size,
                size,
                -size,
                color
        );
        vertex(
                buffer,
                -size,
                size,
                size,
                color
        );

        /*
         * 右
         */
        vertex(
                buffer,
                size,
                -size,
                -size,
                color
        );
        vertex(
                buffer,
                size,
                -size,
                size,
                color
        );
        vertex(
                buffer,
                size,
                size,
                size,
                color
        );
        vertex(
                buffer,
                size,
                size,
                -size,
                color
        );

        /*
         * 上
         */
        vertex(
                buffer,
                -size,
                size,
                -size,
                color
        );
        vertex(
                buffer,
                size,
                size,
                -size,
                color
        );
        vertex(
                buffer,
                size,
                size,
                size,
                color
        );
        vertex(
                buffer,
                -size,
                size,
                size,
                color
        );

        /*
         * 下
         */
        vertex(
                buffer,
                -size,
                -size,
                size,
                color
        );
        vertex(
                buffer,
                size,
                -size,
                size,
                color
        );
        vertex(
                buffer,
                size,
                -size,
                -size,
                color
        );
        vertex(
                buffer,
                -size,
                -size,
                -size,
                color
        );

        var mesh =
                buffer.buildOrThrow();

        BufferUploader.drawWithShader(
                mesh
        );

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void vertex(
            BufferBuilder buffer,
            float x,
            float y,
            float z,
            int color
    ) {
        buffer.addVertex(
                x,
                y,
                z
        ).setColor(color);
    }
}