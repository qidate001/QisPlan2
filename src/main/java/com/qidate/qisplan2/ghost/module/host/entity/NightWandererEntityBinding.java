package com.qidate.qisplan2.ghost.module.host.entity;
import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.event.*;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import static com.qidate.qisplan2.QisPlan2.MODID;
import com.qidate.qisplan2.ghost.module.implementation.nightwanderer.*;

public final class NightWandererEntityBinding {
    private static final ResourceLocation ID = NightWandererModule.ID;
    private static final ResourceLocation ACTIVITY = NightWandererModule.ACTIVITY;
    private static final ResourceLocation ATTACK = NightWandererModule.ATTACK;
    private static final ResourceLocation GROWTH = NightWandererModule.GROWTH;

    /**
     * 为当前实体创建本模块独立的 Goal 实例。
     * 行为实例不得跨实体共享；需要的持久状态仍保存在模块 customData。
     */
    public List<Goal> createEntityGoals(ModularGhostEntity host) {
        NightWandererState.initialize(host);
        return List.of(new NightWandererBehavior(host));
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
     * 在模块有效时调用夜游鬼活动能力，不把能力调用包装成语义事件。
     */
    public static void activity(ModularGhostEntity host) {
        if (isActive(host)) GhostAbilityExecutor.tryExecute(new GhostAbilityContext(
                host, ID, ACTIVITY, GhostAbilityContext.Source.BEHAVIOR, null));
    }
}
