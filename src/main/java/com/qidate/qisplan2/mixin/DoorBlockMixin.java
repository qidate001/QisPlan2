package com.qidate.qisplan2.mixin;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.event.DoorGhostTriggerHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DoorBlock.class)
public abstract class DoorBlockMixin {

    /*
     * ============================================================
     * 玩家右键门
     * ============================================================
     */
    @Inject(
            method = "useWithoutItem",
            at = @At("HEAD")
    )
    private void qisplan2$onPlayerUse(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult,
            CallbackInfoReturnable<?> cir
    ) {

        /*
         * 客户端只负责预测。
         */
        if (level.isClientSide()) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        /*
         * 当前是：
         *
         * OPEN = false
         * ↓
         * 这次就是开门
         *
         * OPEN = true
         * ↓
         * 这次就是关门
         */
        boolean opening =
                !state.getValue(
                        DoorBlock.OPEN
                );

//        QisPlan2.LOGGER.info(
//                "[QisPlan2] 玩家 {} {} 门：{}",
//                player.getName().getString(),
//                opening
//                        ? "打开"
//                        : "关闭",
//                pos
//        );

        DoorGhostTriggerHandler.onDoorChanged(
                serverLevel,
                pos,
                state,
                player,
                opening
        );
    }


    /*
     * ============================================================
     * AI / 其他生物开关门
     * ============================================================
     *
     * 玩家不在这里处理。
     *
     * 因为玩家已经由 useWithoutItem() 处理，
     * 否则会重复触发。
     */
    @Inject(
            method = "setOpen",
            at = @At("HEAD")
    )
    private void qisplan2$onSetOpen(
            Entity entity,
            Level level,
            BlockState state,
            BlockPos pos,
            boolean open,
            CallbackInfo ci
    ) {

        /*
         * 客户端不处理。
         */
        if (level.isClientSide()) {
            return;
        }

        /*
         * 玩家已经在 useWithoutItem() 处理。
         */
        if (entity instanceof Player) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        /*
         * 必须是生物。
         *
         * 红石等：
         * entity == null
         *
         * 不处理。
         */
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

//        QisPlan2.LOGGER.info(
//                "[QisPlan2] 生物 {} {} 门：{}",
//                living.getName().getString(),
//                open
//                        ? "打开"
//                        : "关闭",
//                pos
//        );

        DoorGhostTriggerHandler.onDoorChanged(
                serverLevel,
                pos,
                state,
                living,
                open
        );
    }
}