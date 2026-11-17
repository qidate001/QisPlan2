package com.qidate.qisplan2.client.screen;

import com.qidate.qisplan2.core.QisConfig;
import com.qidate.qisplan2.ghost.module.implementation.GhostBookModule;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/** 客户端界面适配。问答服务与凭据保留在原客户端流程中，不进入厉鬼模块。 */
public final class GhostScreenInputs {
    /** 工具类无需实例化。 */
    private GhostScreenInputs() {}
    /** 打开已知界面；沿用鬼书原有问答服务，不自动提交任何问题。 */
    public static void open(ResourceLocation screen) {
        var minecraft=Minecraft.getInstance();
        if (minecraft.player==null || !minecraft.player.isAlive()) return;
        if (GhostBookModule.ID.equals(screen)) minecraft.setScreen(new GhostBookScreen(
                QisConfig.CLIENT.API_KEY.get(),QisConfig.CLIENT.MODEL_NAME.get()));
    }
}
