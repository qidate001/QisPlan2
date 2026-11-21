package com.qidate.qisplan2.item;

import com.qidate.qisplan2.core.ModDataComponents;
import com.qidate.qisplan2.core.ModItems;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.host.item.GhostModuleItem;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import com.qidate.qisplan2.ghost.module.implementation.GhostUmbrellaModule;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.List;

/** 自带鬼雨伞模块的物品。只负责默认组件与模型状态，不创建鬼域或实施袭击。 */
public class GhostUmbrellaItem extends GhostModuleItem {
    /** 默认安装完整模块，随物品保存和转移。 */
    public GhostUmbrellaItem(Properties properties) {
        super(properties.component(ModDataComponents.GHOST_MODULES.get(),List.of(new GhostModuleData.Entry(GhostUmbrellaModule.ID,100,0))));
    }
    /** 客户端模型继续读取原有组件。 */
    public static boolean isOpen(ItemStack stack) { return Boolean.TRUE.equals(stack.get(ModItems.GHOST_UMBRELLA_OPEN)); }
    /** 外观适配不参与模块规则。 */
    public static void setOpen(ItemStack stack,boolean open) { if(isOpen(stack)!=open)stack.set(ModItems.GHOST_UMBRELLA_OPEN,open); }
    /** 首次读入旧物品时迁移开关，此后由模块状态驱动外观。 */
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected) {
        if(!level.isClientSide && entity instanceof LivingEntity living) {
            var host=new GhostItemModuleHost(stack,living);
            if(host.hasModule(GhostUmbrellaModule.ID)) {
                var data=host.getModuleCustomData(GhostUmbrellaModule.ID);
                if(!data.contains("domainOpen")) {
                    data.putBoolean("domainOpen",isOpen(stack));
                    data.putLong("domainOpenedAt",stack.getOrDefault(ModDataComponents.GHOST_UMBRELLA_OPENED_AT,0L));
                    host.setModuleCustomData(GhostUmbrellaModule.ID,data);
                }
            }
        }
        super.inventoryTick(stack,level,entity,slot,selected);
        if(!level.isClientSide) {
            var host=new GhostItemModuleHost(stack);
            setOpen(stack,host.hasModule(GhostUmbrellaModule.ID) && host.getModuleCustomData(GhostUmbrellaModule.ID).getBoolean("domainOpen"));
        }
    }
}
