package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.core.ModTags;
import com.qidate.qisplan2.ghost.module.event.TouchEvent;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemData;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Minecraft 行为到灵异语义事件的转译层。
 */
public final class GhostModuleInteractionHandler {

    private GhostModuleInteractionHandler() {
    }


    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        // 模块规则只在服务端执行。
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        var player = event.getEntity();

        // 当前第一版只处理玩家主手攻击。
        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty()) {
            return;
        }

        // 非灵异物品不进入模块系统。
        if (!stack.is(ModTags.Items.SUPERNATURAL_ITEMS)) {
            return;
        }

        // 普通白板剑没有模块，不触发任何规则。
        if (GhostItemData.getModules(stack).isEmpty()) {
            return;
        }

        // TouchEvent 描述触碰来源与目标，不规定具体效果。
        if (event.getTarget() instanceof LivingEntity target) {
            GhostModuleRuntime.dispatch(
                    new GhostItemModuleHost(stack, player),
                    new TouchEvent(player, target)
            );
        }
    }

    /**
     * 不可触规则：持有灵异模块物品时，每秒触发一次自身触碰事件。
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        // 仅在服务器执行
        if (player.level().isClientSide()) {
            return;
        }


        // 只检查主手，避免背包中的物品意外触发
        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty()) {
            return;
        }

        // 必须属于可承载灵异模块的物品
        if (!stack.is(ModTags.Items.SUPERNATURAL_ITEMS)) {
            return;
        }

        // 没有模块则不触发
        if (GhostItemData.getModules(stack).isEmpty()) {
            return;
        }

        // 持续手持每 tick 贡献一 tick 接触时长；没有把两次采样之间的空档补算进去。
        var host = new GhostItemModuleHost(stack, player);
        GhostModuleRuntime.dispatch(host, TouchEvent.sample(player, player, false));
        // 保留不可触物品原有的每秒一次触碰触发；瞬时事实不增加鬼地毯计时。
        if (player.isAlive() && player.tickCount % 20 == 0)
            GhostModuleRuntime.dispatch(host, new TouchEvent(player, player));
    }
}