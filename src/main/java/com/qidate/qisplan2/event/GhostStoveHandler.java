package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = QisPlan2.MODID)
public class GhostStoveHandler {

    /**
     * 鬼灶台检测范围。
     * 即以实体所在方块为中心，
     * X/Y/Z 各向外检测 5 格。
     */
    private static final int RANGE = 5;


    /**
     * 每 tick 检查一次实体。
     */
    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {

        /*
         * =========================
         * 只处理生物
         * =========================
         */

        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }


        /*
         * =========================
         * 只在服务端处理
         * =========================
         */

        if (entity.level().isClientSide()) {
            return;
        }


        /*
         * =========================
         * 死亡实体不处理
         * =========================
         */

        if (!entity.isAlive()) {
            return;
        }


        /*
         * =========================
         * 检查附近是否存在鬼灶台
         * =========================
         */

        if (!isNearGhostStove(entity)) {
            return;
        }


        /*
         * =========================
         * 检查实体是否携带食物
         * =========================
         */

        if (!hasFood(entity)) {
            return;
        }


        /*
         * =========================
         * 触发鬼灶台诅咒
         * =========================
         */

        applyGhostStoveCurse(entity);
    }


    /**
     * 检查实体附近 5×5×5 范围内
     * 是否存在鬼灶台。
     */
    private static boolean isNearGhostStove(
            LivingEntity entity
    ) {

        BlockPos entityPos =
                entity.blockPosition();

        BlockPos.MutableBlockPos mutablePos =
                new BlockPos.MutableBlockPos();


        for (int x = -RANGE; x <= RANGE; x++) {

            for (int y = -RANGE; y <= RANGE; y++) {

                for (int z = -RANGE; z <= RANGE; z++) {

                    mutablePos.set(
                            entityPos.getX() + x,
                            entityPos.getY() + y,
                            entityPos.getZ() + z
                    );


                    BlockState state =
                            entity.level().getBlockState(
                                    mutablePos
                            );


                    if (state.is(
                            QisPlan2.GHOST_STOVE.get()
                    )) {

                        return true;
                    }
                }
            }
        }

        return false;
    }


    /**
     * 检查实体身上是否携带食物。
     *
     * 玩家：
     * - 主手
     * - 副手
     * - 背包
     *
     * 普通生物：
     * - 主手
     * - 副手
     */
    private static boolean hasFood(
            LivingEntity entity
    ) {

        /*
         * =========================
         * 主手
         * =========================
         */

        if (isFood(
                entity.getMainHandItem(),
                entity
        )) {

            return true;
        }


        /*
         * =========================
         * 副手
         * =========================
         */

        if (isFood(
                entity.getOffhandItem(),
                entity
        )) {

            return true;
        }


        /*
         * =========================
         * 玩家背包
         * =========================
         */

        if (entity instanceof Player player) {

            for (ItemStack stack :
                    player.getInventory().items) {

                if (isFood(stack, entity)) {
                    return true;
                }
            }
        }


        return false;
    }


    /**
     * 判断一个 ItemStack 是否为食物。
     *
     * Minecraft 1.21.1：
     * FoodProperties 不为空
     * 即代表这个物品属于食物。
     */
    private static boolean isFood(
            ItemStack stack,
            LivingEntity entity
    ) {

        return !stack.isEmpty()
                && stack.getFoodProperties(entity) != null;
    }


    /**
     * 鬼灶台诅咒。
     */
    private static void applyGhostStoveCurse(
            LivingEntity entity
    ) {

        /*
         * ========================================
         * 灵魂粒子
         * ========================================
         */

        if (entity.level()
                instanceof ServerLevel serverLevel) {

            serverLevel.sendParticles(
                    ParticleTypes.SOUL,
                    entity.getX(),
                    entity.getY() + 0.5,
                    entity.getZ(),
                    20,
                    0.4,
                    0.5,
                    0.4,
                    0.05
            );


            /*
             * ====================================
             * 音效
             * ====================================
             */

            serverLevel.playSound(
                    null,
                    entity.blockPosition(),
                    SoundEvents.SOUL_ESCAPE.value(),
                    SoundSource.BLOCKS,
                    1.0F,
                    0.8F
            );
        }


        /*
         * ========================================
         * 必死诅咒
         * ========================================
         */

        SupernaturalDeathHandler.tryKill(
                entity,
                ModDamageTypes.ghostStove(entity)
        );
    }
}