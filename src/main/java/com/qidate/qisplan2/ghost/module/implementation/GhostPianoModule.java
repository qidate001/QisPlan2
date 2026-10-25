package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostRule;
import com.qidate.qisplan2.ghost.module.GhostRuleContext;
import com.qidate.qisplan2.ghost.module.ability.GhostAbility;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.GhostSoundInterruptionEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

import static com.qidate.qisplan2.QisPlan2.MODID;

/**
 * 鬼钢琴模块。
 *
 * 统一承载鬼钢琴的灵异强度、规则和能力。
 *
 * 当前阶段只建立模块定义与初始数据；
 * 音乐中断规则将在后续阶段迁入。
 */
public final class GhostPianoModule implements GhostModule {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "ghost_piano"
            );

    /**
     * 鬼钢琴放置时记录的隔绝初始化状态。
     */
    public static final String DATA_ISOLATION_CHECKED =
            "IsolationChecked";

    /**
     * 放置时是否处于灵异隔绝空间。
     */
    public static final String DATA_WAS_ISOLATED =
            "WasIsolated";

    /**
     * 放置时所属的 Region UUID。
     *
     * 使用字符串保存 UUID；
     * 空字符串代表没有取得有效 Region ID。
     */
    public static final String DATA_INITIAL_REGION_ID =
            "InitialRegionId";

    private static final double INITIAL_INTENSITY = 50.0D;

    private static final ResourceLocation MUSIC_INTERRUPTION_RULE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "ghost_piano/music_interruption"
            );

    private static final ResourceLocation MUSIC_INTERRUPTION_ABILITY_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "ghost_piano/music_interruption"
            );

    private static final List<GhostRule> RULES =
            List.of(new GhostPianoMusicRule());

    private static final List<GhostAbility> ABILITIES =
            List.of(new GhostPianoMusicAbility());

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public GhostModuleInitialData getInitialData() {
        return new GhostModuleInitialData(
                INITIAL_INTENSITY,
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
     * 读取放置时记录的隔绝状态。
     */
    public static boolean wasInitiallyIsolated(
            CompoundTag customData
    ) {
        return customData.getBoolean(DATA_WAS_ISOLATED);
    }

    /**
     * 读取放置时记录的 Region UUID。
     *
     * @return 有效 UUID；不存在或格式错误时返回 null
     */
    public static java.util.UUID getInitialRegionId(
            CompoundTag customData
    ) {
        String value =
                customData.getString(DATA_INITIAL_REGION_ID);

        if (value.isEmpty()) {
            return null;
        }

        try {
            return java.util.UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }




    private static final class GhostPianoMusicRule
            implements GhostRule {

        @Override
        public ResourceLocation getId() {
            return MUSIC_INTERRUPTION_RULE_ID;
        }

        @Override
        public boolean supports(GhostEvent event) {
            return event instanceof GhostSoundInterruptionEvent soundEvent
                    && soundEvent.soundType().equals(ID);
        }

        @Override
        public boolean canTrigger(
                GhostRuleContext context,
                GhostEvent event
        ) {
            return event instanceof GhostSoundInterruptionEvent soundEvent
                    && soundEvent.listener().isAlive()
                    && soundEvent.listener().level()
                    instanceof net.minecraft.server.level.ServerLevel;
        }

        @Override
        public void onTrigger(
                GhostRuleContext context,
                GhostEvent event
        ) {
            if (!(event instanceof GhostSoundInterruptionEvent soundEvent)) {
                return;
            }

            context.submitAbilityInfluence(
                    MUSIC_INTERRUPTION_ABILITY_ID,
                    soundEvent,
                    soundEvent.listener().getUUID(),
                    GhostInfluenceType.ATTACK,
                    context.getModuleContext().intensity()
            );
        }
    }



    private static final class GhostPianoMusicAbility
            implements GhostAbility {

        @Override
        public ResourceLocation getId() {
            return MUSIC_INTERRUPTION_ABILITY_ID;
        }

        @Override
        public boolean canExecute(GhostAbilityContext context) {
            return context.event()
                    instanceof GhostSoundInterruptionEvent soundEvent
                    && soundEvent.soundType().equals(ID)
                    && soundEvent.listener().isAlive()
                    && soundEvent.listener().level()
                    instanceof net.minecraft.server.level.ServerLevel;
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
                    instanceof GhostSoundInterruptionEvent soundEvent)) {
                return false;
            }

            if (!soundEvent.listener().isAlive()
                    || !(soundEvent.listener().level()
                    instanceof net.minecraft.server.level.ServerLevel)) {
                return false;
            }

            return SupernaturalDeathHandler.tryKill(
                    soundEvent.listener(),
                    ModDamageTypes.ghostPiano(soundEvent.listener()),
                    context.getModuleContext().intensity()
            );
        }
    }
}