package com.qidate.qisplan2.ghost.module.implementation;

import net.minecraft.world.entity.LivingEntity;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.ghost.module.GhostMedium;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostRule;
import com.qidate.qisplan2.ghost.module.GhostRuleContext;
import com.qidate.qisplan2.ghost.module.ability.GhostAbility;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.environment.BlockSpreadSpec;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.TouchEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceCollector;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * 完整鬼草丛模块。
 * 规则响应真实接触，并按目标限制五秒一次；媒介要求有效接触目标。
 * 能力经统一裁决后袭击，实际击杀成功才调用通用三维蔓延。
 * 本定义不取得宿主对象，关系数据与世界修改分别通过接口完成。
 */
public final class GhostGrassModule implements GhostModule {
    public static final ResourceLocation ID = id("ghost_grass");
    public static final ResourceLocation ATTACK = id("ghost_grass/contact_attack");
    public static final int ATTACK_COOLDOWN = 5 * 20;
    private static final String LAST_ATTACK = "lastAttackTick";

    /** 六邻域链式扩散，随机新增二至三格；只填空气，不需要下方支撑。 */
    public static final BlockSpreadSpec SPREAD = new BlockSpreadSpec(
            ID, 2, 3, BlockSpreadSpec.Shape.VOLUME_3D, BlockSpreadSpec.Pattern.CHAIN,
            BlockSpreadSpec.Replacement.AIR_ONLY, false, 2, 4096);

    /** 生成当前模块及其规则、媒介和能力使用的资源标识。 */
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("qisplan2", path);
    }

    /** 返回鬼草丛模块的注册身份。 */
    @Override public ResourceLocation getId() { return ID; }

    /** 保留旧草丛的初始强度；运行时袭击使用实例数据，不再次写死伤害强度。 */
    @Override
    public GhostModuleInitialData getInitialData() {
        return new GhostModuleInitialData(0.7D, 0);
    }

    /** 声明接触媒介；五秒冷却属于自动规则，不阻止已满足媒介的主动能力调用。 */
    @Override
    public List<GhostMedium> getMedia() {
        return List.of(new GhostMedium() {
            /** 返回接触媒介标识。 */
            public ResourceLocation getId() { return id("ghost_grass/contact"); }

            /** 瞬时或持续接触均可作为媒介，目标必须位于服务端且仍然存活。 */
            public boolean allows(GhostAbilityContext context) {
                return contactTarget(context) != null;
            }
        });
    }

    /** 返回触碰袭击规则；持续接触也会检查冷却，使受保护目标在五秒后重新接受裁决。 */
    @Override
    public List<GhostRule> getRules() {
        return List.of(new GhostRule() {
            /** 返回触碰规则标识。 */
            public ResourceLocation getId() { return id("ghost_grass/touch_rule"); }

            /** 复用不可触之鬼与鬼地毯使用的接触事实，不新增能力请求型事件。 */
            public boolean supports(GhostEvent event) { return event instanceof TouchEvent; }

            /** 检查服务端目标资格；死机、沉睡及模块移除由规则运行时先行拦截。 */
            public boolean canTrigger(GhostRuleContext context, GhostEvent event) {
                return event instanceof TouchEvent touch
                        && touch.target().isAlive() && !touch.target().level().isClientSide();
            }

            /**
             * 为本次袭击占用冷却并保存目标关系状态，然后收集保护诅咒并提交裁决。
             * 多块草丛同 tick 接触同一目标时共享静态模块的关系状态，不会重复发起袭击。
             * 裁决失败仍消耗本次规则冷却，避免每 tick 重复尝试。
             */
            public void onTrigger(GhostRuleContext context, GhostEvent event) {
                var touch = (TouchEvent) event;
                var module = context.getModuleContext();
                var state = module.targetState().read(touch.target());
                if (!claimAttack(state, touch.target().level().getGameTime())) return;
                module.targetState().write(touch.target(), state);
                GhostInfluenceCollector.collectCurses(context, touch.target().getUUID(), touch.target().getServer());
                context.submitAbilityInfluence(ATTACK, touch, touch.target().getUUID(),
                        GhostInfluenceType.ATTACK, module.intensity());
            }
        });
    }

    /**
     * 检查并占用目标的自动袭击冷却。
     * 首次接触立即获准；同 tick 重复接触或不足一百 tick 时拒绝。
     * 世界时间倒退时允许重新建立基线，避免旧时间记录永久阻止袭击。
     * 本方法只处理关系数据，不执行世界操作或判断载体类型。
     */
    public static boolean claimAttack(CompoundTag state, long now) {
        if (state.contains(LAST_ATTACK)) {
            long last = state.getLong(LAST_ATTACK);
            if (now >= last && now - last < ATTACK_COOLDOWN) return false;
        }
        state.putLong(LAST_ATTACK, now);
        return true;
    }

    /** 返回袭击能力；效果入口仅使用通用世界接口，不访问方块、实体或玩家宿主实现。 */
    @Override
    public List<GhostAbility> getIntrinsicAbilities() {
        return List.of(new GhostAbility() {
            /** 技能图标由所属模块直接声明。 */
            @Override public ResourceLocation iconTexture() { return ResourceLocation.parse("qisplan2:textures/block/ghost_grass.png"); }

            /** 返回草丛袭击能力标识。 */
            public ResourceLocation getId() { return ATTACK; }

            /** 只有接触事实才能为能力提供目标。 */
            public boolean canExecute(GhostAbilityContext context) {
                return contactTarget(context) != null;
            }

            /** 兼容无需取得效果结果的调用方。 */
            public void execute(GhostAbilityContext context) { executeWithResult(context); }

            /**
             * 尝试统一灵异击杀，返回实际结果。
             * 只有击杀成功才扩展附近已有草丛群落；袭击被保护挡住或失败时不蔓延。
             */
            public boolean executeWithResult(GhostAbilityContext context) {
                var target = contactTarget(context);
                var module = context.getModuleContext();
                var world = module.world();
                boolean killed = world.attack(target, ModDamageTypes.GHOST_GRASS, module.intensity());
                if (killed) world.spreadBlocks(target, SPREAD);
                return killed;
            }
        });
    }    /** 接触是媒介，不要求先触发自动规则；主动释放仍必须正在接触目标。 */
    private static LivingEntity contactTarget(GhostAbilityContext context) {
        if (context.event() instanceof TouchEvent touch)
            return touch.target().isAlive() && !touch.target().level().isClientSide() ? touch.target() : null;
        var world = context.getModuleContext().world();
        var target = world.aimedTarget(3);
        return target != null && target.isAlive() && world.touching(target) ? target : null;
    }

}
