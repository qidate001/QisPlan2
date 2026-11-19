package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.event.*;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import com.qidate.qisplan2.death.ModDamageTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import java.util.List;

/** 完整鬼灶台：携食者靠近触发袭击，另提供独立的炼制工作台能力，不识别载体。 */
public final class GhostStoveModule implements GhostModule {
    public static final ResourceLocation ID = ResourceLocation.parse("qisplan2:ghost_stove");
    public static final ResourceLocation ATTACK = ResourceLocation.parse("qisplan2:ghost_stove/food_attack");
    public static final ResourceLocation WORKSTATION = ResourceLocation.parse("qisplan2:ghost_stove/workstation");
    /** 以环境位置为中心各延伸五格；适配器只负责平移与查询。 */
    public static final AABB AREA = new AABB(-5,-5,-5,5,5,5);
    /** 沿用旧默认灵异强度 1。 */
    public ResourceLocation getId() { return ID; }
    /** 工作台制作属于能力维护；死机禁止自动袭击，但不禁止已经投入的炼制。 */
    public void tickAbilities(GhostModuleContext context) { context.world().workstations().tick(ID); }
    /** 物品等交互适配器发布使用事实时，直接调用工作台能力，死机不禁止手动使用。 */
    public void onAbilityEvent(GhostModuleContext context, GhostEvent event) {
        if (event instanceof GhostUseEvent) context.continueAbility(WORKSTATION,event);
    }
    /** 目标必须仍在范围内、符合袭击资格并携带食物。 */
    private static boolean eligible(GhostModuleContext context, LivingEntity target) {
        return target != null && context.world().inArea(target,AREA)
                && context.world().hasCarriedItem(target, stack -> stack.getFoodProperties(target) != null);
    }
    /** 从语义或主动瞄准解析作用目标，不返回或识别来源载体。 */
    private static LivingEntity target(GhostAbilityContext context) {
        return context.event() instanceof FoodCarriedEvent event ? event.target() : context.getModuleContext().world().aimedTarget(8);
    }
    /** 工作台的交互者由可信的调用参数提供。 */
    private static LivingEntity user(GhostAbilityContext context) {
        return context.event() instanceof GhostUseEvent event ? event.user()
                : context.parameters() instanceof GhostInvocationParameters input ? input.recipient() : null;
    }
    /** 规则只查询一个局部 AABB，并把携食事实提交到共享灵异裁决会话。 */
    public List<GhostRule> getRules() { return List.of(new GhostRule() {
        /** 返回规则标识。 */
        public ResourceLocation getId() { return ResourceLocation.parse("qisplan2:ghost_stove/nearby_food"); }
        /** 接受世界周期观察。 */
        public boolean supports(GhostEvent event) { return event instanceof ModuleTickEvent; }
        /** 没有暴露到世界的模块不产生范围效果。 */
        public boolean canTrigger(GhostRuleContext c, GhostEvent event) { return c.getModuleContext().world().present(); }
        /** 仅附近确有携食实体才算触犯，单纯 Tick 不惊醒。 */
        public boolean disturbedWhileSleeping(GhostRuleContext c, GhostEvent event) {
            var m = c.getModuleContext();
            return m.world().present() && m.world().livingEntities(AREA).stream().anyMatch(target -> eligible(m, target));
        }
        /** 不扫描方块、不在模块中分辨玩家背包或其他载体。 */
        public void onTrigger(GhostRuleContext c, GhostEvent event) {
            var m = c.getModuleContext();
            for (var target : m.world().livingEntities(AREA)) if (eligible(m,target))
                c.submitAbilityInfluence(ATTACK,new FoodCarriedEvent(target),target.getUUID(),GhostInfluenceType.ATTACK,m.intensity());
        }
    }); }
    /** 两个独立公开能力分别声明自己的媒介，工作台不继承携食袭击条件。 */
    public List<GhostAbility> getIntrinsicAbilities() { return List.of(new GhostAbility() {
        /** 返回携食袭击标识。 */
        public ResourceLocation getId() { return ATTACK; }
        /** 显示食物主题图标。 */
        public ResourceLocation iconTexture() { return ResourceLocation.parse("minecraft:textures/item/bread.png"); }
        /** 主动袭击同样参加保护与死机裁决。 */
        public GhostAbilityInfluence activeInfluence(GhostAbilityContext c) {
            return GhostAbilityInfluence.of(target(c),GhostInfluenceType.ATTACK,c.getModuleContext().intensity());
        }
        /** 范围和食物是能力硬前提，延迟执行也会重新校验。 */
        public List<GhostMedium> media(GhostAbilityContext ignored) { return List.of(new GhostMedium() {
            /** 返回携食范围媒介标识。 */
            public ResourceLocation getId() { return ResourceLocation.parse("qisplan2:ghost_stove/food_in_range"); }
            /** 重查物品与范围，不信任此前观察到的事实。 */
            public boolean allows(GhostAbilityContext c) { return eligible(c.getModuleContext(),target(c)); }
        }); }
        /** 额外条件由媒介统一处理。 */
        public boolean canExecute(GhostAbilityContext c) { return true; }
        /** 兼容无结果调用。 */
        public void execute(GhostAbilityContext c) { executeWithResult(c); }
        /** 只有裁决获准后才播放表现并尝试击杀。 */
        public boolean executeWithResult(GhostAbilityContext c) {
            var m = c.getModuleContext(); var victim = target(c);
            m.world().attackFeedback(victim,ParticleTypes.SOUL,SoundEvents.SOUL_ESCAPE.value());
            return m.world().attack(victim,ModDamageTypes.GHOST_STOVE,m.intensity());
        }
    }, new GhostAbility() {
        /** 返回工作台能力标识。 */
        public ResourceLocation getId() { return WORKSTATION; }
        /** 使用已有物品图标。 */
        public ResourceLocation iconTexture() { return ResourceLocation.parse("minecraft:textures/item/blaze_powder.png"); }
        /** 工作台出口判断交互者、距离和实际承载状态。 */
        public boolean canExecute(GhostAbilityContext c) { return c.getModuleContext().world().workstations().canOpen(ID,user(c)); }
        /** 兼容无结果调用。 */
        public void execute(GhostAbilityContext c) { executeWithResult(c); }
        /** 玩家、实体或方块形态均通过同一工作台端口打开菜单。 */
        public boolean executeWithResult(GhostAbilityContext c) { return c.getModuleContext().world().workstations().open(ID,user(c)); }
    }); }
}
