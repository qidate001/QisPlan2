package com.qidate.qisplan2.ghost.module.host;

import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
import java.util.List;

/** 将物理生命对象关联到实际作用于其身体的宿主；只有此适配层认识玩家和装备。 */
public final class GhostLivingHosts {
    private GhostLivingHosts() {}
    /** 装备中的物品仍是独立宿主，不冒充体内驾驭模块。 */
    public static List<GhostModuleHost> collect(LivingEntity entity){
        List<GhostModuleHost> result=new ArrayList<>();
        if(entity instanceof Player player)result.add(new PlayerGhostModuleHost(player));
        else if(entity instanceof GhostModuleHost host)result.add(host);
        for(var stack:entity.getAllSlots())if(!stack.isEmpty()&&!GhostItemData.getModules(stack).isEmpty())result.add(new GhostItemModuleHost(stack,entity));
        return result;
    }
}
