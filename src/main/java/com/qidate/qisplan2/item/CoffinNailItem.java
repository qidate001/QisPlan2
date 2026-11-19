package com.qidate.qisplan2.item;

import com.qidate.qisplan2.core.ModDataComponents;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.host.item.GhostModuleItem;
import com.qidate.qisplan2.ghost.module.implementation.CoffinNailModule;
import java.util.List;

/** 自带棺材钉模块的物品；不识别实体类型，不直接写入死机状态。 */
public class CoffinNailItem extends GhostModuleItem {
    /** 模块数据跟随物品栈保存，插入后原样进入独立附着容器。 */
    public CoffinNailItem(Properties properties) {
        super(properties.component(ModDataComponents.GHOST_MODULES.get(),
                List.of(new GhostModuleData.Entry(CoffinNailModule.ID, 1))));
    }
}
