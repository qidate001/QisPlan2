package com.qidate.qisplan2.ghost.module.implementation.calling;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.environment.GhostBehavior;
import com.qidate.qisplan2.ghost.module.event.*;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;

/** 喊名是持续能力；目标、起始视角、喊名次数和转向进度全部属于模块实例。 */
public final class CallingGhostModule implements GhostModule {
    public static final ResourceLocation ID = id("calling_ghost");
    public static final ResourceLocation FOLLOW = id("calling_ghost/follow_call");
    public static final ResourceLocation ATTACK = id("calling_ghost/attack");
    public static final double INITIAL_INTENSITY = 30;
    /** 标识工具不包含宿主信息。 */
    private static ResourceLocation id(String name) { return ResourceLocation.fromNamespaceAndPath("qisplan2", name); }
    /** 自主实体行为只负责选目标和取得移动控制。 */
    public GhostBehavior behavior() { return GhostBehavior.FOLLOW_CALL; }
    /** 返回能力或模块的稳定标识。 */
    public ResourceLocation getId() { return ID; }
    /** 提供新实例的初始强度和复苏。 */
    public GhostModuleInitialData getInitialData() { return new GhostModuleInitialData(INITIAL_INTENSITY, 0); }
    /** 必须接近完全回头，允许十五度视角误差；不是相邻两帧转过四十五度。 */
    public static boolean isTurnAround(float initial, float current) { return Math.abs(Mth.wrapDegrees(current - initial)) >= 165; }
    /** 每次喊名的累计转动量递增，最长一次九十度，逐 tick 推进。 */
    public static float turnAmount(int call) { return Math.min(90, Math.max(0, call) * 10F); }

    /** 公开喊名要求可移动、可追踪；私有袭击只能针对真实回头事件中的当前目标。 */
    public List<GhostMedium> getMedia() { return List.of(new GhostMedium() {
        /** 返回能力或模块的稳定标识。 */
        public ResourceLocation getId() { return id("calling_ghost/target_medium"); }
        /** 校验硬性媒介，不产生世界效果。 */
        public boolean allows(GhostAbilityContext c) {
            var world = c.getModuleContext().world();
            LivingEntity target = c.source() == GhostAbilityContext.Source.POSSESSION ? world.aimedTarget(32) : world.target();
            if (!world.present() || !world.canTrack(target)) return false;
            if (c.abilityId().equals(FOLLOW)) return world.canMove();
            return c.event() instanceof TargetTurnedAroundEvent turn && turn.target() == target
                    && c.getModuleContext().getCustomData().getBoolean("heardCall")
                    && isTurnAround(turn.initialYaw(), turn.currentYaw()) && world.validTarget(target);
        }
    }); }

    /** 自主喊名观察到回头后，在当前规则批次提交袭击。 */
    public List<GhostRule> getRules() { return List.of(new GhostRule() {
        /** 返回能力或模块的稳定标识。 */
        public ResourceLocation getId() { return id("calling_ghost/turn_rule"); }
        /** 只筛选本规则能处理的语义事件。 */
        public boolean supports(GhostEvent e) { return e instanceof TargetTurnedAroundEvent; }
        /** 判定本事件是否应触发自动规则。 */
        public boolean canTrigger(GhostRuleContext c, GhostEvent e) { return !c.getModuleContext().getCustomData().getBoolean("manualCalling"); }
        /** 在当前事件会话中提交能力影响。 */
        public void onTrigger(GhostRuleContext c, GhostEvent e) {
            var turn = (TargetTurnedAroundEvent)e;
            c.submitAbilityInfluence(ATTACK, turn, turn.target().getUUID(), GhostInfluenceType.ATTACK, c.getModuleContext().intensity());
        }
    }); }

    /** 持续主动喊名不依赖自动规则开关；目标失效或遇到空间阻隔时结束。 */
    public void tickAbilities(GhostModuleContext context) {
        var data = context.getCustomData();
        int cooldown = Math.max(0, data.getInt("attackTargetCooldown") - 1);
        data.putInt("attackTargetCooldown", cooldown);
        if (cooldown == 0) data.remove("recentlyAttacked");
        context.setCustomData(data);
        if (data.getBoolean("manualCalling")) {
            var target = context.world().target();
            if (target == null || !context.world().canTrack(target) || !context.world().canMove()) { finish(context, target); return; }
            context.continueAbility(FOLLOW, null);
        }
    }

    /** 对外只提供喊名；攻击不接受自由瞄准目标。 */
    public List<GhostAbility> getIntrinsicAbilities() { return List.of(new GhostAbility() {
        /** 返回能力或模块的稳定标识。 */
        public ResourceLocation getId() { return FOLLOW; }
        /** 声明技能使用的图标纹理。 */
        public ResourceLocation iconTexture() { return ResourceLocation.parse("minecraft:textures/item/name_tag.png"); }
        /** 检查本次执行所需的环境与语义条件。 */
        public boolean canExecute(GhostAbilityContext c) { return c.getModuleContext().world().canMove(); }
        /** 在通过统一执行器后更新本能力的状态与效果。 */
        public void execute(GhostAbilityContext c) {
            var context = c.getModuleContext();
            if (c.source() == GhostAbilityContext.Source.POSSESSION) {
                var target = context.world().aimedTarget(32);
                if (target == null) return;
                var data = context.getCustomData();
                data.putUUID("target", target.getUUID()); data.putBoolean("manualCalling", true);
                data.putBoolean("callInitialized", false); context.setCustomData(data);
            }
            step(context);
        }
    }, new GhostAbility() {
        /** 返回能力或模块的稳定标识。 */
        public ResourceLocation getId() { return ATTACK; }
        /** 内部袭击不作为玩家可直接释放的技能。 */
        public boolean isPublicSkill() { return false; }
        /** 声明技能使用的图标纹理。 */
        public ResourceLocation iconTexture() { return ResourceLocation.parse("minecraft:textures/item/name_tag.png"); }
        /** 检查本次执行所需的环境与语义条件。 */
        public boolean canExecute(GhostAbilityContext c) { return c.event() instanceof TargetTurnedAroundEvent turn && c.getModuleContext().world().validTarget(turn.target()); }
        /** 为持续能力的袭击声明真实目标与强度。 */
        public GhostAbilityInfluence activeInfluence(GhostAbilityContext c) {
            return GhostAbilityInfluence.of(c.event() instanceof TargetTurnedAroundEvent turn ? turn.target() : null,
                    GhostInfluenceType.ATTACK, c.getModuleContext().intensity());
        }
        /** 在通过统一执行器后更新本能力的状态与效果。 */
        public void execute(GhostAbilityContext c) { executeWithResult(c); }
        /** 执行效果并返回真实结果。 */
        public boolean executeWithResult(GhostAbilityContext c) {
            if (!(c.event() instanceof TargetTurnedAroundEvent turn)) return false;
            return c.getModuleContext().world().attack(turn.target(), ModDamageTypes.CALLING_GHOST, c.getModuleContext().intensity());
        }
    }); }

    /** 推进一次持续喊名；每 tick 最多一次，转向基线始终保持为本轮开始时的朝向。 */
    private static void step(GhostModuleContext context) {
        var world = context.world(); var target = world.target();
        if (target == null || !world.canTrack(target)) { finish(context, target); return; }
        var data = context.getCustomData();
        long now = world.gameTime();
        if (data.contains("lastFollowTick") && data.getLong("lastFollowTick") == now) return;
        data.putLong("lastFollowTick", now);
        if (!data.getBoolean("callInitialized")) {
            data.putBoolean("callInitialized", true); data.putFloat("initialYaw", target.getYRot());
            data.putInt("callCount", 0); data.putInt("callCooldown", 0);
            data.putFloat("pendingTurn", 0); data.putBoolean("heardCall", false);
        }
        if (data.getBoolean("heardCall") && isTurnAround(data.getFloat("initialYaw"), target.getYRot())) {
            context.setCustomData(data);
            var event = new TargetTurnedAroundEvent(target, data.getFloat("initialYaw"), target.getYRot());
            if (data.getBoolean("manualCalling")) context.continueAbility(ATTACK, event);
            else context.dispatchEvent(event);
            finish(context, target); return;
        }
        var look = target.getLookAngle();
        var behind = target.position().add(-look.x * 2.5, 1, -look.z * 2.5);
        world.teleport(behind.x, behind.y, behind.z, target);
        int cooldown = Math.max(0, data.getInt("callCooldown") - 1);
        if (cooldown == 0) {
            cooldown = 100;
            if (world.canHearCall(target)) {
                int count = Math.min(1000, data.getInt("callCount") + 1);
                data.putInt("callCount", count); data.putBoolean("heardCall", true);
                data.putFloat("pendingTurn", data.getFloat("pendingTurn") + turnAmount(count));
                world.playCall(target);
            }
        }
        data.putInt("callCooldown", cooldown);
        if (world.canHearCall(target)) {
            float turn = Math.min(3, data.getFloat("pendingTurn"));
            if (turn > 0) world.turnTarget(target, turn);
            data.putFloat("pendingTurn", Math.max(0, data.getFloat("pendingTurn") - turn));
        } else data.putFloat("pendingTurn", 0);
        context.setCustomData(data);
    }

    /** 无论袭击是否成功都结束本轮，短暂避免再次选择同一目标。 */
    private static void finish(GhostModuleContext context, LivingEntity target) {
        var data = context.getCustomData();
        data.remove("target"); data.putBoolean("manualCalling", false); data.putBoolean("callInitialized", false);
        data.putFloat("pendingTurn", 0); data.putBoolean("heardCall", false);
        if (target != null) { data.putUUID("recentlyAttacked", target.getUUID()); data.putInt("attackTargetCooldown", 100); }
        context.setCustomData(data);
    }
}
