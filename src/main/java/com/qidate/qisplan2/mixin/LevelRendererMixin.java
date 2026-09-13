package com.qidate.qisplan2.mixin;

import com.qidate.qisplan2.client.renderer.GhostEyeRenderHandler;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    /**
     * 世界渲染完成后进行鬼眼后处理。
     *
     * 当前阶段仅用于测试：
     * 绘制一个覆盖整个屏幕的纯红色 Shader。
     */
    @Inject(
            method = "renderLevel",
            at = @At("TAIL")
    )
    private void qisplan2$renderGhostEye(
            CallbackInfo ci
    ) {
        GhostEyeRenderHandler.render();
    }
}