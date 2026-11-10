package com.qidate.qisplan2.ghost.module.host.entity;
import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.event.DoorKnockHeardEvent;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluence;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceSession;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import static com.qidate.qisplan2.QisPlan2.MODID;
import com.qidate.qisplan2.ghost.module.implementation.knocking.*;

public final class KnockingGhostEntityBinding {
    private static final ResourceLocation ID = KnockingGhostModule.ID;
    private static final ResourceLocation KNOCK = KnockingGhostModule.KNOCK;
    private static final ResourceLocation ATTACK = KnockingGhostModule.ATTACK;

    /**
     * 为当前实体创建本模块独立的 Goal 实例。
     * 行为实例不得跨实体共享；需要的持久状态仍保存在模块 customData。
     */
    public List<Goal> createEntityGoals(ModularGhostEntity host) { return KnockingGhostBehavior.create(host); }

    /**
     * 检查实体及对应模块是否仍具备运行条件。
     * 行为在开始或继续执行前使用此检查，避免已失效模块继续操作世界。
     */
    public static boolean isActive(ModularGhostEntity host) {
        return host.isAlive() && !host.isSupernaturallyStunned()
                && host.getModules().stream().anyMatch(e -> e.id().equals(ID) && GhostModuleCombatState.available(e));
    }

    /**
     * 发起敲门的世界操作，声音传播与后续听声处理由敲门系统负责。
     */
    public static void knock(ModularGhostEntity host, BlockPos door) {
        if (isActive(host)) GhostAbilityExecutor.tryExecute(new GhostAbilityContext(
                host, ID, KNOCK, GhostAbilityContext.Source.BEHAVIOR, null, () -> {}, new DoorKnockParameters(door)));
    }

    /**
     * 将玩家敲门产生的听声事实接入主动能力链。
     * 准备袭击、提交灵异裁决，获准后安排延迟执行；不重新走自动规则。
     */
    public static void attackFromPlayer(ServerPlayer player, DoorKnockHeardEvent heard) {
        var host = new PlayerGhostModuleHost(player);
        if (!host.hasModule(ID)) return;
        Runnable attack = GhostAbilityExecutor.prepareDeferred(new GhostAbilityContext(
                host, ID, ATTACK, GhostAbilityContext.Source.POSSESSION, heard));
        if (attack == null) return;
        GhostInfluenceSession session = new GhostInfluenceSession();
        session.submit(new GhostInfluence(host, ID, ATTACK, heard.target().getUUID(),
                GhostInfluenceType.ATTACK, GhostModuleHosts.strength(host, ID),
                session.getSourceId(host, ID), session.getBatchId()),
                () -> KnockingGhostDoorSystem.scheduleModuleAttack(attack));
        session.resolveAndApply();
    }
}
