package com.qidate.qisplan2.ghost.module.implementation.candle;

import com.qidate.qisplan2.ghost.module.GhostMedium;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleCombatState;
import com.qidate.qisplan2.ghost.module.GhostRule;
import com.qidate.qisplan2.ghost.module.GhostRuleContext;
import com.qidate.qisplan2.ghost.module.ability.GhostAbility;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.environment.ExposedMedium;
import com.qidate.qisplan2.ghost.module.environment.GhostEnvironmentalEffect;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.IgnitionChangedEvent;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * 鬼烛的完整定义：点燃规则、屏蔽/吸引能力、暴露媒介。
 * 模块不读取物品、玩家或实体。相同状态与环境能力必定得到相同的执行结果。
 */
public abstract class GhostCandleModule implements GhostModule {
    private final ResourceLocation id;

    /**
     * 用给定路径建立鬼烛模块的注册标识。
     * 定义对象只保存不可变身份；点燃状态、燃料和消耗余数保存在模块实例 customData。
     *
     * @param path qisplan2 命名空间内的模块路径
     */
    protected GhostCandleModule(String path) { id = ResourceLocation.fromNamespaceAndPath("qisplan2", path); }

    /**
     * 获取当前定义的唯一标识。
     * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
     */
    @Override public ResourceLocation getId() { return id; }

    /**
     * 提供新建模块实例的初始强度和复苏值。
     * 仅用于初始化；已存在实例以保存的模块数据为准。
     */
    @Override public GhostModuleInitialData getInitialData() { return new GhostModuleInitialData(5, 0); }

    /**
     * 说明该模块是否接受点燃状态输入。
     * 点燃状态保存在模块数据中，交互方式由输入适配器决定。
     */
    @Override public boolean supportsIgnition() { return true; }

    /**
     * 生成鬼烛环境能力的标识，规则与能力使用同一标识关联调用。
     */
    private ResourceLocation effectId() { return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "/effect"); }

    /**
     * 读取点燃状态并结合模块可用性判断是否有效，避免死机或未点燃时显示生效。
     */
    public static boolean isLit(GhostModuleData.Entry entry) {
        return GhostModuleCombatState.available(entry) && entry.customData().getBoolean("lit");
    }

    /**
     * 把被屏蔽的声音强度换算为每 tick 的额外燃烧消耗。
     * 每 10 点强度额外消耗 1 点耐久每秒；负数或非有限值不产生额外消耗。
     */
    public static double soundBurnPerTick(double intensity) {
        return Double.isFinite(intensity) ? Math.max(0, intensity) / 200.0D : 0;
    }

    /**
     * 声明能力执行前必须满足的媒介。
     * 主动调用同样需要通过媒介；规则是否触发由规则层单独判断。
     */
    @Override public List<GhostMedium> getMedia() { return List.of(ExposedMedium.INSTANCE); }

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
            public ResourceLocation getId() { return effectId(); }

            /**
             * 判断当前语义事件是否属于本规则处理的范围。
             * 这里只筛选事件种类或语义，不执行能力和世界操作。
             */
            public boolean supports(GhostEvent event) {
                return event instanceof IgnitionChangedEvent || event instanceof ModuleTickEvent;
            }

            /**
             * 读取模块 customData 中的 lit 状态。
             * 已点燃才触发环境效果能力；暴露条件由独立媒介检查。
             */
            public boolean canTrigger(GhostRuleContext context, GhostEvent event) {
                return context.getModuleContext().getCustomData().getBoolean("lit");
            }

            /**
             * 处理已满足条件的语义事件，发起对应能力或提交灵异影响。
             * 需要裁决的效果仅在获准后执行。
             */
            public void onTrigger(GhostRuleContext context, GhostEvent event) {
                context.tryExecuteAbility(effectId(), event);
            }
        });
    }

    /**
     * 返回本厉鬼自身的能力定义。
     * 世界效果由这些能力执行，输入桥和行为适配器只负责发起调用。
     */
    @Override public List<GhostAbility> getIntrinsicAbilities() {
        return List.of(new GhostAbility() {
            /** 技能图标由所属模块直接声明。 */
            @Override public ResourceLocation iconTexture() { return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/item/" + id.getPath() + ".png"); }

            /**
             * 获取当前定义的唯一标识。
             * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
             */
            public ResourceLocation getId() { return effectId(); }

            /**
             * 本能力没有额外的参数限制。
             * 点燃条件由规则负责，暴露条件由媒介负责，执行器统一检查模块状态。
             */
            public boolean canExecute(GhostAbilityContext context) { return true; }

            /**
             * 发布白烛屏蔽或红烛吸引效果。
             * 仅在模块 tick 中结算燃烧；白烛另按被屏蔽声音的强度增加消耗。
             */
            public void execute(GhostAbilityContext context) {
                var environment = context.getModuleContext().environment();
                environment.emit(environmentalEffect());
                if (context.event() instanceof ModuleTickEvent) {
                    double burn = environment.burnPressure();
                    if (environmentalEffect() == GhostEnvironmentalEffect.CONCEALMENT)
                        burn += soundBurnPerTick(environment.blockedSoundIntensity());
                    environment.consume(burn);
                }
            }
        });
    }
}
