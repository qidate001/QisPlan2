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

@EventBusSubscriber(
        modid = QisPlan2.MODID
)
public final class GhostFluidHandler {

    private GhostFluidHandler() {
    }

    /**
     * 灵异攻击间隔：
     * 20 ticks = 1 秒
     */
    private static final long DAMAGE_INTERVAL = 20L;


    /*
     * ============================================================
     * 鬼湖水
     * ============================================================
     *
     * immersion = 深度（格）
     *
     * 原来的公式：
     *
     * depth / 3 * 10
     *
     * 等价于：
     *
     * depth * (10 / 3)
     */
    private static final GhostFluidConfig GHOST_LAKE =
            new GhostFluidConfig(
                    ModFluids.GHOST_LAKE_WATER_TYPE,

                    // 使用深度
                    true,

                    // 每 1 格深度 = 10/3 攻击强度
                    10.0D / 3.0D,

                    // 最大攻击强度
                    100.0D,

                    // 每 1 格深度 = 5% 复苏度削减
                    5.0D,

                    ModDamageTypes::ghostLakeWater
            );

    private static final GhostFluidConfig GHOST_BLOOD =
            new GhostFluidConfig(
                    ModFluids.GHOST_BLOOD_TYPE,

                    // 鬼血不看深度，看包裹程度
                    false,

                    // 100% 包裹时攻击 30
                    30.0D,

                    // 最大攻击 100
                    100.0D,

                    // 100% 包裹时，每秒压制 10% 复苏度
                    10.0D,

                    ModDamageTypes::ghostBlood
            );


    /*
     * ============================================================
     * 所有鬼液体
     * ============================================================
     */
    private static final GhostFluidConfig[] CONFIGS = {
            GHOST_LAKE,
            GHOST_BLOOD
    };


    @SubscribeEvent
    public static void onEntityTick(
            EntityTickEvent.Post event
    ) {
        Entity entity = event.getEntity();

        /*
         * 鬼液体逻辑全部只在服务端运行。
         */
        if (entity.level().isClientSide()) {
            return;
        }

        /*
         * 目前只处理生物。
         */
        if (!(entity instanceof LivingEntity living)
                || !living.isAlive()) {
            return;
        }


        /*
         * 检查所有鬼液体。
         */
        for (GhostFluidConfig config : CONFIGS) {

            /*
             * 注意：
             *
             * 这里才调用 DeferredHolder#get()
             *
             * 不要在静态初始化阶段调用。
             */
            if (!living.isInFluidType(
                    config.fluidType().get()
            )) {
                continue;
            }


            /*
             * 根据液体类型决定 immersion 的计算方式：
             *
             * 鬼湖水：
             *   immersion = 深度
             *
             * 鬼血：
             *   immersion = 包裹程度
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


            if (immersion <= 0.0D) {
                continue;
            }


            /*
             * 每秒处理一次灵异攻击。
             */
            if (living.level().getGameTime()
                    % DAMAGE_INTERVAL != 0L) {
                continue;
            }


            /*
             * 灵异攻击。
             */
            applyGhostAttack(
                    living,
                    config,
                    immersion
            );


            /*
             * 玩家额外处理：
             *
             * 液体会压制体内厉鬼的复苏。
             */
            if (living instanceof ServerPlayer player) {
                reducePlayerGhostRevival(
                        player,
                        config,
                        immersion
                );
            }


            /*
             * 一个实体同一 tick 只处理一种鬼液体。
             */
            return;
        }
    }


    /**
     * 计算鬼液体的深度。
     *
     * 返回值单位：
     * 格。
     *
     * 例如：
     *
     * 实体脚下到液面 3 格：
     * immersion = 3
     */
    private static double calculateFluidDepth(
            Level level,
            LivingEntity entity,
            GhostFluidConfig config
    ) {
        double feetY = entity.getY();

        int blockX = Mth.floor(entity.getX());
        int blockZ = Mth.floor(entity.getZ());

        double surfaceY = Double.NaN;


        int startY = Mth.floor(feetY);

        int maxSearch = Math.min(
                level.getMaxBuildHeight() - 1,
                startY + 64
        );


        for (int y = startY; y <= maxSearch; y++) {

            BlockPos pos =
                    new BlockPos(
                            blockX,
                            y,
                            blockZ
                    );

            var fluidState =
                    level.getFluidState(pos);


            /*
             * 中间断液体，说明液面已经结束。
             */
            if (fluidState.isEmpty()) {
                break;
            }


            /*
             * 不是当前鬼液体，也结束搜索。
             */
            if (!fluidState.getFluidType().equals(
                    config.fluidType().get()
            )) {
                break;
            }


            /*
             * 记录液面高度。
             */
            surfaceY =
                    y + fluidState.getHeight(
                            level,
                            pos
                    );
        }


        if (Double.isNaN(surfaceY)) {
            return 0.0D;
        }


        return Math.max(
                0.0D,
                surfaceY - feetY
        );
    }


    /**
     * 计算实体被鬼液体包裹的程度。
     *
     * 返回值：
     *
     * 0.0 = 完全没有包裹
     * 1.0 = 整个实体高度都被包裹
     *
     * 这个模式主要给鬼血使用。
     */
    private static double calculateEntityCoverage(
            Level level,
            LivingEntity entity,
            GhostFluidConfig config
    ) {
        var box = entity.getBoundingBox();

        double bottom = box.minY;
        double top = box.maxY;

        double height = top - bottom;


        if (height <= 0.0D) {
            return 0.0D;
        }


        /*
         * 默认液面在实体脚部。
         */
        double highestFluid = bottom;


        /*
         * 当前版本使用实体中心所在的流体柱。
         */
        int blockX = Mth.floor(entity.getX());
        int blockZ = Mth.floor(entity.getZ());


        for (int y = Mth.floor(bottom);
             y <= Mth.floor(top);
             y++) {

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
             * 只计算当前配置的鬼液体。
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
         * 转换成 0~1 的包裹比例。
         */
        return Mth.clamp(
                (highestFluid - bottom)
                        / height,
                0.0D,
                1.0D
        );
    }


    /**
     * 对实体发动鬼液体的灵异攻击。
     *
     * 统一公式：
     *
     * attack =
     *      immersion
     *      × attackPerImmersion
     *
     * 然后限制最大值。
     */
    private static void applyGhostAttack(
            LivingEntity entity,
            GhostFluidConfig config,
            double immersion
    ) {
        double attackStrength =
                immersion
                        * config.attackPerImmersion();


        /*
         * 限制最大攻击强度。
         */
        attackStrength =
                Math.min(
                        attackStrength,
                        config.maxAttack()
                );


        if (attackStrength <= 0.0D) {
            return;
        }


        SupernaturalDeathHandler.tryKill(
                entity,
                config.damageSourceFactory()
                        .apply(entity),
                attackStrength
        );
    }


    /**
     * 压制玩家体内厉鬼的复苏。
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


        if (oldData.isEmpty()) {
            return;
        }


        /*
         * immersion × 每单位压制百分比
         */
        double revivalLossPercent =
                config.revivalLossPerUnit()
                        * immersion;


        if (revivalLossPercent <= 0.0D) {
            return;
        }


        /*
         * 遍历玩家体内所有厉鬼。
         */
        for (var entry : oldData.entrySet()) {

            ResourceLocation ghost =
                    entry.getKey();

            PossessedGhostState state =
                    entry.getValue();


            /*
             * revival() 本身是 0~1。
             *
             * 例如：
             *
             * revival = 0.80
             * revivalLossPercent = 5
             *
             * 新复苏度：
             *
             * 0.80 - 0.05
             */
            double newRevival =
                    Math.max(
                            0.0D,
                            state.revival()
                                    - revivalLossPercent
                                    / 100.0D
                    );


            if (newRevival == state.revival()) {
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


            PossessionHandler.setState(
                    player,
                    ghost,
                    newState
            );
        }
    }
}