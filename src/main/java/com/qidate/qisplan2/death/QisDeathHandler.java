package com.qidate.qisplan2.death;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModAttachments;
import com.qidate.qisplan2.ghost.curse.CurseManager;
import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.GhostModuleCustomDataModifier;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import com.qidate.qisplan2.ghost.module.host.ModuleGhostDomains;
import com.qidate.qisplan2.ghost.possession.manager.GhostSuppressionAllocationHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** 所有已确认死亡统一进入 cleanup；子系统只提供清理操作，不再自行监听死亡。 */
@EventBusSubscriber(modid = QisPlan2.MODID)
public final class QisDeathHandler {
    /** 按实体实例去重；弱引用不阻止旧世界释放，新生身体不继承旧身体的处理标记。 */
    private static final Set<LivingEntity> CLEANED = Collections.newSetFromMap(new WeakHashMap<>());

    private QisDeathHandler() {}

    /** 掉落阶段已通过可取消的死亡检查；取消掉落不代表撤销死亡。 */
    @SubscribeEvent(receiveCanceled = true)
    public static void onConfirmedDeath(LivingDropsEvent event) {
        cleanup(event.getEntity());
    }

    /** 旁观者及仅设置生命值的死亡可能跳过掉落阶段，统一在实体 tick 结束时补齐。 */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity entity && !entity.level().isClientSide) {
            if (!entity.isAlive()) cleanup(entity);
            else CLEANED.remove(entity);
        }
    }

    /** 重生前确保旧身体已处理；末地返回等非死亡重建不执行死亡清理。 */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath() || event.getEntity().level().isClientSide) return;
        cleanup(event.getOriginal());
        event.getEntity().removeData(ModAttachments.DEATH_CURSE_COUNT.get());
    }

    /** 模组主动强制死亡也经过同一个入口，不要求调用方自行破体或清理诅咒。 */
    public static void forceKillAndCleanup(LivingEntity entity) {
        forceKill(entity);
    }

    /** 仅设置生命值的强制死亡不会触发原版死亡事件，因此在这里立即统一清理。 */
    public static void forceKill(LivingEntity entity) {
        if (entity.level().isClientSide || !entity.isAlive()) return;
        entity.setHealth(0.0F);
        cleanup(entity);
    }

    /**
     * 已确认死亡的唯一清理入口，对同一身体保持幂等。
     * 关闭模块鬼域、清理诅咒和压制分配，最后交由宿主释放其驾驭的模块。
     * 此方法不判断死亡能否取消；调用者必须在死亡成立之后调用。
     */
    public static void cleanup(LivingEntity entity) {
        if (entity.level().isClientSide || entity.getServer() == null || !CLEANED.add(entity)) return;
        CurseManager.onTargetDeath(entity.getServer(), entity.getUUID());
        entity.removeData(ModAttachments.DEATH_CURSE_COUNT.get());
        GhostModuleHost host = entity instanceof ServerPlayer player ? new PlayerGhostModuleHost(player)
                : entity instanceof GhostModuleHost modules ? modules : null;
        
        if (host != null) {
            for (var entry : host.getModules()) {
                if (host instanceof GhostModuleCustomDataModifier modifier) {
                    var custom = modifier.getModuleCustomData(entry.id());
                    if (custom.contains("domainOpen")) {
                        custom.putBoolean("domainOpen", false);
                        modifier.setModuleCustomData(entry.id(), custom);
                    }
                }
                ModuleGhostDomains.closeRuntime(host, entry.id());
            }
        }
        if (entity instanceof ServerPlayer player) {
            GhostSuppressionAllocationHandler.clear(player);
            ((PlayerGhostModuleHost) host).releaseOnDeath();
        }
    }
}