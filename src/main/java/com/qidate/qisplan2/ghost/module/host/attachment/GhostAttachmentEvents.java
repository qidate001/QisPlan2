package com.qidate.qisplan2.ghost.module.host.attachment;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.event.EntityUsedEvent;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import com.qidate.qisplan2.network.GhostAttachmentNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** 附着输入与生命周期适配器。没有世界 tick 事件，也不遍历所有厉鬼。 */
@EventBusSubscriber(modid = QisPlan2.MODID)
public final class GhostAttachmentEvents {
    private GhostAttachmentEvents() {}
    /** 实体交互统一生成事实；Shift 空手拔出，普通交互由持有模块决定是否插入。 */
    @SubscribeEvent public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof LivingEntity target)) return;
        var player = event.getEntity();
        if (player.isSpectator()) return;
        if (player.isShiftKeyDown() && event.getHand() == InteractionHand.MAIN_HAND
                && player.getMainHandItem().isEmpty() && GhostAttachments.hasAttachments(target)) {
            if (!player.level().isClientSide()) GhostAttachments.recover(target, player);
            event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS); return;
        }
        if (player.isShiftKeyDown()) return;
        var host = new GhostItemModuleHost(event.getItemStack(), player);
        boolean supported = host.getModules().stream().map(e -> GhostModuleRegistry.get(e.id()))
                .anyMatch(m -> m != null && m.supportsEntityUse());
        if (!supported) return;
        boolean before = GhostAttachments.hasAttachments(target);
        if (!player.level().isClientSide()) GhostModuleRuntime.dispatch(host, new EntityUsedEvent(player, target));
        if (player.level().isClientSide() || before != GhostAttachments.hasAttachments(target)) {
            event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
    /** 载入只迁移旧状态及释放旧缓存，不反复写入模块。 */
    @SubscribeEvent public static void joined(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living) GhostAttachments.loaded(living);
    }
    /** 卸载保留存档事实，仅释放运行时索引。 */
    @SubscribeEvent public static void left(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living) GhostAttachments.unloaded(living);
    }
    /** 后来的观察者也必须看到已有插入模型。 */
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof LivingEntity target && event.getEntity() instanceof ServerPlayer player)
            GhostAttachmentNetwork.syncTo(target, player);
    }
    /** 玩家登录及换维度时为本人补齐附着快照。 */
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) GhostAttachmentNetwork.syncTo(player, player);
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) GhostAttachmentNetwork.syncTo(player, player);
    }
    /** 真正死亡后返还外来物品；不在可取消的伤害前处理阶段提前拔钉。 */
    @SubscribeEvent public static void drops(LivingDropsEvent event) {
        var entity = event.getEntity();
        for (var stack : GhostAttachments.detachAll(entity))
            event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), stack));
    }
}
