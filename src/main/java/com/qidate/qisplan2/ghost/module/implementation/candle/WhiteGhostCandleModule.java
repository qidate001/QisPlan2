package com.qidate.qisplan2.ghost.module.implementation.candle;

import net.minecraft.resources.ResourceLocation;
import com.qidate.qisplan2.ghost.module.environment.GhostEnvironmentalEffect;

/** 白色鬼烛：持有点燃实例时令厉鬼停止袭击。 */
public final class WhiteGhostCandleModule extends GhostCandleModule {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("qisplan2", "white_ghost_candle");

    /**
     * 创建白色鬼烛的共享模块定义，绑定白烛注册标识；不创建点燃状态或燃料实例。
     */
    public WhiteGhostCandleModule() { super("white_ghost_candle"); }

    /**
     * 声明能力输出的环境效果类型，供世界系统统一查询。
     */
    @Override public GhostEnvironmentalEffect environmentalEffect() { return GhostEnvironmentalEffect.CONCEALMENT; }
}
