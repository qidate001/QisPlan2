package com.qidate.qisplan2;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.logging.LogUtils;
import com.qidate.qisplan2.ghost.domain.client.renderer.GhostDomainShaderRegistry;
import com.qidate.qisplan2.core.ModEntityAttributes;
import com.qidate.qisplan2.core.ModRegistries;
import com.qidate.qisplan2.core.QisConfig;
import com.qidate.qisplan2.event.GhostLayerCombatHandler;
import com.qidate.qisplan2.event.PossessionDamageHandler;
import com.qidate.qisplan2.ghost.GhostAbilityInteractionHandler;
import com.qidate.qisplan2.ghost.ability.GhostAbilityRegistry;
import com.qidate.qisplan2.ghost.domain.GhostDomainPlayerLogout;
import com.qidate.qisplan2.ghost.domain.GhostDomainPlayerSync;
import com.qidate.qisplan2.ghost.domain.GhostDomainServerManager;
import com.qidate.qisplan2.ghost.doorplate.GhostDoorPlateTeleportHandler;
import com.qidate.qisplan2.ghost.layer.GhostLayerInteractionHandler;
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

        // 注册 Shaders
        modEventBus.addListener(
                QisPlan2::registerShaders
        );

        // 灵异伤害类型注册
        NeoForge.EVENT_BUS.register(
                PossessionDamageHandler.class
        );

        // 普通伤害处理
        NeoForge.EVENT_BUS.register(
                GhostLayerCombatHandler.class
        );

        // 鬼域异步
        NeoForge.EVENT_BUS.register(
                GhostDomainPlayerSync.class
        );

        // 鬼域监听玩家退出
        NeoForge.EVENT_BUS.register(
                GhostDomainPlayerLogout.class
        );

        // 鬼域互动隔离
        NeoForge.EVENT_BUS.register(
                GhostLayerInteractionHandler.class
        );

        // 驭鬼注册表注册
        GhostAbilityRegistry.bootstrap();

        // 驭鬼事件注册
        GhostAbilityInteractionHandler.register();

        // 鬼门牌注册
        GhostDoorPlateTeleportHandler.register();
    }

    private static void registerShaders(
            RegisterShadersEvent event
    ) {

        try {

            /*
             * ========================================================
             * 鬼眼 Shader
             * ========================================================
             */

            ResourceLocation ghostEyeId =
                    ResourceLocation.fromNamespaceAndPath(
                            MODID,
                            "ghost_eye"
                    );

            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            ghostEyeId,
                            DefaultVertexFormat.POSITION
                    ),
                    shader -> GhostDomainShaderRegistry.register(
                            ghostEyeId,
                            shader
                    )
            );


            /*
             * ========================================================
             * 鬼伞 Shader
             * ========================================================
             */

            ResourceLocation ghostUmbrellaId =
                    ResourceLocation.fromNamespaceAndPath(
                            MODID,
                            "ghost_umbrella"
                    );

            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            ghostUmbrellaId,
                            DefaultVertexFormat.POSITION
                    ),
                    shader -> GhostDomainShaderRegistry.register(
                            ghostUmbrellaId,
                            shader
                    )
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to register ghost domain shader",
                    e
            );
        }
    }
}
