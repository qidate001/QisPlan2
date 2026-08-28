package com.qidate.qisplan2;

import com.mojang.logging.LogUtils;
import com.qidate.qisplan2.client.GhostUmbrellaClient;
import com.qidate.qisplan2.core.ModEntityAttributes;
import com.qidate.qisplan2.core.ModRegistries;
import com.qidate.qisplan2.core.QisConfig;
import com.qidate.qisplan2.event.GhostUmbrellaAttackHandler;
import com.qidate.qisplan2.ghost.GhostAbilityInteractionHandler;
import com.qidate.qisplan2.ghost.ability.GhostAbilityRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

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

        modEventBus.register(
                GhostUmbrellaClient.class
        );

        NeoForge.EVENT_BUS.register(
                GhostUmbrellaAttackHandler.class
        );

        // 驭鬼注册表注册
        GhostAbilityRegistry.bootstrap();

        // 驭鬼事件注册
        GhostAbilityInteractionHandler.register();
    }
}
