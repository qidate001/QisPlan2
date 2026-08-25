package com.qidate.qisplan2.ghost;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.ability.GhostAbilityRegistry;
import com.qidate.qisplan2.ghost.ability.PossessedGhostAbility;
import com.qidate.qisplan2.ghost.ability.nightwanderer.NightWandererAbility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

public final class PossessionHandler {

    private PossessionHandler() {
    }

    /**
     * 兼容旧代码的夜游鬼 ID。
     */
    @Deprecated
    public static final ResourceLocation NIGHT_WANDERER =
            NightWandererAbility.ID;

    /**
     * 白天浅死机值增长：
     * 1 点 / 10 秒
     */
    private static final double DAY_SHALLOW_STUN_PER_TICK =
            1.0D / 200.0D;

    /**
     * 浅死机值最大 100 点
     */
    public static final double MAX_SHALLOW_STUN = PossessedGhostState.MAX_SHALLOW_STUN;


    public static PossessedGhostState addRevival(
            PossessedGhostState state,
            double revivalPercent
    ) {
        if (revivalPercent <= 0.0D) {
            return state;
        }

        /*
         * 死机期间：
         * 不增加复苏。
         */
        if (state.isAnyStun()) {
            return state;
        }

        double revival =
                state.revival();

        double shallowStun =
                state.shallowStun();

        /*
         * 浅死机优先抵消复苏增长。
         */
        double consumed =
                Math.min(
                        shallowStun,
                        revivalPercent
                );

        shallowStun -= consumed;

        double actualRevival =
                revivalPercent
                        - consumed;

        /*
         * 剩余才进入复苏值。
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
                state.lastAbilityUseTick()
        );
    }


    /**
     * 驭鬼。
     */
    public static boolean possess(
            ServerPlayer player,
            ResourceLocation ghost
    ) {

        Map<ResourceLocation, PossessedGhostState> oldData =
                player.getData(QisPlan2.POSSESSED_GHOSTS);

        if (oldData.containsKey(ghost)) {
            return false;
        }

        Map<ResourceLocation, PossessedGhostState> data =
                new HashMap<>(oldData);

        PossessedGhostState state =
                PossessedGhostState.create();

        data.put(
                ghost,
                state
        );

        player.setData(
                QisPlan2.POSSESSED_GHOSTS,
                data
        );

        /*
         * 通知对应能力：
         * 玩家刚刚驾驭这只鬼。
         */
        PossessedGhostAbility ability =
                GhostAbilityRegistry.get(
                        ghost
                );

        if (ability != null) {

            ability.onPossess(
                    new GhostAbilityContext(
                            player,
                            ghost,
                            state
                    )
            );
        }

        return true;
    }


    /**
     * 解除驭鬼。
     */
    public static boolean release(
            ServerPlayer player,
            ResourceLocation ghost
    ) {

        Map<ResourceLocation, PossessedGhostState> oldData =
                player.getData(QisPlan2.POSSESSED_GHOSTS);

        if (!oldData.containsKey(ghost)) {
            return false;
        }

        Map<ResourceLocation, PossessedGhostState> data =
                new HashMap<>(oldData);

        PossessedGhostState state =
                oldData.get(ghost);

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

        data.remove(ghost);

        player.setData(
                QisPlan2.POSSESSED_GHOSTS,
                data
        );

        return true;
    }


    /**
     * 是否驾驭了某只鬼。
     */
    public static boolean hasGhost(
            ServerPlayer player,
            ResourceLocation ghost
    ) {

        return player.getData(
                QisPlan2.POSSESSED_GHOSTS
        ).containsKey(ghost);
    }


    /**
     * 获取某只鬼的状态。
     */
    public static PossessedGhostState getState(
            ServerPlayer player,
            ResourceLocation ghost
    ) {

        return player.getData(
                QisPlan2.POSSESSED_GHOSTS
        ).get(ghost);
    }

    /**
     * 设置某只鬼的状态。
     */
    public static void setState(
            ServerPlayer player,
            ResourceLocation ghost,
            PossessedGhostState state
    ) {

        Map<ResourceLocation, PossessedGhostState> data =
                new HashMap<>(
                        player.getData(
                                QisPlan2.POSSESSED_GHOSTS
                        )
                );

        data.put(
                ghost,
                state
        );

        player.setData(
                QisPlan2.POSSESSED_GHOSTS,
                data
        );
    }

    /**
     * 给指定厉鬼增加浅死机值。
     *
     * 自动限制在 0~100。
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
                        QisPlan2.POSSESSED_GHOSTS
                );

        PossessedGhostState state =
                oldData.get(ghost);

        if (state == null) {
            return false;
        }

        double newShallowStun =
                Math.min(
                        PossessedGhostState.MAX_SHALLOW_STUN,
                        state.shallowStun() + amount
                );

        Map<ResourceLocation, PossessedGhostState> data =
                new HashMap<>(oldData);

        data.put(
                ghost,
                new PossessedGhostState(
                        state.revival(),
                        newShallowStun,
                        state.stunTicks(),
                        state.permanentStun(),
                        state.lastAbilityUseTick()
                )
        );

        player.setData(
                QisPlan2.POSSESSED_GHOSTS,
                data
        );

        return true;
    }

    public static boolean testStun(
            ServerPlayer player,
            ResourceLocation ghost,
            long ticks
    ) {
        Map<ResourceLocation, PossessedGhostState> data =
                new HashMap<>(
                        player.getData(
                                QisPlan2.POSSESSED_GHOSTS
                        )
                );

        PossessedGhostState state =
                data.get(ghost);

        if (state == null) {
            return false;
        }

        data.put(
                ghost,
                new PossessedGhostState(
                        state.revival(),
                        state.shallowStun(),
                        Math.max(1L, ticks),
                        false,
                        state.lastAbilityUseTick()
                )
        );

        player.setData(
                QisPlan2.POSSESSED_GHOSTS,
                data
        );

        return true;
    }


    public static boolean testPermanentStun(
            ServerPlayer player,
            ResourceLocation ghost
    ) {
        Map<ResourceLocation, PossessedGhostState> data =
                new HashMap<>(
                        player.getData(
                                QisPlan2.POSSESSED_GHOSTS
                        )
                );

        PossessedGhostState state =
                data.get(ghost);

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
                        state.lastAbilityUseTick()
                )
        );

        player.setData(
                QisPlan2.POSSESSED_GHOSTS,
                data
        );

        return true;
    }


    public static void tick(
            ServerPlayer player
    ) {
        Map<ResourceLocation, PossessedGhostState> oldData =
                player.getData(QisPlan2.POSSESSED_GHOSTS);

        if (oldData.isEmpty()) {
            return;
        }

        Map<ResourceLocation, PossessedGhostState> data =
                new HashMap<>(oldData);

        boolean changed = false;

        boolean isDay =
                player.level().isDay();

        for (var entry : oldData.entrySet()) {

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
             * ========================================
             * 永久死机
             * ========================================
             *
             * 完全不复苏。
             */
            if (permanentStun) {

                // 什么都不处理

            }
            /*
             * ========================================
             * 普通死机
             * ========================================
             *
             * 死机期间：
             * 不复苏
             * 不增加浅死机
             */
            else if (stunTicks > 0) {

                stunTicks--;

            }

            /*
             * ========================================
             * 复苏达到 100%
             * ========================================
             */
            if (revival >= 1.0D) {

                player.setHealth(0.0F);

                return;
            }

            /*
             * 保存新的状态
             */
            PossessedGhostState newState =
                    new PossessedGhostState(
                            revival,
                            shallowStun,
                            stunTicks,
                            permanentStun,
                            state.lastAbilityUseTick()
                    );

            data.put(
                    ghost,
                    newState
            );

            PossessedGhostAbility ability =
                    GhostAbilityRegistry.get(
                            ghost
                    );

            if (ability != null) {

                PossessedGhostState currentState =
                        data.get(ghost);

                ability.tick(
                        new GhostAbilityContext(
                                player,
                                ghost,
                                currentState
                        )
                );
            }

            /*
             * 判断是否变化
             */
            if (revival != state.revival()
                    || shallowStun != state.shallowStun()
                    || stunTicks != state.stunTicks()
                    || permanentStun != state.permanentStun()) {

                changed = true;
            }
        }

        if (changed) {
            player.setData(
                    QisPlan2.POSSESSED_GHOSTS,
                    data
            );
        }
    }


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

        return ability.use(
                new GhostAbilityContext(
                        player,
                        ghost,
                        state,
                        target
                )
        );
    }

    /**
     * 让玩家驾驭的所有厉鬼增加浅死机值。
     *
     * @param player 玩家
     * @param amount 增加的浅死机值
     * @return 实际被修改的厉鬼数量
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
                        QisPlan2.POSSESSED_GHOSTS
                );

        int count = 0;

        for (ResourceLocation ghost :
                oldData.keySet()) {

            if (addShallowStun(
                    player,
                    ghost,
                    amount
            )) {
                count++;
            }
        }

        return count;
    }
}