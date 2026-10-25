package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.event.GhostPianoMusicHandler;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostRule;
import com.qidate.qisplan2.ghost.module.GhostRuleContext;
import com.qidate.qisplan2.ghost.module.ability.GhostAbility;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.GhostSoundEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import com.qidate.qisplan2.ghost.module.sound.GhostSoundDefinition;
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
     * 鬼钢琴模块提供的声音能力。
     *
     * 声音传播规则属于模块定义，不再由鬼钢琴方块实体决定。
     */
    @Override
    public List<GhostSoundDefinition> getSounds() {
        return List.of(
                new GhostSoundDefinition(
                        ID,
                        ResourceLocation.fromNamespaceAndPath(
                                MODID,
                                "ghost_piano_music"
                        ),
                        32.0D,
                        GhostPianoMusicHandler::isGhostMusicBlocker
                )
        );
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




    /**
     * 鬼钢琴音乐中断规则。
     *
     * 只处理由声音管理器确认的音乐中断事件。
     * 不负责检测听觉范围，也不负责判断其他声源是否仍然存在。
     */
    private static final class GhostPianoMusicRule
            implements GhostRule {

        @Override
        public ResourceLocation getId() {
            return MUSIC_INTERRUPTION_RULE_ID;
        }

        @Override
        public boolean supports(GhostEvent event) {
            return event instanceof GhostSoundEvent soundEvent
                    && soundEvent.soundType().equals(ID)
                    && soundEvent.type() == GhostSoundEvent.Type.INTERRUPTED;
        }

        @Override
        public boolean canTrigger(
                GhostRuleContext context,
                GhostEvent event
        ) {
            return event instanceof GhostSoundEvent soundEvent
                    && soundEvent.soundType().equals(ID)
                    && soundEvent.type() == GhostSoundEvent.Type.INTERRUPTED
                    && soundEvent.listener().isAlive()
                    && soundEvent.listener().level()
                    instanceof net.minecraft.server.level.ServerLevel;
        }

        @Override
        public void onTrigger(
                GhostRuleContext context,
                GhostEvent event
        ) {
            if (!(event instanceof GhostSoundEvent soundEvent)) {
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




    /**
     * 鬼钢琴音乐中断能力。
     *
     * 只有统一影响裁决允许执行时，才发动超自然攻击。
     */
    private static final class GhostPianoMusicAbility
            implements GhostAbility {

        @Override
        public ResourceLocation getId() {
            return MUSIC_INTERRUPTION_ABILITY_ID;
        }

        @Override
        public boolean canExecute(GhostAbilityContext context) {
            return context.event() instanceof GhostSoundEvent soundEvent
                    && soundEvent.soundType().equals(ID)
                    && soundEvent.type() == GhostSoundEvent.Type.INTERRUPTED
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

            if (!(context.event() instanceof GhostSoundEvent soundEvent)) {
                return false;
            }

            if (!canExecute(context)) {
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