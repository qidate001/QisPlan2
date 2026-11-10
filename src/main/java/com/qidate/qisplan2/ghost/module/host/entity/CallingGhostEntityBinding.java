package com.qidate.qisplan2.ghost.module.host.entity;
import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.GhostModuleCombatState;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.event.*;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import static com.qidate.qisplan2.QisPlan2.MODID;
import com.qidate.qisplan2.ghost.module.implementation.calling.*;

public final class CallingGhostEntityBinding {
    private static final ResourceLocation ID = CallingGhostModule.ID;
    private static final ResourceLocation FOLLOW = CallingGhostModule.FOLLOW;
    private static final ResourceLocation ATTACK = CallingGhostModule.ATTACK;

    /**
     * 为当前实体创建本模块独立的 Goal 实例。
     * 行为实例不得跨实体共享；需要的持久状态仍保存在模块 customData。
     */
    public List<Goal> createEntityGoals(ModularGhostEntity host) {
        CallingGhostState.initialize(host);
        return List.of(new CallingGhostBehavior(host));
    }

    /**
     * 检查实体及对应模块是否仍具备运行条件。
     * 行为在开始或继续执行前使用此检查，避免已失效模块继续操作世界。
     */
    public static boolean isActive(ModularGhostEntity host) {
        return host.level() instanceof ServerLevel && host.isAlive() && !host.isCoffinNailed()
                && !host.isSupernaturallyStunned() && host.getModules().stream()
                .anyMatch(e -> e.id().equals(ID) && GhostModuleCombatState.available(e));
    }

    /**
     * 在模块有效时调用跟随喊名能力，具体效果仍由模块能力执行器处理。
     */
    public static void followAndCall(ModularGhostEntity host) {
        if (isActive(host)) GhostAbilityExecutor.tryExecute(new GhostAbilityContext(
                host, ID, FOLLOW, GhostAbilityContext.Source.BEHAVIOR, null));
    }
}
