package com.qidate.qisplan2.ghost.module.host;

import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import com.qidate.qisplan2.ghost.module.suppression.GhostSuppressionLedger;
import com.qidate.qisplan2.ghost.module.suppression.GhostSuppressionState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** 将已成立的声音传播媒介适配为持续压制关系；不识别钢琴或任何具体厉鬼。 */
public final class GhostSoundSuppression {
    private static final Map<UUID, GhostSuppressionLedger> TARGETS = new HashMap<>();
    private GhostSoundSuppression() {}

    /** 仅为承载模块的听众建立关系，普通动物仍接收声音语义事件。 */
    public static void update(UUID source, GhostModuleHost origin, ResourceLocation module,
                              ResourceLocation sound, LivingEntity target) {
        if (module == null) { remove(target.getUUID(), source); return; }
        var definition = GhostModuleRegistry.get(module);
        var targetHost = host(target);
        if (definition == null || !origin.carriesModule(module) || targetHost == null || targetHost.getModules().isEmpty()) {
            remove(target.getUUID(), source); return;
        }
        var offer = definition.soundSuppression(new GhostModuleContext(origin, module), sound);
        if (offer == null) { remove(target.getUUID(), source); return; }
        // 身体锚点相同不等于体内来源：手持物品仍然是外来来源。
        boolean intrinsic = origin == target || origin instanceof PlayerGhostModuleHost player && player.player() == target;
        var relation = new GhostSuppressionLedger.Relation(offer,
                intrinsic && target instanceof Player, intrinsic ? module : null);
        TARGETS.computeIfAbsent(target.getUUID(), ignored -> new GhostSuppressionLedger()).put(source, relation);
    }
    /** 按具体声源撤销，不解除目标上的其他音乐或棺材钉。 */
    public static void remove(UUID target, UUID source) {
        var ledger = TARGETS.get(target);
        if (ledger == null) return;
        ledger.remove(source);
        if (ledger.isEmpty()) TARGETS.remove(target);
    }
    /** 目标的持有模块可以变化，因此只在查询时读取当前模块本质强度。 */
    public static GhostSuppressionState state(LivingEntity target, ResourceLocation module) {
        if (target.level().isClientSide()) return GhostSuppressionState.NONE;
        var ledger = TARGETS.get(target.getUUID());
        var host = host(target);
        if (ledger == null || host == null) return GhostSuppressionState.NONE;
        double strength = host.getModules().stream().filter(e -> e.id().equals(module)).mapToDouble(e -> e.intensity()).findFirst().orElse(0);
        return ledger.state(module, strength);
    }
    /** 只在适配层解释玩家与实体宿主，模块不接触任何载体身份。 */
    private static GhostModuleHost host(LivingEntity target) {
        return target instanceof Player player ? new PlayerGhostModuleHost(player)
                : target instanceof GhostModuleHost host ? host : null;
    }
    /** 目标离线、卸载或死亡时释放临时媒介关系；重新听到时自然重建。 */
    public static void removeTarget(UUID target) { TARGETS.remove(target); }
    /** 服务器关闭时清理，避免同进程重进存档残留关系。 */
    public static void clear() { TARGETS.clear(); }
}
