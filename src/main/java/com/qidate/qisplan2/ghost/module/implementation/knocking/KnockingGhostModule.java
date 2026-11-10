package com.qidate.qisplan2.ghost.module.implementation.knocking;
import com.qidate.qisplan2.ghost.module.environment.GhostBehavior;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.ghost.corrosion.CorrosionType;
import com.qidate.qisplan2.ghost.corrosion.GhostCorrosion;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.DoorKnockHeardEvent;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import com.qidate.qisplan2.ghost.possession.classification.GhostClassification;
import com.qidate.qisplan2.ghost.possession.classification.GhostTag;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import static com.qidate.qisplan2.QisPlan2.MODID;

/**
 * 完整的敲门鬼模块：行为负责找门与靠近，媒介限定有效门和敲门声事件，
 * 听声规则提交延迟袭击裁决，能力负责敲门、鬼门传播和获准后的灵异袭击。
 */
public final class KnockingGhostModule implements GhostModule {
    /**
     * 声明需要的自主行动类型。
     * 具体 Goal 的创建、资源竞争和停止清理由行动适配层负责。
     */
    @Override public GhostBehavior behavior() { return GhostBehavior.KNOCK_DOORS; }
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MODID, "knocking_ghost");
    public static final double INITIAL_INTENSITY = 5.0D;
    public static final ResourceLocation KNOCK = ResourceLocation.fromNamespaceAndPath(MODID, "knocking_ghost/knock");
    public static final ResourceLocation ATTACK = ResourceLocation.fromNamespaceAndPath(MODID, "knocking_ghost/attack");
    private static final ResourceLocation RULE = ResourceLocation.fromNamespaceAndPath(MODID, "knocking_ghost/hearing");
    private static final ResourceLocation MEDIUM = ResourceLocation.fromNamespaceAndPath(MODID, "knocking_ghost/door_sound");

    /**
     * 获取当前定义的唯一标识。
     * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
     */
    @Override public ResourceLocation getId() { return ID; }

    /**
     * 提供新建模块实例的初始强度和复苏值。
     * 仅用于初始化；已存在实例以保存的模块数据为准。
     */
    @Override public GhostModuleInitialData getInitialData() { return new GhostModuleInitialData(INITIAL_INTENSITY, 0); }

    /**
     * 返回灵异防御值，供伤害结算使用。
     */
    @Override public double getSupernaturalDefense() { return 6; }


    /** 玩家敲门产生的后续袭击属于主动能力链，不重新经过自动听声规则。 */

    @Override public List<GhostMedium> getMedia() {
        return List.of(new GhostMedium() {
            /**
             * 获取当前定义的唯一标识。
             * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
             */
            @Override public ResourceLocation getId() { return MEDIUM; }

            /**
             * 校验本媒介要求的环境、事件或输入条件。
             * 返回 false 会阻止本次能力执行，主动使用也不能绕过该检查。
             */
            @Override public boolean allows(GhostAbilityContext c) {
                var world = c.getModuleContext().world();
                if (!world.present() || !c.isModuleAvailable()) return false;
                if (c.parameters() instanceof DoorKnockParameters e) return c.abilityId().equals(KNOCK)
                        && world.isDoor(e.door());
                return c.abilityId().equals(ATTACK) && c.event() instanceof DoorKnockHeardEvent e
                        && world.isOrigin(e.source()) && e.target().isAlive() && world.sameWorld(e.target().level());
            }
        });
    }

    /**
     * 声明本厉鬼响应的语义事件及对应规则。
     * 规则负责决定何时发起能力或提交影响，不绕过执行器直接完成袭击。
     */
    @Override public List<GhostRule> getRules() {
        return List.of(new GhostRule() {
            /**
             * 获取当前定义的唯一标识。
             * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
             */
            @Override public ResourceLocation getId() { return RULE; }

            /**
             * 判断当前语义事件是否属于本规则处理的范围。
             * 这里只筛选事件种类或语义，不执行能力和世界操作。
             */
            @Override public boolean supports(GhostEvent event) { return event instanceof DoorKnockHeardEvent; }

            /**
             * 检查当前事实和模块上下文是否满足规则触发条件。
             * 返回 false 时跳过本规则，不提交能力或灵异影响。
             */
            @Override public boolean canTrigger(GhostRuleContext c, GhostEvent e) {
                return c.getModuleContext().world().present()
                        && e instanceof DoorKnockHeardEvent heard && c.getModuleContext().world().isOrigin(heard.source());
            }

            /**
             * 响应听见敲门的事实，准备可重新校验的延迟袭击。
             * 先提交灵异裁决，只有获准后才把袭击加入延迟队列。
             */
            @Override public void onTrigger(GhostRuleContext c, GhostEvent event) {
                DoorKnockHeardEvent e = (DoorKnockHeardEvent) event;
                // 听见声音时立即处理规则并提交裁决。获准效果安排延迟能力调用，
                // 延迟结束不再制造事件；执行器会重新检查模块、媒介与能力。
                Runnable attack = c.prepareAbility(ATTACK, e);
                if (attack != null) c.submitInfluence(e.target().getUUID(), GhostInfluenceType.ATTACK,
                        c.getModuleContext().effectiveIntensity(),
                        () -> c.getModuleContext().world().scheduleKnockAttack(attack));
            }
        });
    }

    /**
     * 返回本厉鬼自身的能力定义。
     * 世界效果由这些能力执行，输入桥和行为适配器只负责发起调用。
     */
    @Override public List<GhostAbility> getIntrinsicAbilities() {
        return List.of(new GhostAbility() {
            /**
             * 获取当前定义的唯一标识。
             * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
             */
            @Override public ResourceLocation getId() { return KNOCK; }

            /**
             * 检查本次能力调用的事件、参数或环境是否可用于执行。
             * 模块可用性与媒介由统一执行器共同校验。
             */
            @Override public boolean canExecute(GhostAbilityContext c) { return c.parameters() instanceof DoorKnockParameters; }

            /**
             * 执行当前能力的效果。
             * 调用方应使用统一执行器，以确保模块状态和媒介已经检查。
             */
            @Override public void execute(GhostAbilityContext c) {
                var world = c.getModuleContext().world();
                world.knock(((DoorKnockParameters)c.parameters()).door(), c.getModuleContext().effectiveIntensity());
            }
        }, new GhostAbility() {
            /**
             * 获取当前定义的唯一标识。
             * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
             */
            @Override public ResourceLocation getId() { return ATTACK; }

            /**
             * 检查本次能力调用的事件、参数或环境是否可用于执行。
             * 模块可用性与媒介由统一执行器共同校验。
             */
            @Override public boolean canExecute(GhostAbilityContext c) { return c.event() instanceof DoorKnockHeardEvent; }

            /**
             * 执行当前能力的效果。
             * 调用方应使用统一执行器，以确保模块状态和媒介已经检查。
             */
            @Override public void execute(GhostAbilityContext c) { executeWithResult(c); }

            /**
             * 执行能力并返回实际效果是否成功。
             * 返回值供主动交互及成功后结算使用，不能等同于仅接受了调用。
             */
            @Override public boolean executeWithResult(GhostAbilityContext c) {
                DoorKnockHeardEvent e = (DoorKnockHeardEvent)c.event();
                return c.getModuleContext().world().attack(e.target(),
                        ModDamageTypes.KNOCKING_GHOST, c.getModuleContext().effectiveIntensity());
            }
        });
    }

    /**
     * 10 秒。
     */
    private static final GhostCorrosion CORROSION =
            GhostCorrosion.builder()
                    .add(CorrosionType.GLOBAL, 20)
                    .add(CorrosionType.HAND, 20)
                    .build();


    /**
     * 灵异侵蚀
     */
    @Override
    public GhostCorrosion corrosion() {
        return CORROSION;
    }

    /**
     * 厉鬼标签
     */
    @Override
    public GhostClassification classification() {

        return GhostClassification.of(
                GhostTag.ENTITY,
                GhostTag.EXISTENCE_FORM,
                GhostTag.DOOR
        );
    }

    /**
     * 声明能够压制的目标标签；空集合表示没有额外的标签压制对象。
     */
    @Override
    public GhostClassification suppressionTargets() {
        return GhostClassification.empty();
    }

    /**
     * 返回可分配的压制单位数量，供驾驭压制配置使用。
     */
    @Override
    public int suppressionUnits() {
        return 1;
    }

    /**
     * 返回每个压制单位对应的基础灵异强度。
     */
    @Override
    public double suppressionUnitStrength() {
        return 20.0D;
    }


}
