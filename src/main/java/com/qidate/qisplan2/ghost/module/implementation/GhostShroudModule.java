package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.ghost.module.GhostMedium;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostRule;
import com.qidate.qisplan2.ghost.module.GhostRuleContext;
import com.qidate.qisplan2.ghost.module.ability.GhostAbility;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import com.qidate.qisplan2.ghost.module.event.SupernaturalAttackEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * 完整鬼寿衣：覆盖媒介成立时，以永久最大生命损失抵挡来袭，并持续侵蚀当前生命。
 * 模块只处理语义事实、强度与计时，覆盖关系和生命操作均由接口解释。
 */
public final class GhostShroudModule implements GhostModule {
    public static final ResourceLocation ID = id("ghost_shroud");
    public static final ResourceLocation PROTECT = id("ghost_shroud/protect");
    public static final ResourceLocation DRAIN = id("ghost_shroud/drain");
    // 沿用旧属性标识，让存档中已经支付的永久代价继续累计。
    private static final ResourceLocation LOSS = id("ghost_shroud_max_health_loss");
    private static final int DRAIN_INTERVAL = 500;

    /** 创建模块内部标识。 */
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("qisplan2", path);
    }

    /** 返回唯一模块身份。 */
    @Override public ResourceLocation getId() { return ID; }

    /** 初始强度用于模块系统；生命代价按来袭强度计算，保留原有抵挡公式。 */
    @Override public GhostModuleInitialData getInitialData() { return new GhostModuleInitialData(1, 0); }

    /** 使用既有物品纹理作为模块图标。 */
    @Override public ResourceLocation iconTexture() { return id("textures/item/ghost_shroud.png"); }

    /** 至少消耗一点最大生命；非有限强度不能形成合法代价。 */
    public static double protectionCost(double intensity) {
        return Double.isFinite(intensity) && intensity >= 0 ? Math.max(1, intensity) : Double.POSITIVE_INFINITY;
    }

    /** 保护与持续侵蚀都要求覆盖媒介；保护还必须对应当前受袭目标。 */
    @Override public List<GhostMedium> getMedia() {
        return List.of(new GhostMedium() {
            /** 返回覆盖媒介标识。 */
            public ResourceLocation getId() { return id("ghost_shroud/surface"); }

            /** 不询问覆盖如何实现，只验证环境提供的关系事实。 */
            public boolean allows(GhostAbilityContext context) {
                var environment = context.getModuleContext().environment();
                return environment.surfaceCovered()
                        && (!(context.event() instanceof SupernaturalAttackEvent attack)
                        || environment.coversSurface(attack.target()));
            }
        });
    }

    /** 两条规则分别处理来袭事实与覆盖期间的持续侵蚀。 */
    @Override public List<GhostRule> getRules() {
        return List.of(new GhostRule() {
            /** 返回来袭规则标识。 */
            public ResourceLocation getId() { return id("ghost_shroud/attacked"); }

            /** 只响应灵异袭击，普通伤害不触发抵挡。 */
            public boolean supports(GhostEvent event) { return event instanceof SupernaturalAttackEvent; }

            /** 已被抵挡的袭击不再提交保护。 */
            public boolean canTrigger(GhostRuleContext context, GhostEvent event) {
                return event instanceof SupernaturalAttackEvent attack && !attack.isBlocked();
            }

            /** 提交保护能力，最终是否阻断袭击取决于实际代价支付结果。 */
            public void onTrigger(GhostRuleContext context, GhostEvent event) {
                var attack = (SupernaturalAttackEvent) event;
                context.submitAbilityInfluence(PROTECT, event, attack.target(),
                        GhostInfluenceType.PROTECTION, context.getModuleContext().intensity());
            }
        }, new GhostRule() {
            /** 返回持续侵蚀规则标识。 */
            public ResourceLocation getId() { return id("ghost_shroud/erosion"); }

            /** 只接收统一模块生命周期事件。 */
            public boolean supports(GhostEvent event) { return event instanceof ModuleTickEvent; }

            /** 未覆盖时暂停计时；死机和沉睡由规则运行时统一阻止。 */
            public boolean canTrigger(GhostRuleContext context, GhostEvent event) {
                return context.getModuleContext().environment().surfaceCovered();
            }

            /** 计时存入模块独有数据，迁移或保存时随模块一起保留。 */
            public void onTrigger(GhostRuleContext context, GhostEvent event) {
                var module = context.getModuleContext();
                var data = module.getCustomData();
                int ticks = Math.max(0, Math.min(DRAIN_INTERVAL - 1, data.getInt("erosionTicks"))) + 1;
                data.putInt("erosionTicks", ticks >= DRAIN_INTERVAL ? 0 : ticks);
                module.setCustomData(data);
                if (ticks >= DRAIN_INTERVAL) {
                    var effect = context.prepareAbility(DRAIN, event);
                    if (effect != null) effect.run();
                }
            }
        });
    }

    /** 能力只通过世界端口支付生命代价，不接触具体宿主实现。 */
    @Override public List<GhostAbility> getIntrinsicAbilities() {
        return List.of(new GhostAbility() {
            /** 返回保护能力标识。 */
            public ResourceLocation getId() { return PROTECT; }

            /** 多个保护依次执行时，后续保护必须再次检查袭击结果。 */
            public boolean canExecute(GhostAbilityContext context) {
                return context.event() instanceof SupernaturalAttackEvent attack && !attack.isBlocked();
            }

            /** 委托返回真实效果结果的执行入口。 */
            public void execute(GhostAbilityContext context) { executeWithResult(context); }

            /** 只有代价支付成功才抵挡；生命不足时不扣除、不阻断袭击。 */
            public boolean executeWithResult(GhostAbilityContext context) {
                if (!canExecute(context)) return false;
                var attack = (SupernaturalAttackEvent) context.event();
                if (!context.getModuleContext().world().consumeMaxHealth(
                        LOSS, protectionCost(attack.intensity()), 1)) return false;
                attack.block();
                return true;
            }
        }, new GhostAbility() {
            /** 返回持续侵蚀能力标识。 */
            public ResourceLocation getId() { return DRAIN; }

            /** 持续侵蚀由生命周期事实驱动。 */
            public boolean canExecute(GhostAbilityContext context) { return context.event() instanceof ModuleTickEvent; }

            /** 委托真实生命操作。 */
            public void execute(GhostAbilityContext context) { executeWithResult(context); }

            /** 每次侵蚀扣一点当前生命，与永久抵挡代价分开结算。 */
            public boolean executeWithResult(GhostAbilityContext context) {
                return context.getModuleContext().world().drainHealth(1);
            }
        });
    }
}
