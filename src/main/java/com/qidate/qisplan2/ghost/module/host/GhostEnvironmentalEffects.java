package com.qidate.qisplan2.ghost.module.host;

import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.environment.GhostEnvironmentalEffect;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 世界系统查询能力效果，不识别红白鬼烛物品或具体模块类。 */
public final class GhostEnvironmentalEffects {
    /**
     * 禁止外部实例化此工具类；功能统一通过静态入口调用。
     */
    private GhostEnvironmentalEffects() {}

    /**
     * 检查指定载体是否存在已生效的环境能力。
     * 要求模块可用、已点燃、效果类型匹配且满足暴露媒介。
     */
    public static boolean active(GhostModuleHost host, GhostEnvironmentalEffect effect) {
        return host.getModules().stream().anyMatch(e -> GhostModuleCombatState.available(e)
                && e.customData().getBoolean("lit")
                && effect.name().equals(e.customData().getString("activeEffect"))
                && host.moduleEnvironment(e.id()).exposed());
    }

    /**
     * 汇总生命实体自身及可识别携带物提供的环境效果。
     * 世界系统通过此入口查询效果，不依赖具体鬼烛物品类。
     */
    public static boolean on(LivingEntity entity, GhostEnvironmentalEffect effect) {
        if (entity instanceof GhostModuleHost host && active(host, effect)) return true;
        if (entity instanceof Player player) {
            return active(new PlayerGhostModuleHost(player), effect)
                    || active(new GhostItemModuleHost(player.getMainHandItem(), player), effect)
                    || active(new GhostItemModuleHost(player.getOffhandItem(), player), effect);
        }
        return false;
    }

    /** 仅用于物品外观兼容查询，不代表已经满足暴露媒介。 */
    public static boolean isLit(ItemStack stack, ResourceLocation id) {
        return !stack.isEmpty() && new GhostItemModuleHost(stack).getModules().stream().anyMatch(e ->
                e.id().equals(id) && GhostModuleCombatState.available(e) && e.customData().getBoolean("lit"));
    }
}
