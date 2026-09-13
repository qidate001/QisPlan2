package com.qidate.qisplan2;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.logging.LogUtils;
import com.qidate.qisplan2.client.renderer.GhostEyeShader;
import com.qidate.qisplan2.core.ModEntityAttributes;
import com.qidate.qisplan2.core.ModRegistries;
import com.qidate.qisplan2.core.QisConfig;
import com.qidate.qisplan2.event.PossessionDamageHandler;
import com.qidate.qisplan2.ghost.GhostAbilityInteractionHandler;
import com.qidate.qisplan2.ghost.ability.GhostAbilityRegistry;
import com.qidate.qisplan2.ghost.domain.GhostDomainPlayerLogout;
import com.qidate.qisplan2.ghost.domain.GhostDomainPlayerSync;
import com.qidate.qisplan2.ghost.domain.GhostDomainServerTick;
import com.qidate.qisplan2.ghost.doorplate.GhostDoorPlateTeleportHandler;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.io.IOException;

@Mod(QisPlan2.MODID)
public class QisPlan2 {
    public static final String MODID = "qisplan2";
    public static final Logger LOGGER = LogUtils.getLogger();

    // 模组入口
    public QisPlan2(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(
                net.neoforged.fml.config.ModConfig.Type.CLIENT,
                QisConfig.CLIENT_SPEC
        );

        // 完整注册模组 Bus
        ModRegistries.registerAll(modEventBus);
        ModRegistries.initAll();

        // 实体属性注册
        modEventBus.addListener(
                ModEntityAttributes::register
        );

        modEventBus.addListener(
                QisPlan2::registerShaders
        );

        // 灵异伤害类型注册
        NeoForge.EVENT_BUS.register(
                PossessionDamageHandler.class
        );

        // 鬼域
        NeoForge.EVENT_BUS.register(
                GhostDomainServerTick.class
        );

        // 鬼域异步
        NeoForge.EVENT_BUS.register(
                GhostDomainPlayerSync.class
        );

        // 鬼域监听玩家退出
        NeoForge.EVENT_BUS.register(
                GhostDomainPlayerLogout.class
        );

        // 驭鬼注册表注册
        GhostAbilityRegistry.bootstrap();

        // 驭鬼事件注册
        GhostAbilityInteractionHandler.register();

        // 鬼门牌注册
        GhostDoorPlateTeleportHandler.register();
    }

    private static void registerShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            ResourceLocation.fromNamespaceAndPath(
                                    MODID,
                                    "ghost_eye"
                            ),
                            DefaultVertexFormat.POSITION
                    ),
                    GhostEyeShader::setInstance
            );
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to register ghost eye shader",
                    e
            );
        }
    }
}
