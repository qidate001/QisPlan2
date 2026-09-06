package com.qidate.qisplan2.ghost;

import com.qidate.qisplan2.core.ModAttachments;
import com.qidate.qisplan2.event.GhostBreakoutHandler;
import com.qidate.qisplan2.ghost.ability.GhostAbilityRegistry;
import com.qidate.qisplan2.ghost.ability.PossessedGhostAbility;
import com.qidate.qisplan2.ghost.ability.nightwanderer.NightWandererAbility;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

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

    /**
     * 获取玩家当前的非灵异伤害减免。
     *
     * 基础效果：
     *
     * 0只鬼  = 0%
     * 1只鬼  = 50%
     * 2只鬼  = 75%
     * 3只鬼  = 87.5%
     * 4只鬼  = 90%
     *
     * 特殊鬼可以修改减伤数值以及减伤上限。
     */
    public static double getNonSupernaturalDamageReduction(
            ServerPlayer player
    ) {

        Map<ResourceLocation, PossessedGhostState> ghosts =
                player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                );


        int count =
                ghosts.size();


        /*
         * ========================================================
         * 基础减伤
         * ========================================================
         *
         * 1 - 1 / 2^n
         *
         * 1只 = 50%
         * 2只 = 75%
         * 3只 = 87.5%
         * 4只 = 93.75%
         *
         * 但基础上限为90%。
         */
        double reduction =
                count <= 0
                        ? 0.0D
                        : 1.0D - Math.pow(
                        0.5D,
                        count
                );


        /*
         * ========================================================
         * 基础减伤上限
         * ========================================================
         */
        double reductionCap =
                0.90D;


        /*
         * ========================================================
         * 允许特殊鬼修改减伤
         * ========================================================
         */
        for (ResourceLocation ghost :
                ghosts.keySet()) {

            PossessedGhostAbility ability =
                    GhostAbilityRegistry.get(
                            ghost
                    );


            if (ability == null) {
                continue;
            }


            /*
             * 修改当前减伤。
             */
            reduction =
                    ability.modifyNonSupernaturalDamageReduction(
                            player,
                            reduction
                    );


            /*
             * 修改减伤上限。
             */
            reductionCap =
                    ability.modifyNonSupernaturalDamageReductionCap(
                            player,
                            reductionCap
                    );
        }


        /*
         * ========================================================
         * 最终限制
         * ========================================================
         *
         * 防止特殊鬼把减伤弄成：
         *
         * < 0%
         * > 100%
         */
        return Math.clamp(
                reduction,
                0.0D,
                reductionCap);
    }


    /*
     * ============================================================
     * 浅死机
     * ============================================================
     */

    /**
     * 浅死机值最大 100 点。
     */
    public static final double MAX_SHALLOW_STUN =
            PossessedGhostState.MAX_SHALLOW_STUN;


    /*
     * ============================================================
     * 复苏
     * ============================================================
     */

    /**
     * 给状态增加复苏值。
     *
     * revivalPercent 使用百分比。
     *
     * 例如：
     *
     * 10.0 = 10%
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
         * ========================================================
         * 死机期间不复苏。
         * ========================================================
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
                shallowStun,
                state.stunTicks(),
                state.permanentStun(),
                state.lastAbilityUseTick(),
                state.intrinsicStrength()
        );
    }


    /*
     * ============================================================
     * 驭鬼
     * ============================================================
     */

    /**
     * 驭鬼。
     */
    public static boolean possess(
            ServerPlayer player,
            ResourceLocation ghost
    ) {

        /*
         * ========================================================
         * 必须存在对应 Ability。
         * ========================================================
         */

        if (!GhostAbilityRegistry.contains(
                ghost
        )) {

            return false;
        }


        Map<ResourceLocation, PossessedGhostState> oldData =
                player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                );


        /*
         * 已经驾驭。
         */

        if (oldData.containsKey(
                ghost
        )) {

            return false;
        }


        /*
         * ========================================================
         * 获取 Ability。
         * ========================================================
         */

        PossessedGhostAbility ability =
                GhostAbilityRegistry.get(
                        ghost
                );

        if (ability == null) {
            return false;
        }


        /*
         * ========================================================
         * 创建状态。
         *
         * 初始本质强度由 Ability 决定。
         * ========================================================
         */

        PossessedGhostState state =
                PossessedGhostState.create(
                        ability.initialIntrinsicStrength()
                );


        Map<ResourceLocation, PossessedGhostState> data =
                new HashMap<>(
                        oldData
                );


        data.put(
                ghost,
                state
        );


        player.setData(
                ModAttachments.POSSESSED_GHOSTS,
                data
        );


        /*
         * ========================================================
         * 通知 Ability：
         *
         * 玩家刚刚驾驭了这只鬼。
         * ========================================================
         */

        ability.onPossess(
                new GhostAbilityContext(
                        player,
                        ghost,
                        state
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

        Map<ResourceLocation, PossessedGhostState> oldData =
                player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                );


        if (!oldData.containsKey(
                ghost
        )) {

            return false;
        }


        PossessedGhostState state =
                oldData.get(
                        ghost
                );


        PossessedGhostAbility ability =
                GhostAbilityRegistry.get(
                        ghost
                );


        if (ability != null) {

            ability.onRelease(
                    new GhostAbilityContext(
                            player,
                            ghost,
                            state
                    )
            );
        }


        Map<ResourceLocation, PossessedGhostState> data =
                new HashMap<>(
                        oldData
                );


        data.remove(
                ghost
        );


        player.setData(
                ModAttachments.POSSESSED_GHOSTS,
                data
        );


        return true;
    }


    /*
     * ============================================================
     * 查询
     * ============================================================
     */

    /**
     * 是否驾驭了某只鬼。
     */
    public static boolean hasGhost(
            ServerPlayer player,
            ResourceLocation ghost
    ) {

        return player.getData(
                ModAttachments.POSSESSED_GHOSTS
        ).containsKey(
                ghost
        );
    }


    /**
     * 获取某只鬼的状态。
     */
    public static PossessedGhostState getState(
            ServerPlayer player,
            ResourceLocation ghost
    ) {

        return player.getData(
                ModAttachments.POSSESSED_GHOSTS
        ).get(
                ghost
        );
    }

    /**
     * 获取玩家当前驾驭的所有厉鬼状态。
     *
     * 返回副本，避免外部直接修改玩家内部 Attachment 数据。
     */
    public static Map<
            ResourceLocation,
            PossessedGhostState
            > getAllStates(
            ServerPlayer player
    ) {

        return new HashMap<>(
                player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                )
        );
    }

    /**
     * 清空玩家当前驾驭的全部厉鬼。
     *
     * 用于：
     *
     * 玩家死亡
     * 厉鬼破体
     * 特殊强制失去全部驾驭
     */
    public static void clearAll(
            ServerPlayer player
    ) {

        player.setData(
                ModAttachments.POSSESSED_GHOSTS,
                new HashMap<>()
        );
    }


    /**
     * 设置某只鬼的状态。
     */
    public static void setState(
            ServerPlayer player,
            ResourceLocation ghost,
            PossessedGhostState state
    ) {

        if (state == null) {
            return;
        }


        Map<ResourceLocation, PossessedGhostState> data =
                new HashMap<>(
                        player.getData(
                                ModAttachments.POSSESSED_GHOSTS
                        )
                );


        data.put(
                ghost,
                state
        );


        player.setData(
                ModAttachments.POSSESSED_GHOSTS,
                data
        );
    }


    /*
     * ============================================================
     * 强度
     * ============================================================
     */

    /**
     * 获取一只已经驾驭的鬼当前实际发挥出来的强度。
     *
     * 这个方法以后应该作为其他系统读取
     * “当前鬼有多强”的统一入口。
     */
    public static double getEffectiveStrength(
            ServerPlayer player,
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
     * 获取指定鬼当前的本质强度。
     */
    public static double getIntrinsicStrength(
            ServerPlayer player,
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


        return state.intrinsicStrength();
    }


    /**
     * 增加指定鬼的本质强度。
     */
    public static boolean addIntrinsicStrength(
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
                GhostStrengthSystem
                        .addIntrinsicStrength(
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
     * 直接设置指定鬼的本质强度。
     */
    public static boolean setIntrinsicStrength(
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
                GhostStrengthSystem
                        .setIntrinsicStrength(
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

    /**
     * 给指定厉鬼增加浅死机值。
     */
    public static boolean addShallowStun(
            ServerPlayer player,
            ResourceLocation ghost,
            double amount
    ) {

        if (amount <= 0.0D) {
            return false;
        }


        Map<ResourceLocation, PossessedGhostState> oldData =
                player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                );


        PossessedGhostState state =
                oldData.get(
                        ghost
                );


        if (state == null) {
            return false;
        }


        double newShallowStun =
                Math.min(
                        PossessedGhostState.MAX_SHALLOW_STUN,
                        state.shallowStun()
                                + amount
                );


        Map<ResourceLocation, PossessedGhostState> data =
                new HashMap<>(
                        oldData
                );


        data.put(
                ghost,
                new PossessedGhostState(
                        state.revival(),
                        newShallowStun,
                        state.stunTicks(),
                        state.permanentStun(),
                        state.lastAbilityUseTick(),
                        state.intrinsicStrength()
                )
        );


        player.setData(
                ModAttachments.POSSESSED_GHOSTS,
                data
        );


        return true;
    }


    /**
     * 给状态增加浅死机值。
     */
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
                newShallowStun,
                state.stunTicks(),
                state.permanentStun(),
                state.lastAbilityUseTick(),
                state.intrinsicStrength()
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

        Map<ResourceLocation, PossessedGhostState> data =
                new HashMap<>(
                        player.getData(
                                ModAttachments.POSSESSED_GHOSTS
                        )
                );


        PossessedGhostState state =
                data.get(
                        ghost
                );


        if (state == null) {
            return false;
        }


        data.put(
                ghost,
                new PossessedGhostState(
                        state.revival(),
                        state.shallowStun(),
                        Math.max(
                                1L,
                                ticks
                        ),
                        false,
                        state.lastAbilityUseTick(),
                        state.intrinsicStrength()
                )
        );


        player.setData(
                ModAttachments.POSSESSED_GHOSTS,
                data
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

        Map<ResourceLocation, PossessedGhostState> data =
                new HashMap<>(
                        player.getData(
                                ModAttachments.POSSESSED_GHOSTS
                        )
                );


        PossessedGhostState state =
                data.get(
                        ghost
                );


        if (state == null) {
            return false;
        }


        data.put(
                ghost,
                new PossessedGhostState(
                        state.revival(),
                        state.shallowStun(),
                        0L,
                        true,
                        state.lastAbilityUseTick(),
                        state.intrinsicStrength()
                )
        );


        player.setData(
                ModAttachments.POSSESSED_GHOSTS,
                data
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

        Map<ResourceLocation, PossessedGhostState> oldData =
                player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                );


        if (oldData.isEmpty()) {
            return;
        }


        Map<ResourceLocation, PossessedGhostState> data =
                new HashMap<>(
                        oldData
                );


        boolean changed =
                false;


        for (var entry :
                oldData.entrySet()) {

            ResourceLocation ghost =
                    entry.getKey();

            PossessedGhostState state =
                    entry.getValue();


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

                /*
                 * ========================================================
                 * 厉鬼复苏导致死亡。
                 *
                 * 这种死亡不会依赖 LivingDeathEvent，
                 * 所以必须主动触发破体。
                 * ========================================================
                 */

                GhostBreakoutHandler.breakout(
                        player
                );

                /*
                 * 然后再真正让玩家死亡。
                 */
                player.setHealth(
                        0.0F
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
                            shallowStun,
                            stunTicks,
                            permanentStun,
                            state.lastAbilityUseTick(),
                            state.intrinsicStrength()
                    );


            data.put(
                    ghost,
                    newState
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

                PossessedGhostState beforeAbility =
                        data.get(
                                ghost
                        );


                GhostAbilityContext context =
                        new GhostAbilityContext(
                                player,
                                ghost,
                                beforeAbility
                        );


                ability.tick(
                        context
                );


                PossessedGhostState afterAbility =
                        context.state();


                if (afterAbility != beforeAbility) {

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

        PossessedGhostState state =
                getState(
                        player,
                        ghost
                );


        if (state == null) {
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
                        state,
                        target
                );


        boolean success =
                ability.use(
                        context
                );


        /*
         * Ability 执行成功后，
         * 将 Context 中修改后的状态统一写回。
         */
        if (success) {

            setState(
                    player,
                    ghost,
                    context.state()
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

        PossessedGhostState state =
                getState(
                        player,
                        ghost
                );


        if (state == null) {
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
                        state
                );


        boolean success =
                ability.useOnBlock(
                        context,
                        pos
                );


        /*
         * Ability 执行成功后，
         * 统一提交状态。
         */
        if (success) {

            setState(
                    player,
                    ghost,
                    context.state()
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


        Map<ResourceLocation, PossessedGhostState> oldData =
                player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                );


        if (oldData.isEmpty()) {
            return 0;
        }


        Map<ResourceLocation, PossessedGhostState> data =
                new HashMap<>(
                        oldData
                );


        int count =
                0;


        for (var entry :
                oldData.entrySet()) {

            PossessedGhostState oldState =
                    entry.getValue();


            PossessedGhostState newState =
                    addShallowStun(
                            oldState,
                            amount
                    );


            if (newState != oldState) {

                data.put(
                        entry.getKey(),
                        newState
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