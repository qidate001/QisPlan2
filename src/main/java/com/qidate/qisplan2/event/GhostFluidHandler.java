package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModAttachments;
import com.qidate.qisplan2.core.ModFluids;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.fluid.GhostFluidConfig;
import com.qidate.qisplan2.ghost.PossessedGhostState;
import com.qidate.qisplan2.ghost.PossessionHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;

/**
 * 所有灵异液体的统一处理器。
 *
 * 目前支持：
 *
 * - 鬼湖水
 *
 * 以后加入：
 *
 * - 鬼血
 * - 鬼血湖湖水
 *
 * 三者共用这一套逻辑。
 */
@EventBusSubscriber(
        modid = QisPlan2.MODID
)
public final class GhostFluidHandler {

    private GhostFluidHandler() {
    }

    /**
     * 每秒结算一次。
     */
    private static final long DAMAGE_INTERVAL =
            20L;

    /*
     * ============================================================
     * 灵异液体配置
     * ============================================================
     */

    /**
     * 鬼湖水。
     *
     * 使用：
     *
     *     深度
     *
     * 作为 immersion。
     */
    private static final GhostFluidConfig GHOST_LAKE =
            new GhostFluidConfig(
                    ModFluids.GHOST_LAKE_WATER_TYPE,
                    true,

                    /*
                     * 每 3 格深度：
                     *
                     * +10 强度
                     */
                    10.0D,

                    /*
                     * 最大 100 强度。
                     */
                    100.0D,

                    /*
                     * 每格深度：
                     *
                     * 每秒减少 5% 复苏值。
                     */
                    5.0D,

                    ModDamageTypes::ghostLakeWater
            );

    /**
     * 所有灵异液体。
     *
     * 后续鬼血、鬼血湖湖水
     * 都加入这里。
     */
    private static final GhostFluidConfig[] CONFIGS = {

            GHOST_LAKE

    };


    /*
     * ============================================================
     * Entity Tick
     * ============================================================
     */

    @SubscribeEvent
    public static void onEntityTick(
            EntityTickEvent.Post event
    ) {

        Entity entity =
                event.getEntity();

        /*
         * 只在服务端处理。
         */
        if (entity.level().isClientSide()) {
            return;
        }

        /*
         * 只处理 LivingEntity。
         */
        if (!(entity instanceof LivingEntity living)
                || !living.isAlive()) {

            return;
        }

        /*
         * ========================================================
         * 检查所有灵异液体。
         * ========================================================
         */

        for (GhostFluidConfig config : CONFIGS) {

            /*
             * 注意：
             *
             * 这里才调用 .get()。
             *
             * 不在静态初始化阶段调用。
             */
            if (!living.isInFluidType(
                    config.fluidType().get()
            )) {

                continue;
            }

            /*
             * ====================================================
             * 计算 immersion。
             *
             * 鬼湖水：
             *     使用深度。
             *
             * 鬼血：
             *     使用包裹程度。
             * ====================================================
             */

            double immersion =
                    config.useDepth()
                            ? calculateFluidDepth(
                            living.level(),
                            living,
                            config
                    )
                            : calculateEntityCoverage(
                            living.level(),
                            living,
                            config
                    );

            /*
             * 没有真正浸入：
             * 不处理这个液体。
             */
            if (immersion <= 0.0D) {
                continue;
            }

            /*
             * ====================================================
             * 每秒结算一次。
             * ====================================================
             */

            if (living.level().getGameTime()
                    % DAMAGE_INTERVAL != 0L) {

                continue;
            }

            /*
             * 灵异袭击。
             */
            applyGhostAttack(
                    living,
                    config,
                    immersion
            );

            /*
             * 玩家：
             *
             * 同时削减体内厉鬼复苏值。
             */
            if (living instanceof ServerPlayer player) {

                reducePlayerGhostRevival(
                        player,
                        config,
                        immersion
                );
            }

            /*
             * 一个实体同一时间只处理一种
             * 灵异液体。
             */
            return;
        }
    }


    /*
     * ============================================================
     * 液体深度
     * ============================================================
     */

    /**
     * 计算实体脚部到当前灵异液体液面的距离。
     *
     * 主要用于鬼湖水。
     */
    private static double calculateFluidDepth(
            Level level,
            LivingEntity entity,
            GhostFluidConfig config
    ) {

        double feetY =
                entity.getY();

        int blockX =
                Mth.floor(entity.getX());

        int blockZ =
                Mth.floor(entity.getZ());

        double surfaceY =
                Double.NaN;

        int startY =
                Mth.floor(feetY);

        int maxSearch =
                Math.min(
                        level.getMaxBuildHeight() - 1,
                        startY + 64
                );

        /*
         * 从实体脚下开始向上寻找
         * 连续的当前灵异液体。
         */
        for (
                int y = startY;
                y <= maxSearch;
                y++
        ) {

            BlockPos pos =
                    new BlockPos(
                            blockX,
                            y,
                            blockZ
                    );

            var fluidState =
                    level.getFluidState(pos);

            /*
             * 遇到空气：
             *
             * 液体结束。
             */
            if (fluidState.isEmpty()) {
                break;
            }

            /*
             * 不是当前灵异液体：
             *
             * 液体结束。
             */
            if (!fluidState.getFluidType().equals(
                    config.fluidType().get()
            )) {
                break;
            }

            /*
             * 当前方块顶部就是液面。
             */
            surfaceY =
                    y + fluidState.getHeight(
                            level,
                            pos
                    );
        }

        /*
         * 没找到液面。
         */
        if (Double.isNaN(surfaceY)) {
            return 0.0D;
        }

        /*
         * 液面高度 - 实体脚部高度。
         */
        return Math.max(
                0.0D,
                surfaceY - feetY
        );
    }


    /*
     * ============================================================
     * 液体包裹程度
     * ============================================================
     */

    /**
     * 计算实体被灵异液体包裹的程度。
     *
     * 返回：
     *
     *     0.0 ~ 1.0
     *
     * 例如：
     *
     *     0.0 = 完全没有
     *     0.5 = 大约一半身体被包裹
     *     1.0 = 完全包裹
     *
     * 主要用于鬼血。
     */
    private static double calculateEntityCoverage(
            Level level,
            LivingEntity entity,
            GhostFluidConfig config
    ) {

        var box =
                entity.getBoundingBox();

        double bottom =
                box.minY;

        double top =
                box.maxY;

        double height =
                top - bottom;

        /*
         * 防止极端情况下除以 0。
         */
        if (height <= 0.0D) {
            return 0.0D;
        }

        double highestFluid =
                bottom;

        int blockX =
                Mth.floor(entity.getX());

        int blockZ =
                Mth.floor(entity.getZ());

        /*
         * 从实体脚部扫描到头部。
         */
        for (
                int y = Mth.floor(bottom);
                y <= Mth.floor(top);
                y++
        ) {

            BlockPos pos =
                    new BlockPos(
                            blockX,
                            y,
                            blockZ
                    );

            var state =
                    level.getFluidState(pos);

            if (state.isEmpty()) {
                continue;
            }

            /*
             * 只计算当前配置中的液体。
             *
             * 普通水、熔岩、其他灵异液体
             * 都不会影响鬼血的包裹程度。
             */
            if (!state.getFluidType().equals(
                    config.fluidType().get()
            )) {
                continue;
            }

            highestFluid =
                    Math.max(
                            highestFluid,
                            y + state.getHeight(
                                    level,
                                    pos
                            )
                    );
        }

        /*
         * 液体高度 / 实体高度。
         */
        return Mth.clamp(
                (highestFluid - bottom)
                        / height,
                0.0D,
                1.0D
        );
    }


    /*
     * ============================================================
     * 灵异袭击
     * ============================================================
     */

    /**
     * 对实体施加灵异袭击。
     */
    private static void applyGhostAttack(
            LivingEntity entity,
            GhostFluidConfig config,
            double immersion
    ) {

        /*
         * 当前统一公式：
         *
         * immersion / 3 × 每三单位强度
         *
         * 对鬼湖水：
         *
         * 3 格 = 10
         *
         * 6 格 = 20
         *
         * 30 格 = 100（被上限限制）
         */
        double attackStrength =
                immersion
                        / 3.0D
                        * config.attackPerThreeUnits();

        /*
         * 最大值限制。
         */
        attackStrength =
                Math.min(
                        attackStrength,
                        config.maxAttack()
                );

        if (attackStrength <= 0.0D) {
            return;
        }

        /*
         * 使用该液体自己的 DamageSource。
         */
        SupernaturalDeathHandler.tryKill(
                entity,
                config.damageSourceFactory()
                        .apply(entity),
                attackStrength
        );
    }


    /*
     * ============================================================
     * 厉鬼复苏值
     * ============================================================
     */

    /**
     * 削减玩家体内所有厉鬼的复苏值。
     *
     * 鬼湖水：
     *
     *     每秒：
     *
     *     5 × 深度 %
     *
     * 鬼血以后：
     *
     *     改为：
     *
     *     复苏削减倍率 × 包裹程度
     */
    private static void reducePlayerGhostRevival(
            ServerPlayer player,
            GhostFluidConfig config,
            double immersion
    ) {

        Map<
                ResourceLocation,
                PossessedGhostState
                > oldData =
                player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                );

        /*
         * 玩家体内没有鬼。
         */
        if (oldData.isEmpty()) {
            return;
        }

        /*
         * 计算本次应该减少多少百分比。
         */
        double revivalLossPercent =
                config.revivalLossPerUnit()
                        * immersion;

        if (revivalLossPercent <= 0.0D) {
            return;
        }

        /*
         * 每一只鬼独立处理。
         */
        for (var entry :
                oldData.entrySet()) {

            ResourceLocation ghost =
                    entry.getKey();

            PossessedGhostState state =
                    entry.getValue();

            /*
             * revival 保存的是 0.0 ~ 1.0，
             * 所以百分比需要除以 100。
             */
            double newRevival =
                    Math.max(
                            0.0D,
                            state.revival()
                                    - revivalLossPercent
                                    / 100.0D
                    );

            /*
             * 没变化就不写 Attachment。
             */
            if (newRevival
                    == state.revival()) {

                continue;
            }

            PossessedGhostState newState =
                    new PossessedGhostState(
                            newRevival,
                            state.shallowStun(),
                            state.stunTicks(),
                            state.permanentStun(),
                            state.lastAbilityUseTick(),
                            state.intrinsicStrength()
                    );

            /*
             * 使用你现有的统一修改方法。
             */
            PossessionHandler.setState(
                    player,
                    ghost,
                    newState
            );
        }
    }
}