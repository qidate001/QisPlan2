package com.qidate.qisplan2.item.candle;

import com.qidate.qisplan2.core.ModDataComponents;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.host.item.GhostModuleItem;
import com.qidate.qisplan2.ghost.module.implementation.candle.WhiteGhostCandleModule;
import com.qidate.qisplan2.ghost.module.host.GhostEnvironmentalEffects;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** 只配置默认鬼烛模块，点燃、燃烧及效果由模块系统负责。 */
public class WhiteGhostCandleItem extends GhostModuleItem {
    /**
     * 配置自带白烛模块的物品默认组件。
     * 新物品的模块强度来自白烛定义，交互和 tick 复用通用模块物品父类。
     */
    public WhiteGhostCandleItem(Properties properties) {
        super(properties.component(ModDataComponents.GHOST_MODULES.get(),
                List.of(new GhostModuleData.Entry(WhiteGhostCandleModule.ID,
                        new WhiteGhostCandleModule().getInitialData().intensity()))));
    }

    /**
     * 读取点燃状态并结合模块可用性判断是否有效，避免死机或未点燃时显示生效。
     */
    public static boolean isLit(ItemStack stack) { return GhostEnvironmentalEffects.isLit(stack, WhiteGhostCandleModule.ID); }
}
