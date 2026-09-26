package com.qidate.qisplan2.mixin;

import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomainVisionSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 将鬼域视觉描边接入 Minecraft 原版实体发光系统。
 *
 * <p>
 * 本 Mixin 不负责实际绘制实体轮廓。
 * 仅扩展 Minecraft 对“实体是否应该发光”的判断。
 *
 * <p>
 * 实际轮廓绘制仍由 Minecraft 原版
 * OutlineBufferSource 完成。
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    /*
     * ============================================================
     * 鬼域视觉描边
     * ============================================================
     *
     * Minecraft 原版在这里判断实体是否应该发光。
     *
     * ClientGhostDomainVisionSystem
     * ↓
     * 当前实体属于鬼域视觉名单
     * ↓
     * 返回 true
     *
     * 后续继续使用 Minecraft 原版的
     * OutlineBufferSource 进行描边。
     */
    @Inject(
            method = "shouldEntityAppearGlowing",
            at = @At("RETURN"),
            cancellable = true
    )
    private void qisplan2$ghostDomainVision(
            Entity entity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (ClientGhostDomainVisionSystem.shouldOutline(
                entity.getUUID()
        )) {
            cir.setReturnValue(true);
        }
    }
}