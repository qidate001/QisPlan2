package com.qidate.qisplan2.client;

import com.qidate.qisplan2.item.GhostUmbrellaItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

    public static boolean isInsideDomain() {
        return insideDomain;
    }

    public static boolean isPositionInsideDomain(
            double x,
            double z
    ) {
        for (GhostRainSource source : RAIN_SOURCES) {

            double dx =
                    x - source.x();

            double dz =
                    z - source.z();

            double radius =
                    source.radius();

            if (dx * dx + dz * dz
                    <= radius * radius) {

                return true;
            }
        }

        return false;
    }


    /*
     * 鬼雨环境音计时器。
     *
     * 0 表示可以立即播放下一次。
     */
    private static int rainSoundTime = 0;


    private GhostUmbrellaDomainClient() {
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
         * 鬼雨环境音。
         */
        tickRainSound(
                minecraft
        );
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

    private static void tickRainSound(
            Minecraft minecraft
    ) {
        /*
         * 不在鬼雨领域：
         * 清空计时器。
         */
        if (!insideDomain) {
            rainSoundTime = 0;
            return;
        }

        /*
         * 还没到下一次播放时间。
         */
        if (rainSoundTime > 0) {
            rainSoundTime--;
            return;
        }

        /*
         * 找不到玩家就不播放。
         */
        if (minecraft.player == null
                || minecraft.level == null) {
            return;
        }

        /*
         * ========================================================
         * 播放一次原版雨声事件
         * ========================================================
         *
         * 不使用 looping。
         *
         * 每次只播放一次 WEATHER_RAIN，
         * 然后等待一小段时间再播放下一次。
         */
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

        /*
         * ========================================================
         * 下一次播放时间
         * ========================================================
         *
         * 原版本身有 rainSoundTime 调度机制；
         * 我们这里保留这个思路。
         *
         * 不要每 tick 播放。
         */
        rainSoundTime =
                12 + minecraft.level.random.nextInt(12);
    }
}