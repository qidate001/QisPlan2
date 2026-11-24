package com.qidate.qisplan2.ghost.module.implementation.calling;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.environment.GhostBehavior;
import com.qidate.qisplan2.ghost.module.event.*;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;

/** 喊名是持续能力；目标、起始视角、喊名次数和转向进度全部属于模块实例。 */
public final class CallingGhostModule implements GhostModule {
    public static final ResourceLocation ID = id("calling_ghost");
    public static final ResourceLocation FOLLOW = id("calling_ghost/follow_call");
    public static final ResourceLocation CANCEL = id("calling_ghost/cancel_follow");
    public static final ResourceLocation ATTACK = id("calling_ghost/attack");
    public static final double INITIAL_INTENSITY = 30;
    /** 标识工具不包含宿主信息。 */
    private static ResourceLocation id(String name) { return ResourceLocation.fromNamespaceAndPath("qisplan2", name); }
    /** 自主实体行为只负责选目标和取得移动控制。 */
    public GhostBehavior behavior() { return context -> { var actor=context.world().actor(); if(actor==null)return List.of(); CallingGhostState.initialize(actor); return List.of(new CallingGhostBehavior(actor)); }; }
    /** 返回能力或模块的稳定标识。 */
    public ResourceLocation getId() { return ID; }
    /** 提供新实例的初始强度和复苏。 */
    public GhostModuleInitialData getInitialData() { return new GhostModuleInitialData(INITIAL_INTENSITY, 0); }
    /** 后方偏移只用开始追踪时的水平朝向，目标转头不会使来源绕到另一边。 */
    public static Vec3 behindOffset(float initialYaw) {
        double radians = Math.toRadians(initialYaw);
        return new Vec3(Math.sin(radians) * 2.5, 1, -Math.cos(radians) * 2.5);
    }
    /** 第一次七十度，后续逐次增强到完整半圈；实际转向始终朝来源且不越过。 */
    public static float turnAmount(int call) { return Math.min(180, Math.max(0, call) * 70F); }

    /** 公开喊名要求可移动、可追踪；私有袭击只能针对真实看见来源事件中的当前目标。 */
    public List<GhostMedium> getMedia() { return List.of(new GhostMedium() {
        /** 返回能力或模块的稳定标识。 */
        public ResourceLocation getId() { return id("calling_ghost/target_medium"); }
        /** 校验硬性媒介，不产生世界效果。 */
        public boolean allows(GhostAbilityContext c) {
            if (c.abilityId().equals(CANCEL)) return true;
            var world = c.getModuleContext().world();
            if (c.abilityId().equals(FOLLOW) && c.source() == GhostAbilityContext.Source.POSSESSION
                    && world.aimedTarget(32) == null) return true;
            LivingEntity target = c.source() == GhostAbilityContext.Source.POSSESSION ? world.aimedTarget(32) : world.target();
            if (!world.present() || !world.canTrack(target)) return false;
            if (c.abilityId().equals(FOLLOW)) return world.canMove();
            return c.event() instanceof SourceSeenEvent turn && turn.observer() == target
                    && world.sourceVisibleTo(target) && world.validTarget(target);
        }
    }); }

    /** 自主喊名观察到目标看见来源后，在当前规则批次提交袭击。 */
    public List<GhostRule> getRules() { return List.of(new GhostRule() {
        /** 返回能力或模块的稳定标识。 */
        public ResourceLocation getId() { return id("calling_ghost/turn_rule"); }
        /** 只筛选本规则能处理的语义事件。 */
        public boolean supports(GhostEvent e) { return e instanceof SourceSeenEvent || e instanceof ModuleTickEvent; }
        /** 判定本事件是否应触发自动规则。 */
        public boolean canTrigger(GhostRuleContext c, GhostEvent e) { return e instanceof SourceSeenEvent && !c.getModuleContext().getCustomData().getBoolean("manualCalling"); }
        /** 沉睡后停止跟随和扭头，但仍观察原目标是否真正看见来源。 */
        public boolean disturbedWhileSleeping(GhostRuleContext c, GhostEvent e) {
            var m = c.getModuleContext(); var target = m.world().target();
            return target != null && m.world().validTarget(target) && m.world().sourceVisibleTo(target);
        }
        /** 在当前事件会话中提交能力影响。 */
        public void onTrigger(GhostRuleContext c, GhostEvent e) {
            var turn = (SourceSeenEvent)e;
            c.submitAbilityInfluence(ATTACK, turn, turn.observer().getUUID(), GhostInfluenceType.ATTACK, c.getModuleContext().intensity());
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

    /** 对外提供喊名和取消追踪；空放喊名同样取消，袭击始终私有。 */
    public List<GhostAbility> getIntrinsicAbilities() { return List.of(new GhostAbility() {
        /** 返回能力或模块的稳定标识。 */
        public ResourceLocation getId() { return FOLLOW; }
        /** 声明技能使用的图标纹理。 */
        public ResourceLocation iconTexture() { return ResourceLocation.parse("minecraft:textures/item/name_tag.png"); }
        /** 检查本次执行所需的环境与语义条件。 */
        public boolean canExecute(GhostAbilityContext c) { return c.source() == GhostAbilityContext.Source.POSSESSION || c.getModuleContext().world().canMove(); }
        /** 在通过统一执行器后更新本能力的状态与效果。 */
        public void execute(GhostAbilityContext c) {
            var context = c.getModuleContext();
            if (c.source() == GhostAbilityContext.Source.POSSESSION) {
                var target = context.world().aimedTarget(32);
                if (target == null) { finish(context, context.world().target()); return; }
                var data = context.getCustomData();
                data.putUUID("target", target.getUUID()); data.putBoolean("manualCalling", true);
                data.putBoolean("callInitialized", false); data.remove("lastFollowTick"); context.setCustomData(data);
            }
            step(context);
        }
    }, new GhostAbility() {
        /** 取消不需要目标、视线或移动媒介，随时可以结束追踪。 */
        public ResourceLocation getId() { return CANCEL; }
        /** 取消操作不继承追踪的媒介。 */
        public boolean inheritsModuleMedia() { return false; }
        /** 取消使用独立图标。 */
        public ResourceLocation iconTexture() { return ResourceLocation.parse("minecraft:textures/item/barrier.png"); }
        /** 没有目标时重复取消也有效。 */
        public boolean canExecute(GhostAbilityContext c) { return true; }
        /** 立即清空目标及尚未执行的视角牵引。 */
        public void execute(GhostAbilityContext c) { finish(c.getModuleContext(), c.getModuleContext().world().target()); }
    }, new GhostAbility() {
        /** 返回能力或模块的稳定标识。 */
        public ResourceLocation getId() { return ATTACK; }
        /** 内部袭击不作为玩家可直接释放的技能。 */
        public boolean isPublicSkill() { return false; }
        /** 声明技能使用的图标纹理。 */
        public ResourceLocation iconTexture() { return ResourceLocation.parse("minecraft:textures/item/name_tag.png"); }
        /** 检查本次执行所需的环境与语义条件。 */
        public boolean canExecute(GhostAbilityContext c) { return c.event() instanceof SourceSeenEvent turn && c.getModuleContext().world().validTarget(turn.observer()); }
        /** 为持续能力的袭击声明真实目标与强度。 */
        public GhostAbilityInfluence activeInfluence(GhostAbilityContext c) {
            return GhostAbilityInfluence.of(c.event() instanceof SourceSeenEvent turn ? turn.observer() : null,
                    GhostInfluenceType.ATTACK, c.getModuleContext().intensity());
        }
        /** 在通过统一执行器后更新本能力的状态与效果。 */
        public void execute(GhostAbilityContext c) { executeWithResult(c); }
        /** 执行效果并返回真实结果。 */
        public boolean executeWithResult(GhostAbilityContext c) {
            if (!(c.event() instanceof SourceSeenEvent turn)) return false;
            return c.getModuleContext().world().attack(turn.observer(), ModDamageTypes.CALLING_GHOST, c.getModuleContext().intensity());
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
        // 先按固定世界方向跟随位置，再判定目标能否真正看见来源。
        var behind = target.position().add(behindOffset(data.getFloat("initialYaw")));
        world.teleport(behind.x, behind.y, behind.z, target);
        if (attackIfSeen(context, target, data)) return;
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
            float turn = Math.min(Math.min(12, 4 + data.getInt("callCount") * 2), data.getFloat("pendingTurn"));
            float moved = turn > 0 ? world.turnTargetTowardSource(target, turn) : 0;
            data.putFloat("pendingTurn", Math.max(0, data.getFloat("pendingTurn") - moved));
        } else data.putFloat("pendingTurn", 0);
        context.setCustomData(data);
        // 强制视角已同步到服务端，立即判断，不等下一 tick 被新的移动包覆盖。
        attackIfSeen(context, target, data);
    }

    /** 只在来源实际进入视野且无遮挡时发布看见事实，随后统一裁决并结束本轮。 */
    private static boolean attackIfSeen(GhostModuleContext context, LivingEntity target, CompoundTag data) {
        if (!context.world().sourceVisibleTo(target)) return false;
        context.setCustomData(data);
        var event = new SourceSeenEvent(target);
        if (data.getBoolean("manualCalling")) context.continueAbility(ATTACK, event);
        else context.dispatchEvent(event);
        finish(context, target);
        return true;
    }

    /** 无论袭击是否成功都结束本轮，短暂避免再次选择同一目标。 */
    private static void finish(GhostModuleContext context, LivingEntity target) {
        var data = context.getCustomData();
        data.remove("target"); data.putBoolean("manualCalling", false); data.putBoolean("callInitialized", false);
        data.putFloat("pendingTurn", 0); data.putBoolean("heardCall", false);
        data.remove("lastFollowTick"); data.remove("initialYaw"); data.putInt("callCount", 0); data.putInt("callCooldown", 0);
        if (target != null) { data.putUUID("recentlyAttacked", target.getUUID()); data.putInt("attackTargetCooldown", 100); }
        context.setCustomData(data);
    }
}
