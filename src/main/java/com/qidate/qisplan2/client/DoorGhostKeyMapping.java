package com.qidate.qisplan2.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.network.payload.DoorGhostAbilityPayload;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.client.KeyMapping;

import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(
        modid = QisPlan2.MODID,
        value = Dist.CLIENT
)
public final class DoorGhostKeyMapping {

    public static final KeyMapping DOOR_GHOST_ABILITY =
            new KeyMapping(
                    "key.qisplan2.door_ghost_ability",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_G,
                    "key.categories.qisplan2"
            );

    private DoorGhostKeyMapping() {
    }

    /*
     * ============================================================
     * 注册 G 键
     * ============================================================
     */

    @SubscribeEvent
    public static void registerKeyMappings(
            RegisterKeyMappingsEvent event
    ) {

        event.register(
                DOOR_GHOST_ABILITY
        );
    }

    /*
     * ============================================================
     * G 键触发
     * ============================================================
     */

    @SubscribeEvent
    public static void onClientTick(
            ClientTickEvent.Post event
    ) {

        while (
                DOOR_GHOST_ABILITY.consumeClick()
        ) {

            /*
             * 客户端只发送“我要使用门鬼能力”。
             *
             * 具体攻击谁、能不能用，
             * 全部由服务端决定。
             */
            PacketDistributor.sendToServer(
                    new DoorGhostAbilityPayload()
            );
        }
    }
}