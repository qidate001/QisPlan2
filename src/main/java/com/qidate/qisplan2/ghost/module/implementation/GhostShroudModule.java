package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.ghost.corrosion.CorrosionType;
import com.qidate.qisplan2.ghost.corrosion.GhostCorrosion;
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

    /** 新实例初始强度为三十；运行时抵挡上限读取实例当前强度。 */
    @Override public GhostModuleInitialData getInitialData() { return new GhostModuleInitialData(30, 0); }

    /** 驾驭后由通用侵蚀系统计入皮肤六十点，不额外继承默认全局侵蚀。 */
    @Override public GhostCorrosion corrosion() {
        return GhostCorrosion.builder().add(CorrosionType.SKIN, 60).build();
    }

    /** 使用既有物品纹理作为模块图标。 */
    @Override public ResourceLocation iconTexture() { return id("textures/item/ghost_shroud.png"); }

    /** 按实际抵挡量支付最大生命，最低一点；非有限强度不能形成合法代价。 */
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
                int ticks = Math.clamp(data.getInt("erosionTicks"), 0, DRAIN_INTERVAL - 1) + 1;
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

            /** 每次最多抵挡自身强度；代价支付失败时，不修改来袭剩余强度。 */
            public boolean executeWithResult(GhostAbilityContext context) {
                if (!canExecute(context)) return false;
                var attack = (SupernaturalAttackEvent) context.event();
                var module = context.getModuleContext();
                double capacity = module.intensity();
                if (!Double.isFinite(capacity) || capacity <= 0) return false;
                double absorbed = Math.min(attack.remainingIntensity(), capacity);
                if (!module.world().consumeMaxHealth(LOSS, protectionCost(absorbed), 1)) return false;
                // 只削弱已经支付代价的部分；未归零时，后续保护及原袭击继续处理剩余强度。
                attack.absorb(absorbed);
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
