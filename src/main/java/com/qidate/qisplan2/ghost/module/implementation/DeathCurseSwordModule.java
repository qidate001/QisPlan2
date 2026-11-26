package com.qidate.qisplan2.ghost.module.implementation;

import java.util.Set;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.environment.GhostDeathCurseAccess;
import com.qidate.qisplan2.ghost.module.event.*;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

/** 完整必死诅咒之剑：命中叠层、十层袭击、半数概率反噬。模块不识别自己的载体。 */
public final class DeathCurseSwordModule implements GhostModule {
    public static final ResourceLocation ID = ResourceLocation.parse("qisplan2:death_curse_sword");
    public static final ResourceLocation CURSE = ResourceLocation.parse("qisplan2:death_curse_sword/curse");
    public static final int THRESHOLD = 10;
    /** 返回模块身份。 */
    public ResourceLocation getId() { return ID; }
    /** 保留旧灵异袭击强度 3，后续读取模块实例强度。 */
    public GhostModuleInitialData getInitialData() { return new GhostModuleInitialData(3,0); }
    /** 近战规则只接受真实命中，不接受通用接触和手持采样。 */
    public List<GhostRule> getRules() { return List.of(new GhostRule() {
        /** 返回命中规则标识。 */
        public ResourceLocation getId() { return ResourceLocation.parse("qisplan2:death_curse_sword/hit"); }
        /** 普通接触不会叠层，避免与物品接触桥重复触发。 */
        public Set<Class<? extends GhostEvent>> eventTypes() { return Set.of(MeleeHitEvent.class); }

        /** 媒介由统一执行器验证。 */
        public boolean canTrigger(GhostRuleContext c,GhostEvent e) { return true; }
        /** 调用相同的诅咒能力，规则死机由运行时统一过滤。 */
        public void onTrigger(GhostRuleContext c,GhostEvent e) { var run=c.prepareAbility(CURSE,e); if (run!=null) run.run(); }
    }); }
    /** 命中目标来自事实，主动施咒则选择近距离瞄准目标。 */
    private static GhostDeathCurseAccess.Subject target(GhostAbilityContext c) {
        var world=c.getModuleContext().world();
        return world.deathCurses().target(c.event() instanceof MeleeHitEvent hit ? hit.target() : world.aimedTarget(3));
    }
    /** 叠层和阈值属于模块规则；状态存取、裁决与伤害表现由不透明目标出口完成。 */
    public static boolean addCurse(GhostDeathCurseAccess.Subject subject, GhostAbilityContext context, double intensity, boolean backlash) {
        if (subject==null) return false;
        int next=Math.min(THRESHOLD,Math.max(0,subject.stacks()));
        if (next<THRESHOLD) next++;
        subject.stacks(next>=THRESHOLD ? 0 : next);
        if (next<THRESHOLD) return false;
        subject.strike(context,intensity,backlash); return true;
    }
    /** 公开施咒能力；不公开一个绕开十层条件的即死技能。 */
    public List<GhostAbility> getIntrinsicAbilities() { return List.of(new GhostAbility() {
        /** 返回施咒能力标识。 */
        public ResourceLocation getId() { return CURSE; }
        /** 使用剑的图标。 */
        public ResourceLocation iconTexture() { return ResourceLocation.parse("minecraft:textures/item/wooden_sword.png"); }
        /** 无论自动命中或主动调用，都必须存在合法近距离目标。 */
        public List<GhostMedium> media(GhostAbilityContext ignored) { return List.of(new GhostMedium() {
            /** 返回媒介标识。 */
            public ResourceLocation getId() { return ResourceLocation.parse("qisplan2:death_curse_sword/near_target"); }
            /** 由世界出口验证距离、暴露、目标存活及屏蔽。 */
            public boolean allows(GhostAbilityContext c) { return target(c)!=null; }
        }); }
        /** 媒介负责目标资格验证。 */
        public boolean canExecute(GhostAbilityContext c) { return true; }
        /** 先完成双方叠层与裁决，最后只消耗一次来源，避免第一次破碎使反噬丢失。 */
        public void execute(GhostAbilityContext c) {
            var module=c.getModuleContext(); var access=module.world().deathCurses(); double strength=module.intensity();
            boolean exhausted=addCurse(target(c),c,strength,false);
            if (access.random()<.5) exhausted=addCurse(access.backlash(),c,strength,true) || exhausted;
            if (exhausted) access.exhaust();
        }
    }); }
}
