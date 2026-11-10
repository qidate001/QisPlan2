package com.qidate.qisplan2.item.candle;

import com.qidate.qisplan2.core.ModDataComponents;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.host.item.GhostModuleItem;
import com.qidate.qisplan2.ghost.module.implementation.candle.WhiteGhostCandleModule;
import com.qidate.qisplan2.ghost.module.implementation.candle.GhostCandleModule;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** 只配置默认鬼烛模块，点燃、燃烧及效果由模块系统负责。 */
public class WhiteGhostCandleItem extends GhostModuleItem {
    public WhiteGhostCandleItem(Properties properties) {
        super(properties.component(ModDataComponents.GHOST_MODULES.get(),
                List.of(new GhostModuleData.Entry(WhiteGhostCandleModule.ID,
                        new WhiteGhostCandleModule().getInitialData().intensity()))));
    }
    public static boolean isLit(ItemStack stack) { return GhostCandleModule.isLit(stack, WhiteGhostCandleModule.ID); }
}
