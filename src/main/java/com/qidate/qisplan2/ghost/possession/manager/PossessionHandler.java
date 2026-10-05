package com.qidate.qisplan2.ghost.possession.manager;

import com.qidate.qisplan2.core.ModAttachments;
import com.qidate.qisplan2.death.QisDeathHandler;
import com.qidate.qisplan2.event.GhostBreakoutHandler;
import com.qidate.qisplan2.ghost.corrosion.CorrosionMatrix;
import com.qidate.qisplan2.ghost.corrosion.CorrosionType;
import com.qidate.qisplan2.ghost.corrosion.GhostCorrosion;
import com.qidate.qisplan2.ghost.possession.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.possession.ability.GhostAbilityRegistry;
import com.qidate.qisplan2.ghost.possession.ability.PossessedGhostAbility;
import com.qidate.qisplan2.ghost.possession.ability.nightwanderer.NightWandererAbility;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostData;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostDomainData;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;
import com.qidate.qisplan2.ghost.possession.suppression.GhostSuppressionSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class PossessionHandler {

    private PossessionHandler() {
    }


    /*
     * ============================================================
     * 兼容旧代码
     * ============================================================
     */

    /**
     * 兼容旧代码的夜游鬼 ID。
     */
    @Deprecated
    public static final ResourceLocation NIGHT_WANDERER =
            NightWandererAbility.ID;


    /*
     * ============================================================
     * 非灵异伤害减免
     * ============================================================
     */

    public static double getNonSupernaturalDamageReduction(
            ServerPlayer player
    ) {

        Map<ResourceLocation, PossessedGhostData> ghosts =
                player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                );


        double bodyCorrosion =
                getEffectiveBodyCorrosion(player);


        double reduction =
                1.0D
                        - Math.pow(
                        0.5D,
                        bodyCorrosion / 20.0D
                );


        double reductionCap =
                0.90D;


        /*
         * ========================================================
         * 特殊鬼修改
         * ========================================================
         */

        for (ResourceLocation ghost :
                ghosts.keySet()) {

            PossessedGhostAbility ability =
                    GhostAbilityRegistry.get(ghost);

            if (ability == null) {
                continue;
            }


            reduction =
                    ability.modifyNonSupernaturalDamageReduction(
                            player,
                            reduction
                    );


            reductionCap =
                    ability.modifyNonSupernaturalDamageReductionCap(
                            player,
                            reductionCap
                    );
        }


        reductionCap =
                Math.clamp(
                        reductionCap,
                        0.0D,
                        1.0D
                );


        reduction =
                Math.clamp(
                        reduction,
                        0.0D,
                        reductionCap
                );


        return reduction;
    }


    /*
     * ============================================================
     * 肉身侵蚀
     * ============================================================
     */

    public static double getEffectiveBodyCorrosion(
            Player player
    ) {

        CorrosionMatrix matrix =
                getCorrosionMatrix(player);


        return (
                matrix.total(CorrosionType.GLOBAL)
                        + matrix.total(CorrosionType.SKIN)
                        + matrix.total(CorrosionType.FLESH)
                        + matrix.total(CorrosionType.BONE)
        ) / 4.0D;
    }


    public static CorrosionMatrix getCorrosionMatrix(
            Player player
    ) {

        CorrosionMatrix matrix =
                new CorrosionMatrix();


        Map<ResourceLocation, PossessedGhostData> ghosts =
                player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                );


        for (ResourceLocation ghost :
                ghosts.keySet()) {

            PossessedGhostAbility ability =
                    GhostAbilityRegistry.get(ghost);


            if (ability == null) {
                continue;
            }


            GhostCorrosion corrosion =
                    ability.corrosion();


            for (var entry :
                    corrosion.entries().entrySet()) {

                CorrosionType type =
                        entry.getKey();

                int amount =
                        entry.getValue();


                if (type == CorrosionType.GLOBAL) {

                    matrix.add(
                            CorrosionType.GLOBAL,
                            ghost,
                            CorrosionType.GLOBAL,
                            amount
                    );


                    for (CorrosionType other :
                            CorrosionType.values()) {

                        if (other != CorrosionType.GLOBAL) {

                            matrix.add(
                                    other,
                                    ghost,
                                    CorrosionType.GLOBAL,
                                    amount
                            );
                        }
                    }


                    continue;
                }


                matrix.add(
                        type,
                        ghost,
                        type,
                        amount
                );
            }
        }


        return matrix;
    }


    /*
     * ============================================================
     * 生命上限加成
     * ============================================================
     */

    public static double getMaxHealthBonus(
            ServerPlayer player
    ) {

        double bodyCorrosion =
                getEffectiveBodyCorrosion(player);


        double ratio =
                1.0D
                        - Math.pow(
                        0.5D,
                        bodyCorrosion / 20.0D
                );


        double health =
                40.0D * ratio;


        for (ResourceLocation ghost :
                player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                ).keySet()) {

            PossessedGhostAbility ability =
                    GhostAbilityRegistry.get(ghost);


            if (ability == null) {
                continue;
            }


            health =
                    ability.modifyMaxHealthBonus(
                            player,
                            health
                    );
        }


        return Math.max(
                0.0D,
                health
        );
    }


    /*
     * ============================================================
     * 浅死机
     * ============================================================
     */

    public static final double MAX_SHALLOW_STUN =
            PossessedGhostState.MAX_SHALLOW_STUN;


    /*
     * ============================================================
     * 复苏
     * ============================================================
     */

    public static PossessedGhostState addRevival(
            PossessedGhostState state,
            double revivalPercent
    ) {

        if (state == null) {
            return null;
        }


        if (revivalPercent <= 0.0D) {
            return state;
        }


        /*
         * 死机期间不复苏。
         */
        if (state.isAnyStun()) {
            return state;
        }


        double revival =
                state.revival();


        double shallowStun =
                state.shallowStun();


        /*
         * ========================================================
         * 浅死机优先抵消复苏增长。
         * ========================================================
         */

        double consumed =
                Math.min(
                        shallowStun,
                        revivalPercent
                );


        shallowStun -=
                consumed;


        double actualRevival =
                revivalPercent
                        - consumed;


        /*
         * ========================================================
         * 剩余部分进入复苏值。
         * ========================================================
         */

        revival +=
                actualRevival / 100.0D;


        revival =
                Math.min(
                        1.0D,
                        revival
                );


        return new PossessedGhostState(
                revival,
                state.strength(),
                shallowStun,
                state.stunTicks(),
                state.permanentStun(),
                state.lastAbilityUseTick()
        );
    }


    public static PossessedGhostState addRevival(
            ServerPlayer player,
            ResourceLocation ghost,
            PossessedGhostState state,
            double revivalPercent,
            CorrosionType type
    ) {

        if (state == null) {
            return null;
        }


        if (player == null
                || ghost == null) {

            return state;
        }


        if (type == null) {

            return addRevival(
                    state,
                    revivalPercent
            );
        }


        if (revivalPercent <= 0.0D) {
            return state;
        }


        double suppression =
                GhostSuppressionSystem.getSuppression(
                        player,
                        ghost
                );


        double actualRevival =
                revivalPercent
                        * (1.0D - suppression);


        return addRevival(
                state,
                actualRevival
        );
    }


    /*
     * ============================================================
     * 驭鬼
     * ============================================================
     */

    public static boolean possess(
            ServerPlayer player,
            ResourceLocation ghost
    ) {

        if (!GhostAbilityRegistry.contains(
                ghost
        )) {

            return false;
        }


        Map<ResourceLocation, PossessedGhostData> oldData =
                player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                );


        if (oldData.containsKey(ghost)) {
            return false;
        }


        PossessedGhostAbility ability =
                GhostAbilityRegistry.get(
                        ghost
                );


        if (ability == null) {
            return false;
        }


        /*
         * ========================================================
         * 创建完整持久数据。
         *
         * initialStrength：
         *     初始化厉鬼本体强度。
         *
         * initialDomainData：
         *     初始化这只厉鬼的鬼域数据。
         *
         * 注意：
         *     并不是所有厉鬼都有鬼域，
         *     因此 domain 由 Ability 自己决定是否存在。
         * ========================================================
         */

        PossessedGhostData data =
                PossessedGhostData.create(
                        ability.initialStrength()
                );


        Optional<PossessedGhostDomainData> initialDomainData =
                ability.initialDomainData();


        if (initialDomainData.isPresent()) {

            data =
                    data.withDomain(
                            initialDomainData.get()
                    );
        }


        Map<ResourceLocation, PossessedGhostData> newData =
                new HashMap<>(
                        oldData
                );


        newData.put(
                ghost,
                data
        );


        player.setData(
                ModAttachments.POSSESSED_GHOSTS,
                newData
        );


        /*
         * ========================================================
         * 通知 Ability。
         * ========================================================
         */

        ability.onPossess(
                new GhostAbilityContext(
                        player,
                        ghost,
                        data
                )
        );


        return true;
    }


    /*
     * ============================================================
     * 解除驾驭
     * ============================================================
     */

    public static boolean release(
            ServerPlayer player,
            ResourceLocation ghost
    ) {

        Map<ResourceLocation, PossessedGhostData> oldData =
                player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                );


        PossessedGhostData data =
                oldData.get(ghost);


        if (data == null) {
            return false;
        }


        PossessedGhostAbility ability =
                GhostAbilityRegistry.get(
                        ghost
                );


        if (ability != null) {

            ability.onRelease(
                    new GhostAbilityContext(
                            player,
                            ghost,
                            data
                    )
            );
        }


        Map<ResourceLocation, PossessedGhostData> newData =
                new HashMap<>(
                        oldData
                );


        newData.remove(
                ghost
        );


        player.setData(
                ModAttachments.POSSESSED_GHOSTS,
                newData
        );


        GhostSuppressionAllocationHandler.removeByGhost(
                player,
                ghost
        );


        return true;
    }


    /*
     * ============================================================
     * 查询
     * ============================================================
     */

    public static boolean hasGhost(
            ServerPlayer player,
            ResourceLocation ghost
    ) {

        return player.getData(
                ModAttachments.POSSESSED_GHOSTS
        ).containsKey(ghost);
    }


    /**
     * 获取某只鬼的完整持久数据。
     */
    public static PossessedGhostData getData(
            Player player,
            ResourceLocation ghost
    ) {

        return player.getData(
                ModAttachments.POSSESSED_GHOSTS
        ).get(ghost);
    }


    /**
     * 获取某只鬼的通用状态。
     */
    public static PossessedGhostState getState(
            Player player,
            ResourceLocation ghost
    ) {

        PossessedGhostData data =
                getData(
                        player,
                        ghost
                );


        if (data == null) {
            return null;
        }


        return data.state();
    }


    /**
     * 获取所有鬼的完整数据。
     *
     * 返回副本。
     */
    public static Map<
            ResourceLocation,
            PossessedGhostData
            > getAllData(
            Player player
    ) {

        return new HashMap<>(
                player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                )
        );
    }


    /**
     * 获取所有鬼的通用状态。
     *
     * 返回副本。
     *
     * 这是给旧系统/只关心 State 的代码使用的便利方法。
     */
    public static Map<
            ResourceLocation,
            PossessedGhostState
            > getAllStates(
            Player player
    ) {

        Map<
                ResourceLocation,
                PossessedGhostState
                > result =
                new HashMap<>();


        for (var entry :
                player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                ).entrySet()) {

            result.put(
                    entry.getKey(),
                    entry.getValue().state()
            );
        }


        return result;
    }


    /**
     * 清空玩家当前驾驭的全部厉鬼。
     */
    public static void clearAll(
            ServerPlayer player
    ) {

        player.setData(
                ModAttachments.POSSESSED_GHOSTS,
                new HashMap<>()
        );
    }


    /*
     * ============================================================
     * 设置完整数据
     * ============================================================
     */

    public static void setData(
            ServerPlayer player,
            ResourceLocation ghost,
            PossessedGhostData data
    ) {

        if (data == null) {
            return;
        }


        Map<ResourceLocation, PossessedGhostData> newData =
                new HashMap<>(
                        player.getData(
                                ModAttachments.POSSESSED_GHOSTS
                        )
                );


        newData.put(
                ghost,
                data
        );


        player.setData(
                ModAttachments.POSSESSED_GHOSTS,
                newData
        );
    }


    /*
     * ============================================================
     * 设置状态
     * ============================================================
     */

    public static void setState(
            ServerPlayer player,
            ResourceLocation ghost,
            PossessedGhostState state
    ) {

        if (state == null) {
            return;
        }


        PossessedGhostData oldData =
                getData(
                        player,
                        ghost
                );


        if (oldData == null) {
            return;
        }


        setData(
                player,
                ghost,
                oldData.withState(state)
        );
    }


    /*
     * ============================================================
     * 侵蚀
     * ============================================================
     */

    public static int getCorrosion(
            ServerPlayer player,
            CorrosionType type
    ) {

        return getCorrosionMatrix(player)
                .total(type);
    }


    public static EnumMap<
            CorrosionType,
            Integer
            > getAllCorrosion(
            ServerPlayer player
    ) {

        EnumMap<
                CorrosionType,
                Integer
                > result =
                new EnumMap<>(
                        CorrosionType.class
                );


        for (CorrosionType type :
                CorrosionType.values()) {

            result.put(
                    type,
                    getCorrosion(
                            player,
                            type
                    )
            );
        }


        return result;
    }


    public static double getCorrosionRatio(
            ServerPlayer player,
            ResourceLocation ghost,
            CorrosionType type
    ) {

        CorrosionMatrix matrix =
                getCorrosionMatrix(player);


        int total =
                matrix.total(type);


        if (total <= 0) {
            return 0.0D;
        }


        int own =
                matrix.contribution(
                        type,
                        ghost
                );


        return own / (double) total;
    }


    public static CorrosionMatrix getCorrosionMatrix(
            ServerPlayer player
    ) {

        CorrosionMatrix matrix =
                new CorrosionMatrix();


        Map<ResourceLocation, PossessedGhostData> ghosts =
                player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                );


        for (ResourceLocation ghost :
                ghosts.keySet()) {

            PossessedGhostAbility ability =
                    GhostAbilityRegistry.get(
                            ghost
                    );


            if (ability == null) {
                continue;
            }


            GhostCorrosion corrosion =
                    ability.corrosion();


            for (var entry :
                    corrosion.entries().entrySet()) {

                CorrosionType type =
                        entry.getKey();

                int amount =
                        entry.getValue();


                if (type == CorrosionType.GLOBAL) {

                    matrix.add(
                            CorrosionType.GLOBAL,
                            ghost,
                            CorrosionType.GLOBAL,
                            amount
                    );


                    for (CorrosionType other :
                            CorrosionType.values()) {

                        if (other != CorrosionType.GLOBAL) {

                            matrix.add(
                                    other,
                                    ghost,
                                    CorrosionType.GLOBAL,
                                    amount
                            );
                        }
                    }


                    continue;
                }


                matrix.add(
                        type,
                        ghost,
                        type,
                        amount
                );
            }
        }


        return matrix;
    }


    /*
     * ============================================================
     * 强度
     * ============================================================
     */

    /**
     * 获取一只已经驾驭的鬼当前实际发挥出来的强度。
     *
     * 这是其他系统读取“当前鬼有多强”的统一入口。
     */
    public static double getEffectiveStrength(
            Player player,
            ResourceLocation ghost
    ) {

        PossessedGhostState state =
                getState(
                        player,
                        ghost
                );


        if (state == null) {
            return 0.0D;
        }


        PossessedGhostAbility ability =
                GhostAbilityRegistry.get(
                        ghost
                );


        if (ability == null) {
            return 0.0D;
        }


        return GhostStrengthSystem.calculate(
                state,
                ability.minimumStrengthRatio()
        );
    }


    /**
     * 获取当前持久化强度。
     */
    public static double getStrength(
            Player player,
            ResourceLocation ghost
    ) {

        PossessedGhostState state =
                getState(
                        player,
                        ghost
                );


        if (state == null) {
            return 0.0D;
        }


        return state.strength();
    }


    /**
     * 增加指定鬼的强度。
     */
    public static boolean addStrength(
            ServerPlayer player,
            ResourceLocation ghost,
            double amount
    ) {

        PossessedGhostState state =
                getState(
                        player,
                        ghost
                );


        if (state == null) {
            return false;
        }


        PossessedGhostState newState =
                GhostStrengthSystem.addStrength(
                        state,
                        amount
                );


        setState(
                player,
                ghost,
                newState
        );


        return true;
    }


    /**
     * 直接设置指定鬼的当前强度。
     */
    public static boolean setStrength(
            ServerPlayer player,
            ResourceLocation ghost,
            double strength
    ) {

        PossessedGhostState state =
                getState(
                        player,
                        ghost
                );


        if (state == null) {
            return false;
        }


        PossessedGhostState newState =
                GhostStrengthSystem.setStrength(
                        state,
                        strength
                );


        setState(
                player,
                ghost,
                newState
        );


        return true;
    }


    /*
     * ============================================================
     * 浅死机
     * ============================================================
     */

    public static boolean addShallowStun(
            ServerPlayer player,
            ResourceLocation ghost,
            double amount
    ) {

        if (amount <= 0.0D) {
            return false;
        }


        PossessedGhostState state =
                getState(
                        player,
                        ghost
                );


        if (state == null) {
            return false;
        }


        PossessedGhostState newState =
                addShallowStun(
                        state,
                        amount
                );


        if (newState == state) {
            return false;
        }


        setState(
                player,
                ghost,
                newState
        );


        return true;
    }


    public static PossessedGhostState addShallowStun(
            PossessedGhostState state,
            double amount
    ) {

        if (state == null) {
            return null;
        }


        if (amount <= 0.0D) {
            return state;
        }


        double newShallowStun =
                Math.min(
                        PossessedGhostState.MAX_SHALLOW_STUN,
                        state.shallowStun()
                                + amount
                );


        return new PossessedGhostState(
                state.revival(),
                state.strength(),
                newShallowStun,
                state.stunTicks(),
                state.permanentStun(),
                state.lastAbilityUseTick()
        );
    }


    /*
     * ============================================================
     * 普通死机
     * ============================================================
     */

    public static boolean testStun(
            ServerPlayer player,
            ResourceLocation ghost,
            long ticks
    ) {

        PossessedGhostState state =
                getState(
                        player,
                        ghost
                );


        if (state == null) {
            return false;
        }


        PossessedGhostState newState =
                new PossessedGhostState(
                        state.revival(),
                        state.strength(),
                        state.shallowStun(),
                        Math.max(
                                1L,
                                ticks
                        ),
                        false,
                        state.lastAbilityUseTick()
                );


        setState(
                player,
                ghost,
                newState
        );


        return true;
    }


    /*
     * ============================================================
     * 永久死机
     * ============================================================
     */

    public static boolean testPermanentStun(
            ServerPlayer player,
            ResourceLocation ghost
    ) {

        PossessedGhostState state =
                getState(
                        player,
                        ghost
                );


        if (state == null) {
            return false;
        }


        PossessedGhostState newState =
                new PossessedGhostState(
                        state.revival(),
                        state.strength(),
                        state.shallowStun(),
                        0L,
                        true,
                        state.lastAbilityUseTick()
                );


        setState(
                player,
                ghost,
                newState
        );


        return true;
    }


    /*
     * ============================================================
     * Tick
     * ============================================================
     */

    public static void tick(
            ServerPlayer player
    ) {

        Map<ResourceLocation, PossessedGhostData> oldData =
                player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                );


        if (oldData.isEmpty()) {
            return;
        }


        Map<ResourceLocation, PossessedGhostData> data =
                new HashMap<>(
                        oldData
                );


        boolean changed =
                false;


        for (var entry :
                oldData.entrySet()) {

            ResourceLocation ghost =
                    entry.getKey();


            PossessedGhostData oldGhostData =
                    entry.getValue();


            PossessedGhostState state =
                    oldGhostData.state();


            double revival =
                    state.revival();


            double shallowStun =
                    state.shallowStun();


            long stunTicks =
                    state.stunTicks();


            boolean permanentStun =
                    state.permanentStun();


            /*
             * ====================================================
             * 永久死机
             * ====================================================
             */

            if (permanentStun) {

                /*
                 * 什么都不处理。
                 */
            }


            /*
             * ====================================================
             * 普通死机
             * ====================================================
             */

            else if (stunTicks > 0) {

                stunTicks--;

                changed = true;
            }


            /*
             * ====================================================
             * 复苏达到 100%
             * ====================================================
             */

            if (revival >= 1.0D) {

                GhostBreakoutHandler.breakout(
                        player
                );


                QisDeathHandler.forceKillAndCleanup(
                        player
                );


                return;
            }


            /*
             * ====================================================
             * 保存通用状态。
             * ====================================================
             */

            PossessedGhostState newState =
                    new PossessedGhostState(
                            revival,
                            state.strength(),
                            shallowStun,
                            stunTicks,
                            permanentStun,
                            state.lastAbilityUseTick()
                    );


            PossessedGhostData newData =
                    oldGhostData.withState(
                            newState
                    );


            data.put(
                    ghost,
                    newData
            );


            /*
             * ====================================================
             * 调用具体鬼的 Ability。
             * ====================================================
             */

            PossessedGhostAbility ability =
                    GhostAbilityRegistry.get(
                            ghost
                    );


            if (ability != null) {

                GhostAbilityContext context =
                        new GhostAbilityContext(
                                player,
                                ghost,
                                newData
                        );


                ability.tick(
                        context
                );


                PossessedGhostData afterAbility =
                        context.data();


                if (afterAbility != newData) {

                    changed = true;
                }


                data.put(
                        ghost,
                        afterAbility
                );
            }


            /*
             * ====================================================
             * 判断通用状态是否改变。
             * ====================================================
             */

            if (revival != state.revival()
                    || shallowStun != state.shallowStun()
                    || stunTicks != state.stunTicks()
                    || permanentStun != state.permanentStun()) {

                changed = true;
            }
        }


        /*
         * ========================================================
         * 统一提交。
         * ========================================================
         */

        if (changed) {

            player.setData(
                    ModAttachments.POSSESSED_GHOSTS,
                    data
            );
        }
    }


    /*
     * ============================================================
     * 主动能力
     * ============================================================
     */

    public static boolean useAbility(
            ServerPlayer player,
            ResourceLocation ghost,
            LivingEntity target
    ) {

        PossessedGhostData data =
                getData(
                        player,
                        ghost
                );


        if (data == null) {
            return false;
        }


        PossessedGhostAbility ability =
                GhostAbilityRegistry.get(
                        ghost
                );


        if (ability == null) {
            return false;
        }


        GhostAbilityContext context =
                new GhostAbilityContext(
                        player,
                        ghost,
                        data,
                        target
                );


        boolean success =
                ability.use(
                        context
                );


        if (success) {

            setData(
                    player,
                    ghost,
                    context.data()
            );
        }


        return success;
    }


    /*
     * ============================================================
     * 方块主动能力
     * ============================================================
     */

    public static boolean useAbilityOnBlock(
            ServerPlayer player,
            ResourceLocation ghost,
            BlockPos pos
    ) {

        PossessedGhostData data =
                getData(
                        player,
                        ghost
                );


        if (data == null) {
            return false;
        }


        PossessedGhostAbility ability =
                GhostAbilityRegistry.get(
                        ghost
                );


        if (ability == null) {
            return false;
        }


        GhostAbilityContext context =
                new GhostAbilityContext(
                        player,
                        ghost,
                        data
                );


        boolean success =
                ability.useOnBlock(
                        context,
                        pos
                );


        if (success) {

            setData(
                    player,
                    ghost,
                    context.data()
            );
        }


        return success;
    }


    /*
     * ============================================================
     * 所有鬼统一增加浅死机
     * ============================================================
     */

    /**
     * 让玩家驾驭的所有厉鬼增加浅死机值。
     *
     * @return 实际修改的厉鬼数量
     */
    public static int addShallowStunToAll(
            ServerPlayer player,
            double amount
    ) {

        if (amount <= 0.0D) {
            return 0;
        }


        Map<ResourceLocation, PossessedGhostData> oldData =
                player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                );


        if (oldData.isEmpty()) {
            return 0;
        }


        Map<ResourceLocation, PossessedGhostData> data =
                new HashMap<>(
                        oldData
                );


        int count =
                0;


        for (var entry :
                oldData.entrySet()) {

            PossessedGhostData oldGhostData =
                    entry.getValue();


            PossessedGhostState oldState =
                    oldGhostData.state();


            PossessedGhostState newState =
                    addShallowStun(
                            oldState,
                            amount
                    );


            if (newState != oldState) {

                data.put(
                        entry.getKey(),
                        oldGhostData.withState(
                                newState
                        )
                );


                count++;
            }
        }


        if (count > 0) {

            player.setData(
                    ModAttachments.POSSESSED_GHOSTS,
                    data
            );
        }


        return count;
    }
}