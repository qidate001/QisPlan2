package com.qidate.qisplan2.ghost.module.implementation.candle;

import net.minecraft.resources.ResourceLocation;

/** 白色鬼烛：持有点燃实例时令厉鬼停止袭击。 */
public final class WhiteGhostCandleModule extends GhostCandleModule {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("qisplan2", "white_ghost_candle");
    public WhiteGhostCandleModule() { super("white_ghost_candle"); }
}
