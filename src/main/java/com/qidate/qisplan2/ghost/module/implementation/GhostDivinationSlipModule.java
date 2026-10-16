
package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.curse.Curse;
import com.qidate.qisplan2.ghost.curse.CurseManager;
import com.qidate.qisplan2.ghost.curse.type.ghostdivination.GhostDivinationCurse;
import com.qidate.qisplan2.ghost.curse.type.ghostdivination.GhostDivinationCurseSource;
import com.qidate.qisplan2.ghost.curse.type.ghostdivination.GhostDivinationCurseType;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.GhostUseEvent;
import com.qidate.qisplan2.ghost.possession.ability.divinationslip.GhostDivinationSlipAbility;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import com.qidate.qisplan2.network.divinationslip.GhostDivinationNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

import static com.qidate.qisplan2.QisPlan2.MODID;

/**
 * 鬼签模块。
 *
 * 响应灵异使用事件，随机产生生、死、鬼三种结果。
 *
 * 本模块不依赖具体宿主类型。
 * 模块实例数据由宿主负责保存。
 */
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
            return event instanceof GhostUseEvent;
        }

        @Override
        public void onTrigger(
                GhostRuleContext context,
                GhostEvent event
        ) {
            context.tryExecuteAbility(
                    USE_ABILITY_ID,
                    event
            );
        }
    }

    /**
     * 鬼签使用能力。
     *
     * 通过 executeWithResult 返回效果执行结果，
     * 由 GhostAbilityExecutor 统一处理能力复苏刺激。
     */
    private static final class DivinationSlipUseAbility
            implements GhostAbility {

        @Override
        public ResourceLocation getId() {
            return USE_ABILITY_ID;
        }

        @Override
        public GhostAbilityStimulation getStimulation(
                GhostAbilityContext context
        ) {
            return new GhostAbilityStimulation(
                    1.0D,
                    GhostAbilityStimulation.Timing.ON_EFFECT_SUCCESS
            );
        }

        @Override
        public boolean canExecute(GhostAbilityContext context) {
            return context.event() instanceof GhostUseEvent useEvent
                    && useEvent.user() instanceof ServerPlayer;
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

            if (!(context.event() instanceof GhostUseEvent useEvent)) {
                return false;
            }

            if (!(useEvent.user() instanceof ServerPlayer player)) {
                return false;
            }

            // 0：生，1：死，2：鬼。
            int result = player.getRandom().nextInt(3);

            return switch (result) {
                case 0 -> useLife(player);
                case 1 -> useDeath(player);
                case 2 -> useGhost(player);
                default -> false;
            };
        }
    }

    /*
     * ========================================
     * 生签
     * ========================================
     */

    private static boolean useLife(ServerPlayer player) {
        GhostDivinationNetwork.sendResult(player, 0);

        int strength = LIFE_STRENGTH;

        PossessedGhostState state = PossessionHandler.getState(
                player,
                GhostDivinationSlipAbility.ID
        );

        if (state != null && state.stunTicks() > 0) {
            strength = LIFE_STRENGTH_STUN;
        }

        GhostDivinationCurse existing = getLifeCurse(player.getUUID());

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
                player.getUUID(),
                source,
                initialState
        );

        CurseManager.add(player.server, curse);
        return true;
    }

    /**
     * 获取玩家当前的生签诅咒。
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

    private static boolean useGhost(ServerPlayer player) {
        GhostDivinationNetwork.sendResult(player, 2);

        // 鬼签目前没有额外效果。
        return true;
    }

    /*
     * ========================================
     * 死签
     * ========================================
     */

    private static boolean useDeath(ServerPlayer player) {
        GhostDivinationNetwork.sendResult(player, 1);

        GhostDivinationCurse lifeCurse = getLifeCurse(player.getUUID());

        if (lifeCurse != null) {
            CurseManager.remove(
                    player.server,
                    lifeCurse.getId()
            );

            PossessionHandler.testStun(
                    player,
                    GhostDivinationSlipAbility.ID,
                    STUN_TIME
            );

            return true;
        }

        return SupernaturalDeathHandler.tryKill(
                player,
                ModDamageTypes.ghostDivinationSlip(player),
                DEATH_STRENGTH
        );
    }
}
