package com.qidate.qisplan2.ghost.module.host.player.binding;

import com.qidate.qisplan2.ghost.domain.*;
import com.qidate.qisplan2.ghost.domain.type.mist.GhostMistDomainController;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.event.*;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleContext;
import net.minecraft.resources.ResourceLocation;
import static com.qidate.qisplan2.QisPlan2.MODID;
import com.qidate.qisplan2.ghost.module.implementation.mist.GhostMistModule;

/** 玩家承载方式的生命周期和输入适配，不声明厉鬼身份、规则或初始数据。 */
public final class GhostMistPlayerBinding implements GhostPlayerBinding {
    private static final ResourceLocation ID = GhostMistModule.ID;

    /**
     * 推进玩家载体上的持续状态或表现，持久数据仍通过模块入口保存。
     */
    @Override
    public void onPlayerTick(
            PlayerGhostModuleContext context
    ) {

        GhostMistDomainController.tick(
                context
        );
    }

    /**
     * 关闭玩家的鬼雾鬼域，并通过现有控制器清理运行时状态。
     */
    @Override
    public void onRelease(
            PlayerGhostModuleContext context
    ) {

        GhostMistDomainController.close(
                context.player()
        );
    }
}
