package com.qidate.qisplan2.ghost.module.implementation.candle;

import net.minecraft.resources.ResourceLocation;
import com.qidate.qisplan2.ghost.module.environment.GhostEnvironmentalEffect;

/** 红色鬼烛：持有点燃实例时吸引厉鬼。 */
public final class RedGhostCandleModule extends GhostCandleModule {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("qisplan2", "red_ghost_candle");

    /**
     * 创建红色鬼烛的共享模块定义，绑定红烛注册标识；不创建点燃状态或燃料实例。
     */
    public RedGhostCandleModule() { super("red_ghost_candle"); }

    /**
     * 声明能力输出的环境效果类型，供世界系统统一查询。
     */
    @Override public GhostEnvironmentalEffect environmentalEffect() { return GhostEnvironmentalEffect.ATTRACTION; }
}
