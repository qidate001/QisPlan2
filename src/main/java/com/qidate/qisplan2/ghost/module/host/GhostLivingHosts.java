package com.qidate.qisplan2.ghost.module.host;

import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 物理适配层区分身体自身承载与携带物品；关联在同一实体上不代表共用一个宿主。 */
public final class GhostLivingHosts {
    private GhostLivingHosts() {}

    /** 身体的模块属于其自身组成，物品的模块仍属于物品，不能据携带关系转移侵蚀与属性。 */
    public enum Relation { INTRINSIC, CARRIED_ITEM }

    /** 只供适配层使用的宿主关联，不向厉鬼模块暴露物理身份。 */
    public record Binding(GhostModuleHost host, Relation relation) {
        public Binding { Objects.requireNonNull(host); Objects.requireNonNull(relation); }
    }

    /** 收集生命对象关联的宿主，同时保留身体与物品的边界。 */
    public static List<Binding> bindings(LivingEntity entity) {
        var result = new ArrayList<Binding>();
        if (entity instanceof Player player) result.add(new Binding(new PlayerGhostModuleHost(player), Relation.INTRINSIC));
        else if (entity instanceof GhostModuleHost host) result.add(new Binding(host, Relation.INTRINSIC));
        for (var stack : entity.getAllSlots())
            if (!stack.isEmpty() && !GhostItemData.getModules(stack).isEmpty())
                result.add(new Binding(new GhostItemModuleHost(stack, entity), Relation.CARRIED_ITEM));
        return List.copyOf(result);
    }

    /** 能力、媒介、灵异对抗仍可查询全部关联来源；不能用此结果计算身体承载加成。 */
    public static List<GhostModuleHost> collect(LivingEntity entity) {
        return bindings(entity).stream().map(Binding::host).toList();
    }

    /** 身体属性只读取身体自身承载的宿主，不把持有或穿戴当作驾驭。 */
    public static List<GhostModuleHost> intrinsic(LivingEntity entity) {
        return intrinsic(bindings(entity));
    }

    /** 从明确的承载关系选择内在宿主；不按模块类型或玩家身份改变属性公式。 */
    public static List<GhostModuleHost> intrinsic(List<Binding> bindings) {
        return bindings.stream().filter(binding -> binding.relation() == Relation.INTRINSIC).map(Binding::host).toList();
    }
}
