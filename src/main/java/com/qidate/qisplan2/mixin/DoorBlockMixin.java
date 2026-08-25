package com.qidate.qisplan2.mixin;

import com.qidate.qisplan2.event.DoorGhostTriggerHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DoorBlock.class)
public abstract class DoorBlockMixin {

    @Inject(
            method = "setOpen",
            at = @At("RETURN")
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
         * 必须是生物触发。
         *
         * 红石等导致的 setOpen：
         * entity == null
         *
         * 直接忽略。
         */
        if (!(entity
                instanceof net.minecraft.world.entity.LivingEntity living)) {
            return;
        }

        /*
         * 如果本来就是目标状态，
         * 不算真正发生了一次开/关。
         */
        if (!state.hasProperty(
                DoorBlock.OPEN
        )) {
            return;
        }

        if (state.getValue(
                DoorBlock.OPEN
        ) == open) {
            return;
        }

        if (!(level
                instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        DoorGhostTriggerHandler.onDoorChanged(
                serverLevel,
                pos,
                state,
                living,
                open
        );
    }
}