package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.curse.Curse;
import com.qidate.qisplan2.ghost.curse.CurseManager;
import com.qidate.qisplan2.ghost.curse.type.ghostdivination.GhostDivinationCurse;
import com.qidate.qisplan2.ghost.curse.type.ghostdivination.GhostDivinationCurseSource;
import com.qidate.qisplan2.ghost.curse.type.ghostdivination.GhostDivinationCurseType;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostRule;
import com.qidate.qisplan2.ghost.module.GhostRuleContext;
import com.qidate.qisplan2.ghost.module.ability.GhostAbility;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.GhostUseEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import com.qidate.qisplan2.ghost.possession.ability.divinationslip.GhostDivinationSlipAbility;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import com.qidate.qisplan2.network.divinationslip.GhostDivinationNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.UUID;

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

    private static final ResourceLocation USE_ABILITY_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "ghost_divination_slip/use"
            );

    private static final int LIFE_RESULT = 0;
    private static final int DEATH_RESULT = 1;
    private static final int GHOST_RESULT = 2;

    private static final int LIFE_DURATION = 5 * 60 * 20;

    private static final int LIFE_STRENGTH =
            GhostDivinationCurse.DEFAULT_STRENGTH;

    private static final int LIFE_STRENGTH_STUN =
            GhostDivinationCurse.STUN_STRENGTH;

    private static final long STUN_TIME = 5L * 60L * 20L;

    private static final double DEATH_STRENGTH = 50.0D;

    private static final List<GhostRule> RULES =
            List.of(new DivinationSlipUseRule());

    private static final List<GhostAbility> ABILITIES =
            List.of(new DivinationSlipUseAbility());

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public GhostModuleInitialData getInitialData() {
        return new GhostModuleInitialData(
                25.0D,
                0.0D
        );
    }

    @Override
    public List<GhostRule> getRules() {
        return RULES;
    }

    @Override
    public List<GhostAbility> getAbilities() {
        return ABILITIES;
    }

    /**
     * 一次已经确定签文结果的内部事件。
     *
     * 防止延迟执行时重新随机。
     */
    private record DivinationSlipResultEvent(
            LivingEntity user,
            int result
    ) implements GhostEvent {

        private DivinationSlipResultEvent {
            if (user == null) {
                throw new NullPointerException("user");
            }

            if (result < LIFE_RESULT || result > GHOST_RESULT) {
                throw new IllegalArgumentException(
                        "Invalid divination slip result: " + result
                );
            }
        }
    }

    /**
     * 鬼签使用规则。
     */
    private static final class DivinationSlipUseRule
            implements GhostRule {

        @Override
        public ResourceLocation getId() {
            return USE_RULE_ID;
        }

        @Override
        public boolean supports(GhostEvent event) {
            return event instanceof GhostUseEvent;
        }

        @Override
        public boolean canTrigger(
                GhostRuleContext context,
                GhostEvent event
        ) {
            return event instanceof GhostUseEvent useEvent
                    && useEvent.user().level() instanceof ServerLevel;
        }

        @Override
        public void onTrigger(
                GhostRuleContext context,
                GhostEvent event
        ) {
            if (!(event instanceof GhostUseEvent useEvent)) {
                return;
            }

            LivingEntity user = useEvent.user();

            if (!(user.level() instanceof ServerLevel)) {
                return;
            }

            // 本次事件只随机一次，后续使用固定结果。
            int result = user.getRandom().nextInt(3);

            DivinationSlipResultEvent resultEvent =
                    new DivinationSlipResultEvent(user, result);

            switch (result) {
                case LIFE_RESULT -> context.submitAbilityInfluence(
                        USE_ABILITY_ID,
                        resultEvent,
                        user.getUUID(),
                        GhostInfluenceType.PROTECTION,
                        LIFE_STRENGTH
                );

                case DEATH_RESULT -> context.submitAbilityInfluence(
                        USE_ABILITY_ID,
                        resultEvent,
                        user.getUUID(),
                        GhostInfluenceType.ATTACK,
                        DEATH_STRENGTH
                );

                case GHOST_RESULT -> {
                    // 鬼签不参与保护/攻击竞争。
                    // 保持原有的无额外效果语义。
                    context.tryExecuteAbility(
                            USE_ABILITY_ID,
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
    private static final class DivinationSlipUseAbility
            implements GhostAbility {

        @Override
        public ResourceLocation getId() {
            return USE_ABILITY_ID;
        }

        @Override
        public boolean canExecute(GhostAbilityContext context) {
            return context.event() instanceof DivinationSlipResultEvent resultEvent
                    && resultEvent.user().level() instanceof ServerLevel;
        }

        @Override
        public void execute(GhostAbilityContext context) {
            executeWithResult(context);
        }

        @Override
        public boolean executeWithResult(GhostAbilityContext context) {
            if (!context.isModuleAvailable()) {
                return false;
            }

            if (!(context.event()
                    instanceof DivinationSlipResultEvent resultEvent)) {
                return false;
            }

            LivingEntity user = resultEvent.user();

            if (!(user.level() instanceof ServerLevel)) {
                return false;
            }

            return switch (resultEvent.result()) {
                case LIFE_RESULT -> useLife(user);
                case DEATH_RESULT -> useDeath(user);
                case GHOST_RESULT -> useGhost(user);
                default -> false;
            };
        }
    }

    /*
     * ========================================
     * 生签
     * ========================================
     */

    private static boolean useLife(LivingEntity user) {
        sendResultIfPlayer(user, LIFE_RESULT);

        if (!(user.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        int strength = LIFE_STRENGTH;

        // 驭鬼者的死机状态只适用于玩家。
        if (user instanceof ServerPlayer player) {
            PossessedGhostState state = PossessionHandler.getState(
                    player,
                    GhostDivinationSlipAbility.ID
            );

            if (state != null && state.stunTicks() > 0) {
                strength = LIFE_STRENGTH_STUN;
            }
        }

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

    /*
     * ========================================
     * 鬼签
     * ========================================
     */

    private static boolean useGhost(LivingEntity user) {
        // 鬼签目前没有额外效果。
        sendResultIfPlayer(user, GHOST_RESULT);
        return true;
    }

    /*
     * ========================================
     * 死签
     * ========================================
     */

    private static boolean useDeath(LivingEntity user) {
        sendResultIfPlayer(user, DEATH_RESULT);

        return SupernaturalDeathHandler.tryKill(
                user,
                ModDamageTypes.ghostDivinationSlip(user),
                DEATH_STRENGTH
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