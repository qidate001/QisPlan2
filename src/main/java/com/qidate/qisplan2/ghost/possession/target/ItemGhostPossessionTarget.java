package com.qidate.qisplan2.ghost.possession.target;

import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemData;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ItemGhostPossessionTarget
        implements GhostPossessionTarget {

    private final ItemStack stack;

    private final ResourceLocation abilityId;

    private final Consumer<ServerPlayer> failureAction;

    public ItemGhostPossessionTarget(
            ItemStack stack,
            ResourceLocation abilityId,
            Consumer<ServerPlayer> failureAction
    ) {
        this.stack = stack;
        this.abilityId = abilityId;
        this.failureAction = failureAction;
    }

    @Override
    public boolean onSuccess(
            ServerPlayer player
    ) {

        // 结束小游戏时重新确认该物品仍在玩家手中，防止转移已丢弃或替换的物品。
        if (stack.isEmpty() || (player.getMainHandItem() != stack && player.getOffhandItem() != stack)) return false;
        var entries = GhostItemData.getModules(stack);
        var entry = entries.stream().filter(e -> e.id().equals(abilityId)).findFirst().orElse(null);
        if (entry == null) {
            // 尚未写入组件的旧版专用物品，以同一模块定义建立初始实例。
            var module = GhostModuleRegistry.get(abilityId);
            if (module == null || !entries.isEmpty()) return false;
            var initial = module.getInitialData();
            entry = new GhostModuleData.Entry(abilityId, initial.intensity(), initial.revival(), false, false, new CompoundTag());
        }
        if (!PossessionHandler.possess(player, entry)) return false;
        var remaining = entries.stream().filter(e -> !e.id().equals(abilityId)).toList();
        if (remaining.isEmpty()) stack.shrink(1);
        else {
            // 一件物品上的其他模块保持原样；可堆叠物品先分离一件再修改，避免批量移除。
            var changed = stack.copyWithCount(1);
            new GhostItemModuleHost(changed).removeModule(abilityId);
            stack.shrink(1);
            if (!player.getInventory().add(changed)) player.drop(changed, false);
        }

        return true;
    }

    @Override
    public void onFailure(
            ServerPlayer player
    ) {

        failureAction.accept(
                player
        );
    }
}