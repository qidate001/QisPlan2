package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.fluid.ModFluids;
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
public final class GhostLakeHandler {

    private GhostLakeHandler() {
    }

    /**
     * 每秒结算一次。
     */
    private static final long DAMAGE_INTERVAL =
            20L;

    /**
     * 每秒每单位沉入深度的灵异袭击强度。
     */
    private static final double ENTITY_DAMAGE_PER_DEPTH =
            10.0D;

    /**
     * 每 3 格沉入深度增加 10 点灵异袭击强度。
     */
    private static final double ENTITY_DAMAGE_PER_3_DEPTH =
            10.0D;

    /**
     * 普通鬼湖水最大灵异袭击强度：
     *
     * 100 / 秒。
     */
    private static final double MAX_ENTITY_ATTACK_STRENGTH =
            100.0D;

    /**
     * 玩家体内每只鬼：
     *
     * 每秒减少：
     *
     * 5 × 沉下深度
     *
     * 点复苏值。
     *
     * 注意：
     *
     * 这里的“5”是百分比。
     */
    private static final double GHOST_REVIVAL_LOSS_PER_DEPTH =
            5.0D;


    /**
     * 鬼湖水下沉速度。
     *
     * 每 tick 向下推。
     */
    private static final double SINK_SPEED =
            0.35D;


    @SubscribeEvent
    public static void onEntityTick(
            EntityTickEvent.Post event
    ) {

        Entity entity =
                event.getEntity();

        /*
         * ========================================================
         * 只处理服务端。
         * ========================================================
         */

        if (!(entity.level()
                instanceof Level level)
                || level.isClientSide()) {

            return;
        }

        /*
         * ========================================================
         * 只处理活着的 LivingEntity。
         * ========================================================
         */

        if (!(entity instanceof LivingEntity living)
                || !living.isAlive()) {

            return;
        }

        /*
         * 不处理观察者。
         *
         * Spectator 本身不会正常参与实体交互。
         */
        if (living.isSpectator()) {
            return;
        }

        /*
         * ========================================================
         * 鬼湖水 FluidType
         * ========================================================
         */

        var fluidType =
                ModFluids.GHOST_LAKE_WATER_TYPE.get();

        /*
         * ========================================================
         * 判断是否接触鬼湖水。
         * ========================================================
         */

        if (!living.isInFluidType(
                fluidType
        )) {

            return;
        }

        /*
         * ========================================================
         * 获取沉下深度。
         *
         * NeoForge：
         *
         * getFluidTypeHeight()
         *
         * 返回该流体在实体包围盒中的实际高度。
         * ========================================================
         */

        double depth =
                calculateGhostLakeDepth(
                        level,
                        living
                );

        if (depth <= 0.0D) {
            return;
        }

        /*
         * ========================================================
         * 极快速下沉。
         *
         * X / Z 直接清零：
         *
         * 不允许水平移动。
         *
         * Y：
         *
         * 向下快速沉。
         * ========================================================
         */

        sinkEntity(
                living
        );

        /*
         * ========================================================
         * 每秒结算一次灵异效果。
         * ========================================================
         */

        long gameTime =
                level.getGameTime();

        if (gameTime % DAMAGE_INTERVAL != 0L) {
            return;
        }

        /*
         * ========================================================
         * 普通灵异袭击。
         * ========================================================
         */

        applyGhostLakeAttack(
                living,
                depth
        );

        /*
         * ========================================================
         * 玩家体内厉鬼复苏值下降。
         * ========================================================
         */

        if (living instanceof ServerPlayer player) {

            reducePlayerGhostRevival(
                    player,
                    depth
            );
        }
    }

    private static double calculateGhostLakeDepth(
            Level level,
            LivingEntity entity
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

        for (int y = startY;
             y <= maxSearch;
             y++) {

            BlockPos pos =
                    new BlockPos(
                            blockX,
                            y,
                            blockZ
                    );

            var fluidState =
                    level.getFluidState(
                            pos
                    );

            if (fluidState.isEmpty()) {
                break;
            }

            if (!fluidState.is(
                    ModFluids.GHOST_LAKE_WATER.get()
            )) {
                break;
            }

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
     * 强制让实体快速向下沉。
     */
    private static void sinkEntity(
            LivingEntity entity
    ) {

        /*
         * 不允许水平移动。
         */
        entity.setDeltaMovement(
                0.0D,
                Math.min(
                        entity.getDeltaMovement().y,
                        -SINK_SPEED
                ),
                0.0D
        );

        entity.hasImpulse = true;

        /*
         * 防止游泳姿态参与正常水上移动。
         */
        entity.setSwimming(
                false
        );

        /*
         * 飞行中的玩家：
         *
         * 立即停止飞行。
         */
        if (entity instanceof ServerPlayer player) {

            if (player.getAbilities().flying) {

                player.getAbilities().flying =
                        false;

                player.onUpdateAbilities();
            }
        }
    }


    /**
     * 鬼湖水对实体本身的灵异袭击。
     *
     * 强度：
     *
     * 10 × 深度
     */
    private static void applyGhostLakeAttack(
            LivingEntity entity,
            double depth
    ) {

        /*
         * 每 3 格深度：
         *
         * +10 强度
         *
         * 即：
         *
         * depth / 3 × 10
         */
        double attackStrength =
                depth
                        / 3.0D
                        * ENTITY_DAMAGE_PER_3_DEPTH;

        /*
         * 最大 100。
         */
        attackStrength =
                Math.min(
                        attackStrength,
                        MAX_ENTITY_ATTACK_STRENGTH
                );

        if (attackStrength <= 0.0D) {
            return;
        }

        SupernaturalDeathHandler.tryKill(
                entity,
                ModDamageTypes.ghostLakeWater(
                        entity
                ),
                attackStrength
        );
    }


    /**
     * 鬼湖水削减玩家体内所有厉鬼的复苏值。
     *
     * 每秒：
     *
     * 5 × 深度
     *
     * 百分比。
     */
    private static void reducePlayerGhostRevival(
            ServerPlayer player,
            double depth
    ) {

        Map<
                ResourceLocation,
                PossessedGhostState
                > oldData =
                player.getData(
                        QisPlan2.POSSESSED_GHOSTS
                );

        if (oldData.isEmpty()) {
            return;
        }

        double revivalLossPercent =
                GHOST_REVIVAL_LOSS_PER_DEPTH
                        * depth;

        if (revivalLossPercent <= 0.0D) {
            return;
        }

        for (var entry :
                oldData.entrySet()) {

            ResourceLocation ghost =
                    entry.getKey();

            PossessedGhostState state =
                    entry.getValue();

            /*
             * ====================================================
             * 每只鬼都独立减少复苏。
             * ====================================================
             */

            double newRevival =
                    Math.max(
                            0.0D,
                            state.revival()
                                    - revivalLossPercent
                                    / 100.0D
                    );

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

            PossessionHandler.setState(
                    player,
                    ghost,
                    newState
            );
        }
    }
}