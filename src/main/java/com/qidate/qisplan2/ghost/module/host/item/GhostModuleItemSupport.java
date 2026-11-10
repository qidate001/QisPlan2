package com.qidate.qisplan2.ghost.module.host.item;

import com.qidate.qisplan2.ghost.module.GhostModuleCombatState;
import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.event.GhostUseEvent;
import com.qidate.qisplan2.ghost.possession.manager.GhostPossessionManager;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import com.qidate.qisplan2.ghost.possession.target.ItemGhostPossessionTarget;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 模块物品的共用交互接口。
 * 普通物品和剑可以保留各自的原版父类，通过本接口复用规则分发与驾驭逻辑。
 * 接口不添加默认模块；模块配置由具体物品的 Data Components 决定。
 */
public interface GhostModuleItemSupport {
    /**
     * 普通物品与白板剑共用的交互入口，避免复制规则分发和驾驭逻辑。
     * Shift 使用优先驾驭不可用模块；普通使用只发布使用事实，由模块规则决定效果。
     */
    default InteractionResultHolder<ItemStack> useModules(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            GhostItemModuleHost host = new GhostItemModuleHost(stack);
            if (player.isShiftKeyDown()) {
                var entry = host.getModules().stream()
                        .filter(e -> !GhostModuleCombatState.available(e))
                        .filter(e -> !PossessionHandler.hasGhost(serverPlayer, e.id()))
                        .findFirst().orElse(null);
                if (entry != null) {
                    boolean started = GhostPossessionManager.start(serverPlayer,
                            new ItemGhostPossessionTarget(stack, entry.id(), ignored -> {
                                // 驾驭失败只解除选中模块的死机，不影响物品上的其他模块。
                                if (!host.hasModule(entry.id())) return;
                                host.setDeadlocked(entry.id(), false);
                                GhostModuleCombatState.set(host, entry, 0, false);
                            }));
                    return started ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
                }
            }
            GhostModuleRuntime.dispatch(host, new GhostUseEvent(player));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** 所有模块物品共用的生命周期，不依赖具体物品类型。 */
    default void tickModules(ItemStack stack, Level level, Entity entity) {
        if (level.isClientSide || !(entity instanceof Player player)) return;
        var host = new GhostItemModuleHost(stack);
        for (var entry : host.getModules()) {
            if (stack.isEmpty()) break;
            var module = GhostModuleRegistry.get(entry.id());
            if (module != null) module.onItemTick(host, player);
        }
    }
}
