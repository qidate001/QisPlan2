package com.qidate.qisplan2.ghost.module.implementation.divinationslip;


import com.qidate.qisplan2.ghost.module.ability.GhostInvocationParameters;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityStimulation;
import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.bearing.SpiritualPart;
import com.qidate.qisplan2.ghost.corrosion.GhostCorrosion;
import com.qidate.qisplan2.ghost.curse.Curse;
import com.qidate.qisplan2.ghost.curse.CurseManager;
import com.qidate.qisplan2.ghost.curse.type.ghostdivination.GhostDivinationCurse;
import com.qidate.qisplan2.ghost.curse.type.ghostdivination.GhostDivinationCurseSource;
import com.qidate.qisplan2.ghost.curse.type.ghostdivination.GhostDivinationCurseType;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostRule;
import com.qidate.qisplan2.ghost.module.GhostRuleContext;
import com.qidate.qisplan2.ghost.module.ability.GhostAbility;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityExecutor;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.DivinationSlipResultEvent;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.GhostUseEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceCollector;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import com.qidate.qisplan2.ghost.possession.classification.GhostClassification;
import com.qidate.qisplan2.ghost.possession.classification.GhostTag;
import com.qidate.qisplan2.network.divinationslip.GhostDivinationNetwork;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import static com.qidate.qisplan2.QisPlan2.MODID;

public final class GhostDivinationSlipModule implements GhostModule {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "ghost_divination_slip"
            );

    private static final ResourceLocation USE_RULE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "ghost_divination_slip/use_rule"
            );

    /** 每个签文结果就是一个独立能力，规则与主动调用共用标识。 */
    public static ResourceLocation resultAbility(int result) {
        return ResourceLocation.fromNamespaceAndPath(MODID, "ghost_divination_slip/" + switch (result) {
            case 0 -> "life"; case 1 -> "death"; case 2 -> "ghost";
            default -> throw new IllegalArgumentException("Unknown divination result");
        });
    }

    private static final int LIFE_RESULT = 0;
    private static final int DEATH_RESULT = 1;
    private static final int GHOST_RESULT = 2;

    private static final int LIFE_DURATION = 5 * 60 * 20;

    private static final List<GhostRule> RULES =
            List.of(new DivinationSlipUseRule());

    private static final List<GhostAbility> ABILITIES =
            List.of(new DivinationSlipAbility(LIFE_RESULT, "life"),
                    new DivinationSlipAbility(DEATH_RESULT, "death"), new DivinationSlipAbility(GHOST_RESULT, "ghost"));

    /**
     * 获取当前定义的唯一标识。
     * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
     */
    @Override
    public ResourceLocation getId() {
        return ID;
    }

    /**
     * 提供新建模块实例的初始强度和复苏值。
     * 仅用于初始化；已存在实例以保存的模块数据为准。
     */
    @Override
    public GhostModuleInitialData getInitialData() {
        return new GhostModuleInitialData(
                50.0D,
                0.0D
        );
    }

    /**
     * 声明本厉鬼响应的语义事件及对应规则。
     * 规则负责决定何时发起能力或提交影响，不绕过执行器直接完成袭击。
     */
    @Override
    public List<GhostRule> getRules() {
        return RULES;
    }

    /**
     * 返回本厉鬼自身的能力定义。
     * 世界效果由这些能力执行，输入桥和行为适配器只负责发起调用。
     */
    @Override
    public List<GhostAbility> getIntrinsicAbilities() {
        return ABILITIES;
    }

    private static final GhostCorrosion CORROSION =
            GhostCorrosion.builder()
                    .add(SpiritualPart.GLOBAL, 20)
                    .build();

    private static final double REVIVAL_GAIN = 20.0D;

    /**
     * 返回本厉鬼对各身体部位和全局的侵蚀配置。
     */
    @Override
    public GhostCorrosion corrosion() {
        return CORROSION;
    }

    /**
     * 返回模块展示图标的纹理位置。
     */
    @Override
    public ResourceLocation iconTexture() {
        return ResourceLocation.fromNamespaceAndPath(
                QisPlan2.MODID,
                "textures/item/ghost_divination_slip.png"
        );
    }

    /**
     * 返回压制配置界面使用的图标纹理位置。
     */
    @Override
    public ResourceLocation suppressionIconTexture() {
        return ResourceLocation.fromNamespaceAndPath(
                QisPlan2.MODID,
                "textures/item/death_sign.png"
        );
    }

    /**
     * 声明能够压制的目标标签；空集合表示没有额外的标签压制对象。
     */
    @Override
    public GhostClassification suppressionTargets() {
        return GhostClassification.of(
                GhostTag.ENTITY
        );
    }

    /**
     * 声明本厉鬼的灵异分类标签，供压制关系匹配使用。
     */
    @Override
    public GhostClassification classification() {
        return GhostClassification.of(
                GhostTag.ENTITY
        );
    }

    /**
     * 返回可分配的压制单位数量，供驾驭压制配置使用。
     */
    @Override
    public int suppressionUnits() {
        return 2;
    }

    /**
     * 返回每个压制单位对应的基础灵异强度。
     */
    @Override
    public double suppressionUnitStrength() {
        return 20.0D;
    }


    /**
     * 鬼签使用规则。
     */
    private static final class DivinationSlipUseRule
            implements GhostRule {

        /**
         * 获取当前定义的唯一标识。
         * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
         */
        @Override
        public ResourceLocation getId() {
            return USE_RULE_ID;
        }

        /**
         * 判断当前语义事件是否属于本规则处理的范围。
         * 这里只筛选事件种类或语义，不执行能力和世界操作。
         */
        @Override
        public boolean supports(GhostEvent event) {
            return event instanceof GhostUseEvent || event instanceof DivinationSlipResultEvent;
        }

        /**
         * 检查当前事实和模块上下文是否满足规则触发条件。
         * 返回 false 时跳过本规则，不提交能力或灵异影响。
         */
        @Override
        public boolean canTrigger(
                GhostRuleContext context,
                GhostEvent event
        ) {
            return (event instanceof GhostUseEvent useEvent && useEvent.user().level() instanceof ServerLevel)
                    || (event instanceof DivinationSlipResultEvent result && result.user().level() instanceof ServerLevel);
        }

        /**
         * 处理已满足条件的语义事件，发起对应能力或提交灵异影响。
         * 需要裁决的效果仅在获准后执行。
         */
        @Override
        public void onTrigger(
                GhostRuleContext context,
                GhostEvent event
        ) {
            LivingEntity user = event instanceof GhostUseEvent useEvent ? useEvent.user()
                    : ((DivinationSlipResultEvent)event).user();

            if (!(user.level() instanceof ServerLevel serverLevel)) {
                return;
            }

            // 本次事件只随机一次，后续使用固定结果。
            int result = event instanceof DivinationSlipResultEvent fixed ? fixed.result() : user.getRandom().nextInt(3);

            DivinationSlipResultEvent resultEvent =
                    new DivinationSlipResultEvent(user, result);

            switch (result) {
                case LIFE_RESULT -> {
                    GhostInfluenceCollector.collectCurses(
                            context,
                            user.getUUID(),
                            serverLevel.getServer()
                    );

                    context.submitAbilityInfluence(
                            resultAbility(result),
                            resultEvent,
                            user.getUUID(),
                            GhostInfluenceType.PROTECTION,
                            context.getModuleContext().intensity()
                    );
                }

                case DEATH_RESULT -> {
                    GhostInfluenceCollector.collectCurses(
                            context,
                            user.getUUID(),
                            serverLevel.getServer()
                    );

                    context.submitAbilityInfluence(
                            resultAbility(result),
                            resultEvent,
                            user.getUUID(),
                            GhostInfluenceType.ATTACK,
                            context.getModuleContext().intensity()
                    );
                }

                case GHOST_RESULT -> {
                    // 鬼签不参与保护/攻击竞争。
                    // 保持原有的无额外效果语义。
                    context.tryExecuteAbility(
                            resultAbility(result),
                            resultEvent
                    );
                }

                default -> {
                    // 随机范围受限，此处仅作防御性处理。
                }
            }
        }
    }

    /**
     * 执行已经确定结果的鬼签能力。
     *
     * 生签与死签的能力效果由统一裁决控制。
     * 复苏刺激仍由 GhostAbilityExecutor 负责。
     */
    private record DivinationSlipAbility(int result, String name) implements GhostAbility {
        /** 每种签文有自己的能力 ID。 */
        public ResourceLocation getId() { return resultAbility(result); }
        /** 各签文直接声明对应图标。 */
        public ResourceLocation iconTexture() { return ResourceLocation.fromNamespaceAndPath(MODID, "textures/item/" + name + "_sign.png"); }
        /** 规则提供抽签事实，主动输入提供作用对象，两者不是宿主引用。 */
        private LivingEntity recipient(GhostAbilityContext c) {
            if (c.event() instanceof DivinationSlipResultEvent event) return event.user();
            return c.parameters() instanceof GhostInvocationParameters input ? input.recipient() : null;
        }
        /** 目标必须处于服务端且存活；主动调用不需要伪造抽签事件。 */
        public boolean canExecute(GhostAbilityContext c) {
            var target = recipient(c);
            return target != null && target.isAlive() && target.level() instanceof ServerLevel;
        }
        /** 主动使用的复苏代价归模块声明，统一执行器负责结算。 */
        public GhostAbilityStimulation getStimulation(GhostAbilityContext c) {
            return c.source() == GhostAbilityContext.Source.POSSESSION
                    ? new GhostAbilityStimulation(20, GhostAbilityStimulation.Timing.ON_ACCEPTED)
                    : GhostAbilityStimulation.none();
        }
        /** 委托返回效果结果的实现。 */
        public void execute(GhostAbilityContext c) { executeWithResult(c); }
        /** 每个实例只执行自己声明的结果，没有中央技能预设。 */
        public boolean executeWithResult(GhostAbilityContext c) {
            var target = recipient(c);
            return switch (result) {
                case LIFE_RESULT -> useLife(target, c);
                case DEATH_RESULT -> useDeath(target, c);
                case GHOST_RESULT -> useGhost(target, c);
                default -> false;
            };
        }
    }

    /**
     * 执行生签效果，处理目标身上的生签诅咒或相应保护状态。
     */
    private static boolean useLife(LivingEntity user, GhostAbilityContext context) {
        sendResultIfPlayer(user, LIFE_RESULT);

        if (!(user.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        int strength = (int) context.getModuleContext().intensity();

        // 强度直接来自模块实例，不再读取旧驭鬼状态并替换成固定死机强度。
        GhostDivinationCurse existing = getLifeCurse(user.getUUID());

        if (existing != null) {
            existing.addTime(LIFE_DURATION);
            existing.setStrength(strength);
            CurseManager.markDirty();
            return true;
        }

        GhostDivinationCurseType curseType =
                new GhostDivinationCurseType();

        GhostDivinationCurseSource source =
                new GhostDivinationCurseSource();

        CompoundTag initialState = new CompoundTag();
        initialState.putInt("Strength", strength);
        initialState.putInt("RemainingTicks", LIFE_DURATION);

        Curse curse = curseType.create(
                user.getUUID(),
                source,
                initialState
        );

        CurseManager.add(serverLevel.getServer(), curse);
        return true;
    }

    /**
     * 获取指定实体当前的生签诅咒。
     */
    private static GhostDivinationCurse getLifeCurse(UUID target) {
        for (Curse curse : CurseManager.getByTarget(target)) {
            if (curse instanceof GhostDivinationCurse lifeCurse) {
                return lifeCurse;
            }
        }

        return null;
    }


    /**
     * 展示抽到鬼签的结果。
     * 当前鬼签结果没有额外世界效果，本方法不单独增加复苏；调用入口负责相应代价。
     */
    private static boolean useGhost(LivingEntity user, GhostAbilityContext context) {
        // 鬼签目前没有额外效果。
        sendResultIfPlayer(user, GHOST_RESULT);
        return true;
    }


    /**
     * 执行死签结果对应的灵异袭击，返回实际处理结果。
     */
    private static boolean useDeath(LivingEntity user, GhostAbilityContext context) {
        sendResultIfPlayer(user, DEATH_RESULT);

        return SupernaturalDeathHandler.tryKill(
                user,
                ModDamageTypes.ghostDivinationSlip(user),
                context.getModuleContext().intensity()
        );
    }

    /**
     * 仅向玩家发送鬼签结果展示特效。
     */
    private static void sendResultIfPlayer(
            LivingEntity user,
            int result
    ) {
        if (user instanceof ServerPlayer player) {
            GhostDivinationNetwork.sendResult(player, result);
        }
    }


}
