package com.qidate.qisplan2.ghost.module.implementation.mist;
import com.qidate.qisplan2.ghost.module.environment.GhostBehavior;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.ghost.domain.*;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.*;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostDomainData;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import static com.qidate.qisplan2.QisPlan2.MODID;

/** 完整鬼雾模块：独立维护鬼域生命周期，游荡参与行为调度，鬼域袭击经过统一裁决。 */
public final class GhostMistModule implements GhostModule {
    /**
     * 声明需要的自主行动类型。
     * 具体 Goal 的创建、资源竞争和停止清理由行动适配层负责。
     */
    @Override public GhostBehavior behavior() { return GhostBehavior.WANDER; }
    public static final ResourceLocation ID = id("ghost_mist");
    public static final double INITIAL_INTENSITY = 5;
    public static final ResourceLocation DOMAIN = id("ghost_mist/domain");
    public static final ResourceLocation ATTACK = id("ghost_mist/attack");

    /**
     * 构造或返回本定义使用的资源标识，保持注册与调用入口一致。
     */
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MODID, path); }

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

    /** 生命周期不依赖 MOVE 控制，也不依赖被死机过滤的规则 Tick。 */

    @Override public List<GhostMedium> getMedia() {
        return List.of(new GhostMedium() {
            /**
             * 获取当前定义的唯一标识。
             * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
             */
            @Override public ResourceLocation getId() { return id("ghost_mist/domain_medium"); }

            /**
             * 校验本媒介要求的环境、事件或输入条件。
             * 返回 false 会阻止本次能力执行，主动使用也不能绕过该检查。
             */
            @Override public boolean allows(GhostAbilityContext c) {
                var world = c.getModuleContext().world();
                if (!world.present()) return false;
                if (c.abilityId().equals(DOMAIN)) return world.canMaintainDomain();
                return c.event() instanceof GhostDomainExposureEvent e && world.isOrigin(e.source())
                        && world.containsInDomain(e.domainId(), e.target());
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
            @Override public ResourceLocation getId() { return id("ghost_mist/exposure_rule"); }

            /**
             * 判断当前语义事件是否属于本规则处理的范围。
             * 这里只筛选事件种类或语义，不执行能力和世界操作。
             */
            @Override public boolean supports(GhostEvent e) { return e instanceof GhostDomainExposureEvent; }

            /**
             * 检查当前事实和模块上下文是否满足规则触发条件。
             * 返回 false 时跳过本规则，不提交能力或灵异影响。
             */
            @Override public boolean canTrigger(GhostRuleContext c, GhostEvent e) {
                return c.getModuleContext().world().present();
            }

            /**
             * 处理已满足条件的语义事件，发起对应能力或提交灵异影响。
             * 需要裁决的效果仅在获准后执行。
             */
            @Override public void onTrigger(GhostRuleContext c, GhostEvent event) {
                var e = (GhostDomainExposureEvent)event;
                c.submitAbilityInfluence(ATTACK, e, e.target().getUUID(),
                        GhostInfluenceType.ATTACK, e.domainStrength() / 5.0D);
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
            @Override public ResourceLocation getId() { return DOMAIN; }

            /**
             * 检查本次能力调用的事件、参数或环境是否可用于执行。
             * 模块可用性与媒介由统一执行器共同校验。
             */
            @Override public boolean canExecute(GhostAbilityContext c) { return c.getModuleContext().world().canMaintainDomain(); }

            /**
             * 执行当前能力的效果。
             * 调用方应使用统一执行器，以确保模块状态和媒介已经检查。
             */
            @Override public void execute(GhostAbilityContext c) {
                c.getModuleContext().world().maintainDomain();
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
            @Override public boolean canExecute(GhostAbilityContext c) { return c.event() instanceof GhostDomainExposureEvent; }

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
                var world = c.getModuleContext().world();
                var e = (GhostDomainExposureEvent)c.event();
                boolean killed = world.attack(e.target(), ModDamageTypes.GHOST_MIST, e.domainStrength() / 5.0D);
                if (killed) world.growDomain();
                return killed;
            }
        });
    }

    /**
     * 基础强度。
     */

    @Override
    public double minimumStrengthRatio() {
        return 0.50D;
    }

    /**
     * 鬼域。
     */
    @Override
    public Optional<PossessedGhostDomainData> initialDomainData() {
        return Optional.of(
                PossessedGhostDomainData.create(
                        getInitialData().intensity(),
                        80.0D
                )
        );
    }


}
