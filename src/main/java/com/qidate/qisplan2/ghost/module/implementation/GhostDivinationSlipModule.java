
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

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
        public boolean canExecute(GhostAbilityContext context) {
            return context.event() instanceof GhostUseEvent useEvent
                    && useEvent.user().level() instanceof ServerLevel;
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

            LivingEntity user = useEvent.user();

            if (!(user.level() instanceof ServerLevel)) {
                return false;
            }

            int result = user.getRandom().nextInt(3);

            return switch (result) {
                case 0 -> useLife(user);
                case 1 -> useDeath(user);
                case 2 -> useGhost(user);
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
        // 只有玩家需要接收结果展示特效。
        sendResultIfPlayer(user, 0);

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
        sendResultIfPlayer(user, 2);
        return true;
    }

    /*
     * ========================================
     * 死签
     * ========================================
     */

    private static boolean useDeath(LivingEntity user) {
        sendResultIfPlayer(user, 1);

        if (!(user.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        GhostDivinationCurse lifeCurse = getLifeCurse(user.getUUID());

        if (lifeCurse != null) {
            CurseManager.remove(
                    serverLevel.getServer(),
                    lifeCurse.getId()
            );

            // 只有玩家才有驭鬼者状态及对应的死机逻辑。
            if (user instanceof ServerPlayer player) {
                PossessionHandler.testStun(
                        player,
                        GhostDivinationSlipAbility.ID,
                        STUN_TIME
                );
            }

            return true;
        }

        return SupernaturalDeathHandler.tryKill(
                user,
                ModDamageTypes.ghostDivinationSlip(user),
                DEATH_STRENGTH
        );
    }

    /**
     * 仅向玩家发送鬼签结果展示特效。
     */
    private static void sendResultIfPlayer(LivingEntity user, int result) {
        if (user instanceof ServerPlayer player) {
            GhostDivinationNetwork.sendResult(player, result);
        }
    }
}
