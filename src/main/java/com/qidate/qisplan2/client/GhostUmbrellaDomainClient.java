package com.qidate.qisplan2.client;

import com.qidate.qisplan2.item.GhostUmbrellaItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

public final class GhostUmbrellaDomainClient {

    /*
     * ============================================================
     * 鬼雨领域
     * ============================================================
     */

    private static final double DOMAIN_RADIUS = 50.0D;


    /*
     * ============================================================
     * 当前客户端所有鬼雨源
     * ============================================================
     */

    private static final List<GhostRainSource> RAIN_SOURCES =
            new ArrayList<>();


    /*
     * 本地玩家是否处于至少一个鬼雨领域。
     */
    private static boolean insideDomain = false;


    private GhostUmbrellaDomainClient() {
    }

    /*
     * 声音字段
     */
    private static GhostRainSound rainSound;

    public static boolean isInsideDomain() {
        return insideDomain;
    }


    /*
     * ============================================================
     * Client Tick
     * ============================================================
     */

    @SubscribeEvent
    public static void onClientTick(
            ClientTickEvent.Post event
    ) {

        var minecraft =
                net.minecraft.client.Minecraft.getInstance();


        /*
         * 没进入世界。
         */
        if (minecraft.level == null
                || minecraft.player == null) {

            RAIN_SOURCES.clear();
            insideDomain = false;
            return;
        }


        ClientLevel level =
                minecraft.level;

        Player localPlayer =
                minecraft.player;


        /*
         * ========================================================
         * 重建鬼雨源
         * ========================================================
         */

        RAIN_SOURCES.clear();


        for (Player player : level.players()) {

            /*
             * 主手
             */
            ItemStack mainHand =
                    player.getMainHandItem();


            /*
             * 副手
             */
            ItemStack offHand =
                    player.getOffhandItem();


            boolean umbrellaOpen =
                    isOpenUmbrella(mainHand)
                            || isOpenUmbrella(offHand);


            if (!umbrellaOpen) {
                continue;
            }


            /*
             * 一个打开的鬼雨伞
             * 就是一个鬼雨源。
             */
            RAIN_SOURCES.add(
                    new GhostRainSource(
                            player.getX(),
                            player.getZ(),
                            DOMAIN_RADIUS
                    )
            );
        }


        /*
         * ========================================================
         * 判断本地玩家是否在领域里
         * ========================================================
         */

        insideDomain =
                isInsideAnyDomain(
                        localPlayer
                );

        /*
         * ========================================================
         * 鬼雨声音
         * ========================================================
         */

        if (insideDomain) {

            if (rainSound == null
                    || rainSound.isStopped()) {

                rainSound =
                        new GhostRainSound();

                Minecraft.getInstance()
                        .getSoundManager()
                        .play(rainSound);
            }

        } else {

            if (rainSound != null) {
                rainSound.stopSound();
                rainSound = null;
            }
        }
    }


    /*
     * ============================================================
     * 判断玩家是否在任意鬼雨领域
     * ============================================================
     */

    private static boolean isInsideAnyDomain(
            Player localPlayer
    ) {

        double playerX =
                localPlayer.getX();

        double playerZ =
                localPlayer.getZ();


        for (GhostRainSource source : RAIN_SOURCES) {

            double dx =
                    playerX - source.x();

            double dz =
                    playerZ - source.z();


            double distanceSqr =
                    dx * dx
                            + dz * dz;


            double radius =
                    source.radius();


            if (distanceSqr
                    <= radius * radius) {

                return true;
            }
        }


        return false;
    }


    /*
     * ============================================================
     * 判断伞是否打开
     * ============================================================
     */

    private static boolean isOpenUmbrella(
            ItemStack stack
    ) {

        return stack.getItem()
                instanceof GhostUmbrellaItem
                && GhostUmbrellaItem.isOpen(stack);
    }


    /*
     * ============================================================
     * 世界渲染
     * ============================================================
     */

    @SubscribeEvent
    public static void onRenderLevelStage(
            RenderLevelStageEvent event
    ) {

        /*
         * ========================================================
         * 只在天气之后绘制
         * ========================================================
         */

        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_WEATHER) {

            return;
        }


        /*
         * 没有鬼雨源。
         */
        if (RAIN_SOURCES.isEmpty()) {
            return;
        }


        /*
         * 本地玩家不在任何鬼雨领域。
         */
        if (!insideDomain) {
            return;
        }


        /*
         * ========================================================
         * 绘制鬼雨
         * ========================================================
         */

        GhostRainRenderer.render(
                event,
                RAIN_SOURCES
        );
    }
}