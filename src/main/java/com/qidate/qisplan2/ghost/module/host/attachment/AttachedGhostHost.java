package com.qidate.qisplan2.ghost.module.host.attachment;

import com.qidate.qisplan2.ghost.module.host.GhostWorldAdapters;
import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.environment.GhostWorldAccess;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemData;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** 独立的附着来源，只读原物品中的模块。目标身体只是锚点，不是模块的驾驭者。 */
public record AttachedGhostHost(ItemStack stack) implements GhostModuleHost {
    /** 关系重建时读取原实例数据，绝不合并进目标模块列表。 */
    @Override public List<GhostModuleData.Entry> getModules() { return GhostItemData.getModules(stack); }
    /** 独立来源只提供其已成立的插入媒介，不暴露锚定的实体。 */
    @Override public GhostWorldAccess moduleWorld(ResourceLocation module) {
        return GhostWorldAdapters.bind(this, module);
    }
}
