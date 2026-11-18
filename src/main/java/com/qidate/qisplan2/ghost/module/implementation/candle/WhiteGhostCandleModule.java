package com.qidate.qisplan2.ghost.module.implementation.candle;

import java.util.List;
import com.qidate.qisplan2.ghost.module.influence.GhostAttackContext;
import com.qidate.qisplan2.ghost.module.influence.GhostAttackResponse;
import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import net.minecraft.resources.ResourceLocation;
import com.qidate.qisplan2.ghost.module.environment.GhostEnvironmentalEffect;

/** 白色鬼烛：持有点燃实例时令厉鬼停止袭击。 */
public final class WhiteGhostCandleModule extends GhostCandleModule {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("qisplan2", "white_ghost_candle");

    /** 燃烧且暴露时屏蔽来袭；没有白烛的上下文不会调用本声明。 */
    @Override public List<GhostAttackResponse> getAttackResponses() {
        return List.of(new GhostAttackResponse() {
            /** 屏蔽先于消耗生命上限的抵挡。 */
            @Override public int order() { return 0; }
            /** 只读取模块燃烧状态与环境端口，不区分物品或实体。 */
            @Override public void respond(GhostModuleContext module,
                    GhostAttackContext attack) {
                if (isLit(module.getData()) && module.environment().exposed()) attack.event().block();
            }
        });
    }

    /**
     * 创建白色鬼烛的共享模块定义，绑定白烛注册标识；不创建点燃状态或燃料实例。
     */
    public WhiteGhostCandleModule() { super("white_ghost_candle"); }

    /**
     * 声明能力输出的环境效果类型，供世界系统统一查询。
     */
    @Override public GhostEnvironmentalEffect environmentalEffect() { return GhostEnvironmentalEffect.CONCEALMENT; }
}
