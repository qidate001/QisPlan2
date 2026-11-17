package com.qidate.qisplan2.item;

import com.qidate.qisplan2.core.ModDataComponents;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.host.item.GhostModuleItem;
import com.qidate.qisplan2.ghost.module.implementation.GhostBookModule;
import java.util.List;

/** 自带完整鬼书模块的普通物品；右键、驾驭及周期处理复用通用物品入口。 */
public class GhostBookItem extends GhostModuleItem {
    /** 仅声明默认模块，物品不再直接读取配置或打开客户端界面。 */
    public GhostBookItem(Properties properties) {
        super(properties.component(ModDataComponents.GHOST_MODULES.get(),
                List.of(new GhostModuleData.Entry(GhostBookModule.ID,1))));
    }
}
