package com.qidate.qisplan2.ghost.module.implementation.knocking;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.corrosion.CorrosionType;
import com.qidate.qisplan2.ghost.corrosion.GhostCorrosion;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.DoorKnockHeardEvent;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleContext;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import com.qidate.qisplan2.ghost.possession.ability.ghostdoor.knocking.KnockingGhostDoorSystem;
import com.qidate.qisplan2.ghost.possession.classification.GhostClassification;
import com.qidate.qisplan2.ghost.possession.classification.GhostTag;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import static com.qidate.qisplan2.QisPlan2.MODID;

/**
 * 完整的敲门鬼模块：行为负责找门与靠近，媒介限定有效门和敲门声事件，
 * 听声规则提交延迟袭击裁决，能力负责敲门、鬼门传播和获准后的灵异袭击。
 */
public final class KnockingGhostModule implements GhostModule {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MODID, "knocking_ghost");
    public static final double INITIAL_INTENSITY = 5.0D;
    private static final ResourceLocation KNOCK = ResourceLocation.fromNamespaceAndPath(MODID, "knocking_ghost/knock");
    private static final ResourceLocation ATTACK = ResourceLocation.fromNamespaceAndPath(MODID, "knocking_ghost/attack");
    private static final ResourceLocation RULE = ResourceLocation.fromNamespaceAndPath(MODID, "knocking_ghost/hearing");
    private static final ResourceLocation MEDIUM = ResourceLocation.fromNamespaceAndPath(MODID, "knocking_ghost/door_sound");

    @Override public ResourceLocation getId() { return ID; }
    @Override public GhostModuleInitialData getInitialData() { return new GhostModuleInitialData(INITIAL_INTENSITY, 0); }
    @Override public double getSupernaturalDefense() { return 6; }
    @Override public List<Goal> createEntityGoals(ModularGhostEntity host) { return KnockingGhostBehavior.create(host); }

    /** 每次行为及延迟任务执行前重新检查，移除、休眠和死机立即阻止后续操作。 */
    public static boolean isActive(ModularGhostEntity host) {
        return host.isAlive() && !host.isSupernaturallyStunned()
                && host.getModules().stream().anyMatch(e -> e.id().equals(ID) && GhostModuleCombatState.available(e));
    }

    public static void knock(ModularGhostEntity host, BlockPos door) {
        if (isActive(host)) GhostAbilityExecutor.tryExecute(new GhostAbilityContext(
                host, ID, KNOCK, GhostAbilityContext.Source.BEHAVIOR, null, () -> {}, new DoorKnockParameters(door)));
    }

    @Override public List<GhostMedium> getMedia() {
        return List.of(new GhostMedium() {
            @Override public ResourceLocation getId() { return MEDIUM; }
            @Override public boolean allows(GhostAbilityContext c) {
                LivingEntity host = GhostModuleHosts.living(c.host());
                if (!GhostModuleHosts.available(c.host(), ID) || !(host.level() instanceof ServerLevel)) return false;
                if (c.parameters() instanceof DoorKnockParameters e) return c.abilityId().equals(KNOCK)
                        && KnockingGhostDoorSystem.isDoor(host.level(), e.door());
                return c.abilityId().equals(ATTACK) && c.event() instanceof DoorKnockHeardEvent e
                        && e.source() == host && e.target().isAlive() && e.target().level() == host.level();
            }
        });
    }

    @Override public List<GhostRule> getRules() {
        return List.of(new GhostRule() {
            @Override public ResourceLocation getId() { return RULE; }
            @Override public boolean supports(GhostEvent event) { return event instanceof DoorKnockHeardEvent; }
            @Override public boolean canTrigger(GhostRuleContext c, GhostEvent e) {
                return GhostModuleHosts.available(c.host(), ID)
                        && e instanceof DoorKnockHeardEvent heard && heard.source() == GhostModuleHosts.living(c.host());
            }
            @Override public void onTrigger(GhostRuleContext c, GhostEvent event) {
                DoorKnockHeardEvent e = (DoorKnockHeardEvent) event;
                // 听见声音时立即处理规则并提交裁决。获准效果安排延迟能力调用，
                // 延迟结束不再制造事件；执行器会重新检查模块、媒介与能力。
                Runnable attack = GhostAbilityExecutor.prepareDeferred(new GhostAbilityContext(
                        c.host(), ID, ATTACK, GhostAbilityContext.Source.RULE, e));
                if (attack != null) c.submitInfluence(e.target().getUUID(), GhostInfluenceType.ATTACK,
                        GhostModuleHosts.strength(c.host(), ID),
                        () -> KnockingGhostDoorSystem.scheduleModuleAttack(attack));
            }
        });
    }

    @Override public List<GhostAbility> getIntrinsicAbilities() {
        return List.of(new GhostAbility() {
            @Override public ResourceLocation getId() { return KNOCK; }
            @Override public boolean canExecute(GhostAbilityContext c) { return c.parameters() instanceof DoorKnockParameters; }
            @Override public void execute(GhostAbilityContext c) {
                LivingEntity host = GhostModuleHosts.living(c.host());
                KnockingGhostDoorSystem.knock((ServerLevel) host.level(), host,
                        ((DoorKnockParameters)c.parameters()).door(), c.getModuleContext().intensity());
            }
        }, new GhostAbility() {
            @Override public ResourceLocation getId() { return ATTACK; }
            @Override public boolean canExecute(GhostAbilityContext c) { return c.event() instanceof DoorKnockHeardEvent; }
            @Override public void execute(GhostAbilityContext c) { executeWithResult(c); }
            @Override public boolean executeWithResult(GhostAbilityContext c) {
                DoorKnockHeardEvent e = (DoorKnockHeardEvent)c.event();
                return SupernaturalDeathHandler.tryKill(e.target(),
                        ModDamageTypes.knockingGhost(GhostModuleHosts.living(c.host())), GhostModuleHosts.strength(c.host(), ID));
            }
        });
    }


    /**
     * 10 秒。
     */
    private static final long TEN_SECONDS = 200L;

    /**
     * 普通敲门：
     *
     * +10%
     */
    private static final double NORMAL_REVIVAL_GAIN = 10.0D;

    /**
     * 10 秒内再次敲门：
     *
     * +30%
     */
    private static final double RAPID_REVIVAL_GAIN = 30.0D;

    /**
     * 每个受到袭击的目标：
     *
     * +2%
     */
    private static final double TARGET_REVIVAL_BONUS = 2.0D;


    /**
     * 灵异侵蚀
     */
    private static final GhostCorrosion CORROSION =
            GhostCorrosion.builder()
                    .add(CorrosionType.GLOBAL, 20)
                    .add(CorrosionType.HAND, 20)
                    .build();


    /**
     * 敲门鬼完整复苏时的本质灵异强度。
     */


    /**
     * 敲门鬼目前没有 LivingEntity 主动能力。
     */
    @Override
    public boolean use(
            PlayerGhostModuleContext context
    ) {
        return false;
    }

    /**
     * 灵异侵蚀
     */
    @Override
    public GhostCorrosion corrosion() {
        return CORROSION;
    }

    /**
     * 厉鬼标签
     */
    @Override
    public GhostClassification classification() {

        return GhostClassification.of(
                GhostTag.ENTITY,
                GhostTag.EXISTENCE_FORM,
                GhostTag.DOOR
        );
    }

    @Override
    public GhostClassification suppressionTargets() {
        return GhostClassification.empty();
    }

    @Override
    public int suppressionUnits() {
        return 1;
    }

    @Override
    public double suppressionUnitStrength() {
        return 20.0D;
    }

    /**
     * Shift + 右键门。
     */
    @Override
    public boolean useOnBlock(
            PlayerGhostModuleContext context,
            BlockPos clickedPos
    ) {

        ServerPlayer player =
                context.player();

        if (!(player.level()
                instanceof ServerLevel serverLevel)) {

            return false;
        }


        /*
         * ========================================================
         * 找到门的下半部分
         * ========================================================
         */

        BlockPos doorPos =
                getLowerDoorPos(
                        serverLevel,
                        clickedPos
                );

        if (doorPos == null) {
            return false;
        }


        /*
         * ========================================================
         * 计算这一次“直接敲下这扇门”
         * 有多少生物听见。
         *
         * 注意：
         *
         * 鬼门后续传播产生的其他门，
         * 不在这里重复增加驾驭者复苏。
         * ========================================================
         */

        int targetCount =
                KnockingGhostDoorSystem.countHearingTargets(
                        serverLevel,
                        player,
                        doorPos
                );


        /*
         * ========================================================
         * 真正执行敲门。
         *
         * 包括：
         *
         * 声音
         * 延迟攻击
         * 鬼门
         * 50 格共鸣
         * 50 层递归
         * ========================================================
         */

        double attackStrength =
                PossessionHandler.getEffectiveStrength(
                        player,
                        ID
                );

        KnockingGhostDoorSystem.knock(
                serverLevel,
                player,
                doorPos,
                attackStrength
        );


        /*
         * ========================================================
         * 驾驭代价
         * ========================================================
         */

        PossessedGhostState state =
                context.state();

        long now =
                serverLevel.getGameTime();

        boolean rapid =
                state.lastAbilityUseTick() > 0
                        && now - state.lastAbilityUseTick()
                        < TEN_SECONDS;


        double baseGain =
                rapid
                        ? RAPID_REVIVAL_GAIN
                        : NORMAL_REVIVAL_GAIN;


        double revivalGain =
                baseGain
                        + targetCount
                        * TARGET_REVIVAL_BONUS;


        PossessedGhostState newState =
                PossessionHandler.addRevival(
                        player,
                        ID,
                        state,
                        revivalGain,
                        CorrosionType.HAND
                );


        /*
         * 记录能力使用时间。
         */

        newState =
                new PossessedGhostState(
                        newState.revival(),
                        state.strength(),
                        newState.shallowStun(),
                        newState.stunTicks(),
                        newState.permanentStun(),
                        now
                );

        context.setState(
                newState
        );


        /*
         * ========================================================
         * 普通门：
         *
         * 敲一次就毁掉。
         *
         * 鬼门：
         * 不毁。
         * ========================================================
         */

        if (!KnockingGhostDoorSystem.isGhostDoor(
                serverLevel,
                doorPos
        )) {

            serverLevel.destroyBlock(
                    doorPos,
                    false
            );
        }


        /*
         * ========================================================
         * 复苏到 100%
         * ========================================================
         */

        if (newState.revival()
                >= 1.0D) {

            player.setHealth(0.0F);
        }


        return true;
    }

    /**
     * 获取门的下半部分。
     */
    private static BlockPos getLowerDoorPos(
            ServerLevel level,
            BlockPos clickedPos
    ) {

        BlockState state =
                level.getBlockState(
                        clickedPos
                );

        /*
         * ========================================================
         * 原版 DoorBlock
         * ========================================================
         */
        if (state.getBlock()
                instanceof DoorBlock) {

            if (!state.hasProperty(
                    DoorBlock.HALF
            )) {
                return null;
            }

            if (state.getValue(
                    DoorBlock.HALF
            ) == DoubleBlockHalf.UPPER) {

                return clickedPos.below();
            }

            return clickedPos;
        }

        /*
         * ========================================================
         * 其他标签门
         * ========================================================
         */
        if (state.is(BlockTags.DOORS)) {

            if (state.hasProperty(
                    BlockStateProperties.DOUBLE_BLOCK_HALF
            )) {

                if (state.getValue(
                        BlockStateProperties.DOUBLE_BLOCK_HALF
                ) == DoubleBlockHalf.UPPER) {

                    return clickedPos.below();
                }
            }

            return clickedPos;
        }

        return null;
    }

}
