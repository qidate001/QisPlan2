package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostRule;
import com.qidate.qisplan2.ghost.module.GhostRuleContext;
import com.qidate.qisplan2.ghost.module.ability.GhostAbility;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.DestructionAttemptEvent;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import java.util.List;

import com.qidate.qisplan2.ghost.possession.classification.GhostClassification;
import com.qidate.qisplan2.ghost.possession.classification.GhostTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import static com.qidate.qisplan2.QisPlan2.MODID;

/**
 * 完整的鬼石砖模块：方块、物品或实体即将被摧毁时反击破坏者。
 * 只有灵异反击实际成功，才取消当前这一次摧毁；裁决失败或攻击被抵挡不会保护宿主。
 */
public final class GhostStoneBricksModule implements GhostModule {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MODID, "ghost_stone_bricks");
    private static final ResourceLocation RULE_ID = ResourceLocation.fromNamespaceAndPath(MODID, "ghost_stone_bricks/destruction_rule");
    private static final ResourceLocation RETALIATION = ResourceLocation.fromNamespaceAndPath(MODID, "ghost_stone_bricks/retaliation");
    private static final List<GhostAbility> ABILITIES = List.of(new RetaliationAbility());

    /**
     * 获取当前定义的唯一标识。
     * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
     */
    @Override public ResourceLocation getId() { return ID; }

    /**
     * 提供新建模块实例的初始强度和复苏值。
     * 仅用于初始化；已存在实例以保存的模块数据为准。
     */
    @Override public GhostModuleInitialData getInitialData() { return new GhostModuleInitialData(5, 0); }

    /**
     * 返回本厉鬼自身的能力定义。
     * 世界效果由这些能力执行，输入桥和行为适配器只负责发起调用。
     */
    @Override public List<GhostAbility> getIntrinsicAbilities() { return ABILITIES; }

    /**
     * 声明本厉鬼响应的语义事件及对应规则。
     * 规则负责决定何时发起能力或提交影响，不绕过执行器直接完成袭击。
     */
    @Override public List<GhostRule> getRules() { return List.of(new DestructionRule()); }

    /**
     * 声明能够压制的目标标签；空集合表示没有额外的标签压制对象。
     */
    @Override public GhostClassification suppressionTargets() { return GhostClassification.of(GhostTag.ENTITY); }

    /**
     * 声明本厉鬼的灵异分类标签，供压制关系匹配使用。
     */
    @Override public GhostClassification classification() { return GhostClassification.of(GhostTag.ENTITY); }

    /**
     * 返回可分配的压制单位数量，供驾驭压制配置使用。
     */
    @Override public int suppressionUnits() { return 1; }

    /**
     * 返回模块展示图标的纹理位置。
     */
    @Override
    public ResourceLocation iconTexture() {
        return ResourceLocation.fromNamespaceAndPath(
                QisPlan2.MODID,
                "textures/block/ghost_stone_bricks.png"
        );
    }

    /**
     * 返回压制配置界面使用的图标纹理位置。
     */
    @Override
    public ResourceLocation suppressionIconTexture() {
        return ResourceLocation.fromNamespaceAndPath(
                QisPlan2.MODID,
                "textures/block/ghost_stone_bricks.png"
        );
    }

    /** 每种摧毁事实都明确自己的反击对象；环境伤害没有对象时不执行反击。 */
    private static LivingEntity target(GhostEvent event) {
        return event instanceof DestructionAttemptEvent destruction ? destruction.getAttacker() : null;
    }

    private static final class DestructionRule implements GhostRule {
        /**
         * 获取当前定义的唯一标识。
         * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
         */
        @Override public ResourceLocation getId() { return RULE_ID; }

        /**
         * 判断当前语义事件是否属于本规则处理的范围。
         * 这里只筛选事件种类或语义，不执行能力和世界操作。
         */
        @Override public boolean supports(GhostEvent event) {
            return event instanceof DestructionAttemptEvent;
        }

        /**
         * 检查当前事实和模块上下文是否满足规则触发条件。
         * 返回 false 时跳过本规则，不提交能力或灵异影响。
         */
        @Override public boolean canTrigger(GhostRuleContext context, GhostEvent event) {
            return supports(event) && !((DestructionAttemptEvent)event).isCanceled()
                    && target(event) != null && target(event).isAlive();
        }

        /**
         * 处理已满足条件的语义事件，发起对应能力或提交灵异影响。
         * 需要裁决的效果仅在获准后执行。
         */
        @Override public void onTrigger(GhostRuleContext context, GhostEvent event) {
            context.submitAbilityInfluence(RETALIATION, event, target(event).getUUID(),
                    GhostInfluenceType.ATTACK, context.getModuleContext().intensity());
        }
    }

    /** 三种宿主共用同一条“实际反击成功才撤销摧毁”的能力实现。 */
    private static final class RetaliationAbility implements GhostAbility {
            /** 技能图标由所属模块直接声明。 */
            @Override public ResourceLocation iconTexture() { return ResourceLocation.parse("qisplan2:textures/block/ghost_stone_bricks.png"); }

        /**
         * 获取当前定义的唯一标识。
         * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
         */
        @Override public ResourceLocation getId() { return RETALIATION; }

        /**
         * 检查本次能力调用的事件、参数或环境是否可用于执行。
         * 模块可用性与媒介由统一执行器共同校验。
         */
        @Override public boolean canExecute(GhostAbilityContext context) {
            if (context.event() == null) return context.getModuleContext().world().validTarget(context.getModuleContext().world().aimedTarget(8));
            return context.event() instanceof DestructionAttemptEvent
                    && !((DestructionAttemptEvent)context.event()).isCanceled()
                    && target(context.event()) != null && target(context.event()).isAlive();
        }

        /**
         * 执行当前能力的效果。
         * 调用方应使用统一执行器，以确保模块状态和媒介已经检查。
         */
        @Override public void execute(GhostAbilityContext context) { executeWithResult(context); }

        /**
         * 对摧毁责任者尝试灵异反击。
         * 只有反击实际成功才撤销本次摧毁；被保护挡住或执行失败不取消摧毁。
         */
        @Override public boolean executeWithResult(GhostAbilityContext context) {
            if (!canExecute(context)) return false;
            LivingEntity attacker = context.event() == null ? context.getModuleContext().world().aimedTarget(8) : target(context.event());
            boolean succeeded = context.getModuleContext().world().attack(attacker,
                    ModDamageTypes.GHOST_STONE_BRICKS, context.getModuleContext().intensity());
            if (succeeded && context.event() instanceof DestructionAttemptEvent destruction) destruction.cancel();
            return succeeded;
        }
    }
}
