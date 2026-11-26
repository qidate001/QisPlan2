package com.qidate.qisplan2.ghost.module.implementation.candle;

import java.util.Set;
import com.qidate.qisplan2.ghost.module.GhostModuleContext;
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
        return (GhostModuleCombatState.available(entry) || entry.customData().getBoolean("manualBurn")) && entry.customData().getBoolean("lit");
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
            public Set<Class<? extends GhostEvent>> eventTypes() { return Set.of(IgnitionChangedEvent.class,ModuleTickEvent.class); }


            /**
             * 读取模块 customData 中的 lit 状态。
             * 已点燃才触发环境效果能力；暴露条件由独立媒介检查。
             */
            public boolean canTrigger(GhostRuleContext context, GhostEvent event) {
                return context.getModuleContext().getCustomData().getBoolean("lit")
                        && !context.getModuleContext().getCustomData().getBoolean("manualBurn");
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
        return List.of(burningSwitch(true), burningSwitch(false), new GhostAbility() {
            /** 技能图标由所属模块直接声明。 */
            @Override public ResourceLocation iconTexture() { return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/item/" + id.getPath() + ".png"); }

            /**
             * 获取当前定义的唯一标识。
             * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
             */
            public ResourceLocation getId() { return effectId(); }

            /** 吸引或屏蔽只由燃烧过程维持，不能作为独立技能。 */
            @Override public boolean isPublicSkill() { return false; }

            /**
             * 本能力没有额外的参数限制。
             * 点燃条件由规则负责，暴露条件由媒介负责，执行器统一检查模块状态。
             */
            public boolean canExecute(GhostAbilityContext context) { return context.getModuleContext().getCustomData().getBoolean("lit"); }

            /**
             * 发布白烛屏蔽或红烛吸引效果。
             * 仅在模块 tick 中结算燃烧；白烛另按被屏蔽声音的强度增加消耗。
             */
            public void execute(GhostAbilityContext context) {
                var environment = context.getModuleContext().environment();
                environment.setBurning(true);
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

    /** 已主动点燃的持续能力独立于自动规则，但仍每 tick 通过暴露媒介。 */
    @Override public boolean ticksAbilities() { return true; }

    /** 维护已启动能力，由注册时声明的 Tick 订阅调度。 */
    public void tickAbilities(GhostModuleContext context) {
        var data = context.getCustomData();
        if (data.getBoolean("lit") && data.getBoolean("manualBurn"))
            context.continueAbility(effectId(), new ModuleTickEvent());
    }

    /** 开始与结束燃烧是公开技能；停止不要求暴露，避免无法熄灭。 */
    private GhostAbility burningSwitch(boolean start) {
        return new GhostAbility() {
            /** 返回能力或模块的稳定标识。 */
            public ResourceLocation getId() { return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + (start ? "/ignite" : "/extinguish")); }
            /** 声明技能使用的图标纹理。 */
            public ResourceLocation iconTexture() { return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/item/" + id.getPath() + ".png"); }
            /** 开始燃烧要求暴露，结束燃烧允许随时执行。 */
            public boolean inheritsModuleMedia() { return start; }
            /** 检查本次执行所需的环境与语义条件。 */
            public boolean canExecute(GhostAbilityContext c) { return !start || c.getModuleContext().environment().exposed(); }
            /** 在通过统一执行器后更新本能力的状态与效果。 */
            public void execute(GhostAbilityContext c) {
                var module = c.getModuleContext();
                var data = module.getCustomData();
                data.putBoolean("lit", start);
                data.putBoolean("manualBurn", start);
                if (!start) data.remove("activeEffect");
                module.setCustomData(data);
                module.environment().setBurning(start);
                if (start) module.continueAbility(effectId(), new IgnitionChangedEvent(true));
            }
        };
    }
}
