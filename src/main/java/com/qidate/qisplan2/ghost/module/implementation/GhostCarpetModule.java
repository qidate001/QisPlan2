package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.*;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceCollector;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * 完整鬼地毯：累计接触达到一轮时提交灵异袭击，获准后执行袭击，击杀成功才蔓延。
 * 离开不清空进度；只有真实接触采样增加计时。模块不读取或判断自己的宿主类型。
 * 计时阈值属于规则，接触事实属于媒介，世界效果统一由环境出口实现。
 */
public final class GhostCarpetModule implements GhostModule {
    public static final ResourceLocation ID = id("ghost_carpet");
    public static final ResourceLocation ATTACK = id("ghost_carpet/contact_attack");
    public static final int CONTACT_TICKS = 15 * 20;
    private static final String PROGRESS = "contactTicks";

    /** 在模组命名空间内创建规则和能力标识。 */
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("qisplan2", path); }

    /** 返回完整鬼地毯的模块身份。 */
    @Override public ResourceLocation getId() { return ID; }

    /** 沿用旧鬼地毯的初始袭击强度 2，实例后续以自己的强度数据为准。 */
    @Override public GhostModuleInitialData getInitialData() { return new GhostModuleInitialData(2, 0); }

    /** 声明接触媒介；累计十五秒属于规则，主动调用仍需有真实接触目标。 */
    @Override public List<GhostMedium> getMedia() {
        return List.of(new GhostMedium() {
            /** 返回接触媒介标识。 */
            public ResourceLocation getId() { return id("ghost_carpet/contact"); }
            /** 只接受服务端仍存活目标的接触事实，不用宿主类别决定能力资格。 */
            public boolean allows(GhostAbilityContext c) {
                return c.event() instanceof TouchEvent touch && touch.target().isAlive()
                        && !touch.target().level().isClientSide();
            }
        });
    }

    /** 返回累计接触规则；瞬时点击不计入持续时间。 */
    @Override public List<GhostRule> getRules() {
        return List.of(new GhostRule() {
            /** 返回累计接触规则标识。 */
            public ResourceLocation getId() { return id("ghost_carpet/contact_duration"); }
            /** 复用接触语义，只处理携带真实持续时长的采样。 */
            public boolean supports(GhostEvent event) {
                return event instanceof TouchEvent touch && touch.elapsedTicks() > 0;
            }
            /** 只在目标仍存活的服务端推进；死机过滤由规则运行时处理。 */
            public boolean canTrigger(GhostRuleContext c, GhostEvent event) {
                return event instanceof TouchEvent touch && touch.target().isAlive()
                        && !touch.target().level().isClientSide();
            }
            /** 每次先保存新进度，再提交裁决；袭击被挡住也已消耗本轮接触时间。 */
            public void onTrigger(GhostRuleContext c, GhostEvent event) {
                var touch = (TouchEvent) event;
                var context = c.getModuleContext();
                var state = context.targetState().read(touch.target());
                int interval = context.world().contactIntervalTicks(touch.target(), CONTACT_TICKS);
                int attempts = accumulate(state, touch.elapsedTicks(), interval);
                context.targetState().write(touch.target(), state);
                if (attempts > 0)
                    GhostInfluenceCollector.collectCurses(c, touch.target().getUUID(), touch.target().getServer());
                for (int i = 0; i < attempts; i++)
                    c.submitAbilityInfluence(ATTACK, touch, touch.target().getUUID(),
                            GhostInfluenceType.ATTACK, context.intensity());
            }
        });
    }

    /**
     * 累计本次真实接触时间，保留不足一轮的余数并返回完整轮数。
     * 离开时不调用此方法，因此不会清零或把离线时间算作接触。
     */
    public static int accumulate(CompoundTag state, int elapsedTicks) {
        return accumulate(state, elapsedTicks, CONTACT_TICKS);
    }

    /** 按环境提供的有效间隔累计；配置改变时保留已有时间，并据新阈值结算完整轮数。 */
    public static int accumulate(CompoundTag state, int elapsedTicks, int interval) {
        if (interval <= 0) throw new IllegalArgumentException("Non-positive contact interval");
        if (elapsedTicks < 0) throw new IllegalArgumentException("Negative contact duration");
        long total = Math.max(0, state.getInt(PROGRESS)) + (long) elapsedTicks;
        state.putInt(PROGRESS, (int)(total % interval));
        return (int)(total / interval);
    }

    /** 能力只执行获准效果，不自行累计接触时间或重新触发规则。 */
    @Override public List<GhostAbility> getIntrinsicAbilities() {
        return List.of(new GhostAbility() {
            /** 返回灵异袭击能力标识。 */
            public ResourceLocation getId() { return ATTACK; }
            /** 确认本次调用具有接触目标。 */
            public boolean canExecute(GhostAbilityContext c) { return c.event() instanceof TouchEvent; }
            /** 兼容不需要取得袭击结果的调用入口。 */
            public void execute(GhostAbilityContext c) { executeWithResult(c); }
            /** 播放袭击表现并尝试击杀；只有实际击杀成功后才请求地毯蔓延。 */
            public boolean executeWithResult(GhostAbilityContext c) {
                var target = ((TouchEvent)c.event()).target();
                var world = c.getModuleContext().world();
                world.contactAttackFeedback(target);
                boolean killed = world.attack(target, ModDamageTypes.GHOST_CARPET, c.getModuleContext().intensity());
                if (killed) world.spreadCarpet(target);
                return killed;
            }
        });
    }
}
