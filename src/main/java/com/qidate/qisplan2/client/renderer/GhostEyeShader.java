package com.qidate.qisplan2.client.renderer;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.client.renderer.ShaderInstance;

public final class GhostEyeShader {

    private static ShaderInstance INSTANCE;

    private GhostEyeShader() {
    }

    public static void setInstance(ShaderInstance shader) {
        INSTANCE = shader;

        QisPlan2.LOGGER.info(
                "[鬼眼渲染] Shader 注册成功: {}",
                shader.getName()
        );
    }

    public static ShaderInstance getInstance() {
        return INSTANCE;
    }

    public static boolean isReady() {
        return INSTANCE != null;
    }
}