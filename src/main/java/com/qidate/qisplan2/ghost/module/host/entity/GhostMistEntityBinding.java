package com.qidate.qisplan2.ghost.module.host.entity;
import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.entity.ai.GhostWanderGoal;
import com.qidate.qisplan2.ghost.domain.*;
import com.qidate.qisplan2.ghost.domain.type.mist.GhostMistDomainController;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.event.*;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import static com.qidate.qisplan2.QisPlan2.MODID;
import com.qidate.qisplan2.ghost.module.implementation.mist.*;

public final class GhostMistEntityBinding {
    private static final ResourceLocation ID = GhostMistModule.ID;
    private static final ResourceLocation DOMAIN = GhostMistModule.DOMAIN;
    private static final ResourceLocation ATTACK = GhostMistModule.ATTACK;

    /**
     * 为当前实体创建本模块独立的 Goal 实例。
     * 行为实例不得跨实体共享；需要的持久状态仍保存在模块 customData。
     */
    public List<Goal> createEntityGoals(ModularGhostEntity host) {
        GhostMistState.initialize(host);
        return List.of(new GhostWanderGoal(host, 0.7D) {
            /**
             * 判断当前行为是否可以开始执行。
             * 检查行为需要的模块状态、环境或目标条件，交由 Goal 调度器决定是否取得控制。
             */
            public boolean canUse() { return isActive(host) && super.canUse(); }

            /**
             * 重新检查行为是否仍可继续。
             * 条件不满足时由调度器停止行为，释放占用的控制资源。
             */
            public boolean canContinueToUse() { return isActive(host) && super.canContinueToUse(); }
        });
    }

    /**
     * 检查实体及对应模块是否仍具备运行条件。
     * 行为在开始或继续执行前使用此检查，避免已失效模块继续操作世界。
     */
    public static boolean isActive(ModularGhostEntity host) {
        return host.level() instanceof ServerLevel && host.isAlive() && !host.isRemoved()
                && !host.isCoffinNailed() && !host.isSupernaturallyStunned()
                && host.getModules().stream().anyMatch(e -> e.id().equals(ID) && GhostModuleCombatState.available(e));
    }

    /**
     * 维护实体来源的鬼域生命周期。
     * 模块失效时清理运行时资源；有效时通过统一执行器调用鬼域能力。
     */
    public void onEntityTick(ModularGhostEntity host) {
        if (!isActive(host)) {
            onEntityDetached(host);
            return;
        }
        GhostAbilityExecutor.tryExecute(new GhostAbilityContext(host, ID, DOMAIN,
                GhostAbilityContext.Source.BEHAVIOR, null));
    }

    /**
     * 移除当前实体来源的对应运行时鬼域。
     * 按模块类型清理，不误删同一实体的其他鬼域。
     */
    public void onEntityDetached(ModularGhostEntity host) {
        if (host.level() instanceof ServerLevel level) {
            // 只移除该宿主的鬼雾类型，保留其他模块可能拥有的鬼域。
            GhostDomainManager.get(level).removeBySourceAndType(host.getUUID(), GhostMistDomainController.DOMAIN_TYPE);
        }
    }

    /**
     * 查询当前模块记录的有效运行时对象，不存在时返回 null。
     */
    private static GhostDomain current(ModularGhostEntity host) {
        return GhostDomainManager.get((ServerLevel)host.level())
                .getBySourceAndType(host.getUUID(), GhostMistDomainController.DOMAIN_TYPE);
    }
}
