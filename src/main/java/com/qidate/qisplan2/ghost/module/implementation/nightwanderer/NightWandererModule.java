package com.qidate.qisplan2.ghost.module.implementation.nightwanderer;

import com.qidate.qisplan2.ghost.module.ability.GhostAbility;
import com.qidate.qisplan2.ghost.module.environment.GhostBehavior;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.ghost.bearing.SpiritualPart;
import com.qidate.qisplan2.ghost.corrosion.GhostCorrosion;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.*;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import static com.qidate.qisplan2.QisPlan2.MODID;

/**
 * 完整夜游鬼：夜间行为负责寻找和靠近目标，附近实体事实触发袭击规则。
 * 媒介限制夜间、距离、冷却与有效目标；袭击必须通过灵异裁决。
 * 击杀数和各项计时由 NightWandererState 写入模块 customData。
 */
public final class NightWandererModule implements GhostModule {

    /**
     * 声明需要的自主行动类型。
     * 具体 Goal 的创建、资源竞争和停止清理由行动适配层负责。
     */
    @Override public GhostBehavior behavior() { return GhostBehavior.NIGHT_HUNT; }
    public static final ResourceLocation ID = id("night_wanderer");
    public static final double INITIAL_INTENSITY = 5.0D;
    public static final ResourceLocation ATTACK = id("night_wanderer/attack");

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

    /**
     * 返回灵异防御值，供伤害结算使用。
     */
    @Override public double getSupernaturalDefense() { return 6; }

    /** 实体行为直接调用活动能力，不以请求伪装语义事件。 */

    @Override public List<GhostMedium> getMedia() {
        return List.of(new GhostMedium() {
            /**
             * 获取当前定义的唯一标识。
             * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
             */
            @Override public ResourceLocation getId() { return id("night_wanderer/medium"); }

            /**
             * 校验本媒介要求的环境、事件或输入条件。
             * 返回 false 会阻止本次能力执行，主动使用也不能绕过该检查。
             */
            @Override public boolean allows(GhostAbilityContext c) {
                var world = c.getModuleContext().world();
                if (!world.present()) return false;
                if (world.isDay()) return false;
                var target = attackTarget(c);
                return world.validTarget(target) && world.position().distanceToSqr(target.position()) <= 9
                        && c.getModuleContext().getCustomData().getInt("attackCooldown") == 0;
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
            @Override public ResourceLocation getId() { return id("night_wanderer/rules"); }

            /**
             * 判断当前语义事件是否属于本规则处理的范围。
             * 这里只筛选事件种类或语义，不执行能力和世界操作。
             */
            @Override public boolean supports(GhostEvent e) {
                return e instanceof EntityNearbyEvent || e instanceof SleepTimeSkippedEvent || e instanceof ModuleTickEvent;
            }

            /**
             * 检查当前事实和模块上下文是否满足规则触发条件。
             * 返回 false 时跳过本规则，不提交能力或灵异影响。
             */
            @Override public boolean canTrigger(GhostRuleContext c, GhostEvent e) {
                return c.getModuleContext().world().present();
            }

            /**
             * 按事实分别推进袭击冷却、提交附近目标袭击或结算睡眠跳时成长。
             * 冷却由模块 tick 推进，因此不依赖实体 Goal 是否正在执行。
             */
            @Override public void onTrigger(GhostRuleContext c, GhostEvent e) {
                if (e instanceof ModuleTickEvent) {
                    // 冷却是模块状态，不能只依靠实体 Goal 推进；主动调用同样使用它。
                    var data = c.getModuleContext().getCustomData();
                    int cooldown = data.getInt("attackCooldown");
                    if (cooldown > 0) {
                        data.putInt("attackCooldown", cooldown - 1);
                        c.getModuleContext().setCustomData(data);
                    }
                } else if (e instanceof EntityNearbyEvent nearby) {
                    c.submitAbilityInfluence(ATTACK, e, nearby.target().getUUID(),
                            GhostInfluenceType.ATTACK, c.getModuleContext().intensity());
                } else {
                    if (e instanceof SleepTimeSkippedEvent sleep && sleep.skippedTicks() >= 600)
                        addKills(c.getModuleContext(), (int)Math.min(Integer.MAX_VALUE, sleep.skippedTicks() / 600));
                }
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
            @Override public ResourceLocation iconTexture() { return ResourceLocation.parse("minecraft:textures/item/iron_sword.png"); }

            /**
             * 获取当前定义的唯一标识。
             * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
             */
            @Override public ResourceLocation getId() { return ATTACK; }
            /** 主动使用复苏代价由模块声明；规则与行为调用不重复收取。 */
            @Override public GhostAbilityStimulation getStimulation(GhostAbilityContext c) {
                if (c.source() != GhostAbilityContext.Source.POSSESSION) return GhostAbilityStimulation.none();
                var data = c.getModuleContext().getCustomData();
                long last = data.getLong("lastDirectUse");
                long now = c.getModuleContext().world().gameTime();
                double gain = data.contains("lastDirectUse") && now >= last && now - last < 200 ? 30 : 10;
                return new GhostAbilityStimulation(gain, GhostAbilityStimulation.Timing.ON_ACCEPTED);
            }


            /**
             * 检查本次能力调用的事件、参数或环境是否可用于执行。
             * 模块可用性与媒介由统一执行器共同校验。
             */
            @Override public boolean canExecute(GhostAbilityContext c) { return attackTarget(c) != null; }

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
                var context = c.getModuleContext();
                var target = attackTarget(c);
                if (c.source() == GhostAbilityContext.Source.POSSESSION) {
                    var state = context.getCustomData();
                    state.putLong("lastDirectUse", context.world().gameTime());
                    context.setCustomData(state);
                }
                context.world().stopAndSwing();
                boolean killed = context.world().attack(target, ModDamageTypes.GHOST_NIGHT_WANDERER, context.effectiveIntensity());
                if (killed) addKills(context, 1);
                var data = context.getCustomData();
                data.putInt("attackCooldown", Math.max(1, 200 - Math.max(0, data.getInt("killCount")) / 5));
                context.setCustomData(data);
                return killed;
            }
        });
    }

    /**
     * 返回本厉鬼对各身体部位和全局的侵蚀配置。
     */
    private static final GhostCorrosion CORROSION =
            GhostCorrosion.builder()
                    .add(SpiritualPart.GLOBAL, 20)
                    .add(SpiritualPart.HAND, 20)
                    .add(SpiritualPart.EYE, 20)
                    .add(SpiritualPart.FOOT, 10)
                    .build();

    @Override
    public GhostCorrosion corrosion() {
        return CORROSION;
    }


    /**
     * 声明主动能力的影响类型，供调用入口建立灵异裁决。
     */
    @Override public GhostInfluenceType activeInfluence() { return GhostInfluenceType.ATTACK; }


    /** 成长只修改本模块数据，换载体不会丢失击杀数或复制强度。 */
    private static void addKills(GhostModuleContext context, int amount) {
        var data = context.getCustomData();
        int old = Math.max(0, data.getInt("killCount"));
        int next = (int)Math.min(Integer.MAX_VALUE, (long)old + Math.max(0, amount));
        data.putInt("killCount", next);
        context.setCustomData(data);
        context.setIntensity(context.intensity() + (next - old) * 0.1D);
    }

    /** 规则使用附近事实，主动输入使用目标参数，两条入口执行同一个袭击能力。 */
    private static LivingEntity attackTarget(GhostAbilityContext context) {
        if (context.parameters() instanceof GhostTargetParameters input) return input.target();
        if (context.event() == null) return context.getModuleContext().world().aimedTarget(3);
        return context.event() instanceof EntityNearbyEvent event
                && context.getModuleContext().world().isOrigin(event.observer()) ? event.target() : null;
    }
}
