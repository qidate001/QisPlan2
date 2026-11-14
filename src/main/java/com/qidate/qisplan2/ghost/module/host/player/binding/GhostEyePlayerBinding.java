package com.qidate.qisplan2.ghost.module.host.player.binding;

import com.qidate.qisplan2.ghost.domain.type.eye.GhostEyeDomainController;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleContext;
import net.minecraft.resources.ResourceLocation;
import com.qidate.qisplan2.ghost.module.implementation.eye.GhostEyeModule;

/** 玩家承载方式的生命周期和输入适配，不声明厉鬼身份、规则或初始数据。 */
public final class GhostEyePlayerBinding implements GhostPlayerBinding {
    private static final ResourceLocation ID = GhostEyeModule.ID;

    /**
     * 推进玩家载体上的持续状态或表现，持久数据仍通过模块入口保存。
     */
    @Override
    public void onPlayerTick(
            PlayerGhostModuleContext context
    ) {

        GhostEyeDomainController.tick(context);
    }

    /**
     * 处理解除驾驭后的表现或鬼域清理。
     */
    @Override
    public void onRelease(
            PlayerGhostModuleContext context
    ) {

        GhostEyeDomainController.close(context.player());
    }
}
