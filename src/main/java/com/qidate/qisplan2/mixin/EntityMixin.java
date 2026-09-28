package com.qidate.qisplan2.mixin;

import com.qidate.qisplan2.ghost.curse.Curse;
import com.qidate.qisplan2.ghost.curse.CurseManager;
import com.qidate.qisplan2.ghost.curse.type.ghosttombstone.GhostTombstoneCurse;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Entity Mixin。
 *
 * 用于处理鬼墓碑诅咒强制将玩家拉入土中的特殊情况。
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    /**
     * 阻止正在被鬼墓碑诅咒拉入地下的玩家
     * 被 Minecraft 原版机制推出方块。
     */
    @Inject(
            method = "pushOutOfBlocks",
            at = @At("HEAD"),
            cancellable = true
    )
    private void qisplan2$preventPushOutOfBlocks(
            double x,
            double y,
            double z,
            CallbackInfo ci
    ) {

        Entity entity =
                (Entity) (Object) this;

        /*
         * 只处理玩家。
         *
         * 其他实体完全保持原版行为。
         */
        if (!(entity instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
        }

        /*
         * 查询这个玩家当前的诅咒。
         */
        for (Curse curse :
                CurseManager.getByTarget(
                        player.getUUID()
                )) {

            /*
             * 只有鬼墓碑诅咒正在下沉时，
             * 才禁止原版把玩家推出方块。
             */
            if (curse instanceof GhostTombstoneCurse ghostTombstoneCurse
                    && ghostTombstoneCurse.isSinking()) {

                ci.cancel();

                return;
            }
        }
    }
}