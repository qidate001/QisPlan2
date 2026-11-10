package com.qidate.qisplan2.item;

import com.qidate.qisplan2.core.ModDataComponents;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.host.item.GhostModuleItem;
import com.qidate.qisplan2.ghost.module.implementation.divinationslip.GhostDivinationSlipModule;
import java.util.List;

/**
 * 自带鬼签模块的普通物品。
 * 这里只声明默认模块，不再保存独立死机时间，也不直接实现抽签、保护或袭击。
 * 使用与驾驭复用通用模块物品入口，实际效果由鬼签模块处理。
 */
public class GhostDivinationSlipItem extends GhostModuleItem {
    public GhostDivinationSlipItem(Properties properties) {
        super(properties.component(ModDataComponents.GHOST_MODULES.get(), defaultModules()));
    }

    private static List<GhostModuleData.Entry> defaultModules() {
        var initial = new GhostDivinationSlipModule().getInitialData();
        return List.of(new GhostModuleData.Entry(GhostDivinationSlipModule.ID,
                initial.intensity(), initial.revival()));
    }
}