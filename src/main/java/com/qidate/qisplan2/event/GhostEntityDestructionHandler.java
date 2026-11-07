package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.event.EntityDestructionAttemptEvent;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** 把原版死亡与模组灵异秒杀统一桥接到“实体摧毁尝试”语义。 */
@EventBusSubscriber(modid = QisPlan2.MODID)
public final class GhostEntityDestructionHandler {
    private GhostEntityDestructionHandler() {}

    // 同一次调用链中，每个受害宿主只处理一次，阻止两只鬼石砖互相反击造成无限递归。
    private static final ThreadLocal<Set<LivingEntity>> ACTIVE =
            ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || event.isCanceled() || ACTIVE.get().contains(entity)) return;
        withAttempt(entity, event.getSource(), () -> {
            event.setCanceled(true);
            // 死亡事件可能在生命值已归零后触发。取消事件本身不会恢复生命值。
            entity.setHealth(Math.max(entity.getHealth(), Math.min(1.0F, entity.getMaxHealth())));
            return false;
        }, () -> true);
    }

    /**
     * 灵异秒杀在真正伤害及强制死亡前处理一次摧毁尝试。
     * 同一伤害随后触发原版死亡事件时不重复反击；被取消则完全跳过强制死亡。
     */
    public static boolean executeLethalAttack(LivingEntity entity, DamageSource source,
            BooleanSupplier attack) {
        return withAttempt(entity, source, () -> false, attack);
    }

    private static boolean withAttempt(LivingEntity entity, DamageSource source,
            BooleanSupplier prevented, BooleanSupplier allowed) {
        Set<LivingEntity> active = ACTIVE.get();
        if (!active.add(entity)) return allowed.getAsBoolean();
        try {
            GhostModuleHost host = entity instanceof Player player ? new PlayerGhostModuleHost(player)
                    : entity instanceof GhostModuleHost modules ? modules : null;
            if (host != null && !host.getModules().isEmpty()) {
                var attempt = new EntityDestructionAttemptEvent(entity, source);
                GhostModuleRuntime.dispatch(host, attempt);
                if (attempt.isCanceled()) return prevented.getAsBoolean();
            }
            return allowed.getAsBoolean();
        } finally {
            active.remove(entity);
            if (active.isEmpty()) ACTIVE.remove();
        }
    }
}
