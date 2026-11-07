package com.qidate.qisplan2.ghost.possession.manager;

import com.qidate.qisplan2.death.QisDeathHandler;
import com.qidate.qisplan2.event.GhostBreakoutHandler;
import com.qidate.qisplan2.ghost.corrosion.CorrosionMatrix;
import com.qidate.qisplan2.ghost.corrosion.CorrosionType;
import com.qidate.qisplan2.ghost.corrosion.GhostCorrosion;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleCombatState;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityExecutor;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import com.qidate.qisplan2.ghost.module.host.player.PlayerAbilityParameters;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostData;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleContext;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import com.qidate.qisplan2.ghost.module.host.player.PlayerModuleAbility;
import com.qidate.qisplan2.ghost.module.host.player.PlayerModuleEvents;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostData;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;
import com.qidate.qisplan2.ghost.possession.suppression.GhostSuppressionSystem;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class PossessionHandler {

    private PossessionHandler() {
    }

    /*
     * ============================================================
     * 非灵异伤害减免
     * ============================================================
     */

    public static double getNonSupernaturalDamageReduction(
            ServerPlayer player
    ) {

        Map<ResourceLocation, PossessedGhostData> ghosts =
                PlayerGhostData.views(player);


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

            GhostModule ability =
                    GhostModuleRegistry.get(ghost);

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
                PlayerGhostData.views(player);


        for (ResourceLocation ghost :
                ghosts.keySet()) {

            GhostModule ability =
                    GhostModuleRegistry.get(ghost);


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
                PlayerGhostData.views(player).keySet()) {

            GhostModule ability =
                    GhostModuleRegistry.get(ghost);


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
        GhostModule module = GhostModuleRegistry.get(ghost);
        if (module == null) return false;
        var initial = module.getInitialData();
        return possess(player, new GhostModuleData.Entry(ghost, initial.intensity(), initial.revival(), false, false,
                new CompoundTag()));
    }


    /*
     * ============================================================
     * 解除驾驭
     * ============================================================
     */

    /** 将完整的模块实例交给玩家，不再按驾驭 Ability 重新生成一份数据。 */
    public static boolean possess(ServerPlayer player, GhostModuleData.Entry entry) {
        GhostModule module = GhostModuleRegistry.get(entry.id());
        if (module == null || !player.isAlive()) return false;
        var host = new PlayerGhostModuleHost(player);
        if (!host.importModule(entry)) return false;
        module.onPossess(new PlayerGhostModuleContext(player, entry.id(), getData(player, entry.id())));
        return true;
    }

    public static boolean release(
            ServerPlayer player,
            ResourceLocation ghost
    ) {

        Map<ResourceLocation, PossessedGhostData> oldData =
                PlayerGhostData.views(player);


        PossessedGhostData data =
                oldData.get(ghost);


        if (data == null) {
            return false;
        }


        GhostModule ability =
                GhostModuleRegistry.get(
                        ghost
                );


        if (ability != null) {

            ability.onRelease(
                    new PlayerGhostModuleContext(
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


        PlayerGhostData.writeViews(player,
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

        return PlayerGhostData.views(player).containsKey(ghost);
    }


    /**
     * 获取某只鬼的完整持久数据。
     */
    public static PossessedGhostData getData(
            Player player,
            ResourceLocation ghost
    ) {

        return PlayerGhostData.views(player).get(ghost);
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
                PlayerGhostData.views(player)
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
                PlayerGhostData.views(player).entrySet()) {

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
        for (var entry : List.copyOf(new PlayerGhostModuleHost(player).getModules())) release(player, entry.id());
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

        if (data == null || !new PlayerGhostModuleHost(player).hasModule(ghost)) {
            return;
        }


        Map<ResourceLocation, PossessedGhostData> newData =
                new HashMap<>(
                        PlayerGhostData.views(player)
                );


        newData.put(
                ghost,
                data
        );


        PlayerGhostData.writeViews(player,
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


    public static EnumMap<CorrosionType, Integer> getAllCorrosion(
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
                PlayerGhostData.views(player);


        for (ResourceLocation ghost :
                ghosts.keySet()) {

            GhostModule ability =
                    GhostModuleRegistry.get(
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


        GhostModule ability =
                GhostModuleRegistry.get(
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
        var host = new PlayerGhostModuleHost(player);
        var entry = host.getModules().stream().filter(e -> e.id().equals(ghost)).findFirst().orElse(null);
        if (entry == null) return false;
        GhostModuleCombatState.set(host, entry, (int)Math.clamp(ticks, 1, Integer.MAX_VALUE), false);
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
        // 无期限死机统一写入模块 deadlocked，由模块命令负责解除。
        return new PlayerGhostModuleHost(player).setDeadlocked(ghost, true);

    }


    /*
     * ============================================================
     * Tick
     * ============================================================
     */

    public static void tick(
            ServerPlayer player
    ) {
        var host = new PlayerGhostModuleHost(player);
        GhostModuleCombatState.tick(host);
        for (var entry : List.copyOf(host.getModules())) {
            GhostModule module = GhostModuleRegistry.get(entry.id());
            if (module == null || !host.hasModule(entry.id())) continue;
            if (entry.revival() >= module.getMaxRevival()) {
                GhostBreakoutHandler.breakout(player);
                QisDeathHandler.forceKillAndCleanup(player);
                return;
            }
            // 玩家持续能力（如已主动展开的鬼域）独立于自动规则；死机不撤销主动能力。
            // 规则是否失效由下面的 GhostModuleRuntime 统一判断。
            module.onPlayerTick(new PlayerGhostModuleContext(player, entry.id(), getData(player, entry.id())));
            if (!player.isAlive()) return;
        }
        GhostModuleRuntime.dispatch(host, new ModuleTickEvent());
        PlayerModuleEvents.tickTouch(player, host);
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
        GhostModule module = GhostModuleRegistry.get(ghost);
        var host = new PlayerGhostModuleHost(player);
        if (module == null || !host.hasModule(ghost)) return false;
        return GhostAbilityExecutor.tryExecuteWithResult(new GhostAbilityContext(host, ghost, PlayerModuleAbility.id(module),
                GhostAbilityContext.Source.POSSESSION, null, () -> {}, new PlayerAbilityParameters(target, null)));
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
        GhostModule module = GhostModuleRegistry.get(ghost);
        var host = new PlayerGhostModuleHost(player);
        if (module == null || !host.hasModule(ghost)) return false;
        return GhostAbilityExecutor.tryExecuteWithResult(new GhostAbilityContext(host, ghost, PlayerModuleAbility.id(module),
                GhostAbilityContext.Source.POSSESSION, null, () -> {}, new PlayerAbilityParameters(null, pos)));
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
                PlayerGhostData.views(player);


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

            PlayerGhostData.writeViews(player,
                    data
            );
        }


        return count;
    }
}