package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.ghost.module.suppression.GhostSuppression;
import com.qidate.qisplan2.ghost.module.GhostModuleCombatState;
import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityInfluence;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
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
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import static com.qidate.qisplan2.QisPlan2.MODID;

/**
 * 鬼钢琴模块。
 *
 * 统一承载鬼钢琴的灵异强度、规则和能力。
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
            List.of(musicSwitch(true), musicSwitch(false), new GhostPianoMusicAbility());

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
    public List<GhostAbility> getIntrinsicAbilities() {
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
                        32.0D
                )
        );
    }

    /** 鬼音乐媒介成立时，以当前模块本质强度的 30% 持续压制其他模块。 */
    @Override public GhostSuppression soundSuppression(
            GhostModuleContext context, ResourceLocation soundType) {
        return ID.equals(soundType) ? new GhostSuppression(
                GhostSuppression.Kind.NORMAL, context.intensity() * 0.30D) : null;
    }

    /** 自主状态默认播放；显式开始的音乐不因规则死机而被取消。 */
    @Override public boolean soundEnabled(GhostModuleData.Entry entry, boolean automatic) {
        var data = entry.customData();
        boolean playing = data.contains("musicPlaying") ? data.getBoolean("musicPlaying") : automatic;
        return playing && (data.getBoolean("manualMusic") || GhostModuleCombatState.available(entry));
    }

    /** 主动音乐的中断仍是听觉事实，私有袭击只能由该持续流程调用。 */
    @Override public void onAbilityEvent(GhostModuleContext context, GhostEvent event) {
        if (context.getCustomData().getBoolean("manualMusic") && event instanceof GhostSoundEvent sound
                && sound.soundType().equals(ID) && sound.type() == GhostSoundEvent.Type.INTERRUPTED)
            context.continueAbility(MUSIC_INTERRUPTION_ABILITY_ID, event);
    }

    /** 播放和停止是公开技能，声音传播统一由管理器按模块状态同步。 */
    private static GhostAbility musicSwitch(boolean playing) {
        return new GhostAbility() {
            /** 返回能力或模块的稳定标识。 */
            public ResourceLocation getId() { return ResourceLocation.fromNamespaceAndPath(MODID, "ghost_piano/" + (playing ? "play" : "stop")); }
            /** 声明技能使用的图标纹理。 */
            public ResourceLocation iconTexture() { return ResourceLocation.parse("qisplan2:textures/item/ghost_piano.png"); }
            /** 检查本次执行所需的环境与语义条件。 */
            public boolean canExecute(GhostAbilityContext c) { return c.getModuleContext().world().present(); }
            /** 在通过统一执行器后更新本能力的状态与效果。 */
            public void execute(GhostAbilityContext c) {
                var data = c.getModuleContext().getCustomData();
                data.putBoolean("musicPlaying", playing);
                data.putBoolean("manualMusic", true);
                c.getModuleContext().setCustomData(data);
            }
        };
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
    public static UUID getInitialRegionId(
            CompoundTag customData
    ) {
        String value =
                customData.getString(DATA_INITIAL_REGION_ID);

        if (value.isEmpty()) {
            return null;
        }

        try {
            return UUID.fromString(value);
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
            return !context.getModuleContext().getCustomData().getBoolean("manualMusic")
                    && event instanceof GhostSoundEvent soundEvent
                    && soundEvent.soundType().equals(ID)
                    && soundEvent.type() == GhostSoundEvent.Type.INTERRUPTED
                    && soundEvent.listener().isAlive()
                    && soundEvent.listener().level()
                    instanceof ServerLevel;
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
        /** 仅由已满足语义的内部流程发动，不允许独立点选袭击。 */
        @Override public boolean isPublicSkill() { return false; }

        /** 主动渠道与规则使用相同的袭击/保护语义，获准前不执行世界效果。 */
        @Override public GhostAbilityInfluence activeInfluence(GhostAbilityContext context) {
            return GhostAbilityInfluence.of(context.event() instanceof GhostSoundEvent sound ? sound.listener() : null, GhostInfluenceType.ATTACK, context.getModuleContext().intensity());
        }

            /** 技能图标由所属模块直接声明。 */
            @Override public ResourceLocation iconTexture() { return ResourceLocation.parse("qisplan2:textures/item/ghost_piano.png"); }


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
                    instanceof ServerLevel;
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

            if (!(context.event() instanceof GhostSoundEvent soundEvent)) return false;

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
