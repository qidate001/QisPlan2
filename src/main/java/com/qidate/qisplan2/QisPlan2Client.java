package com.qidate.qisplan2;

import com.qidate.qisplan2.event.DeathCurseHudOverlay;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = QisPlan2.MODID, dist = Dist.CLIENT)
public class QisPlan2Client {

    public QisPlan2Client(IEventBus modEventBus, ModContainer modContainer) {

        // 注册配置界面工厂
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (mc, parent) -> new ConfigurationScreen(modContainer, parent)
        );

        // 注册客户端 HUD
        modEventBus.addListener(
                DeathCurseHudOverlay::registerDeathCurseLayer
        );
    }
}