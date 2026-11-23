package com.qidate.qisplan2.ghost.module.host;

import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** 在世界适配边界分类目标；分类依据实际承载模块，不把背包中的物品模块算作驾驭。 */
public record GhostDomainTargetFacts(int group,String name,String type) {
    /** 实体 ID 使用注册表名称，玩家使用游戏档案名称而非可修改的显示名。 */
    public static GhostDomainTargetFacts of(Entity entity) {
        GhostModuleHost host=entity instanceof Player player?new PlayerGhostModuleHost(player):entity instanceof GhostModuleHost h?h:null;
        boolean ghost=host!=null && host.getModules().stream().anyMatch(e->host.carriesModule(e.id()));
        int group=entity instanceof Player ? (ghost?2:0):(ghost?1:3);
        return new GhostDomainTargetFacts(group,entity instanceof Player p?p.getGameProfile().getName():"",BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }
}
