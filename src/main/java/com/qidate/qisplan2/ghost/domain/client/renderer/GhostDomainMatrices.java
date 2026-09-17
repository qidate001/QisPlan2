package com.qidate.qisplan2.ghost.domain.client.renderer;

import org.joml.Matrix4f;

public final class GhostDomainMatrices {

    private static final Matrix4f MODEL_VIEW_MATRIX =
            new Matrix4f();

    private static final Matrix4f PROJECTION_MATRIX =
            new Matrix4f();

    private GhostDomainMatrices() {
    }

    /**
     * 保存当前世界渲染帧的 ModelView 矩阵。
     */
    public static void captureModelViewMatrix(
            Matrix4f matrix
    ) {
        MODEL_VIEW_MATRIX.set(matrix);
    }

    /**
     * 获取当前帧保存的 ModelView 矩阵。
     */
    public static Matrix4f getModelViewMatrix() {
        return MODEL_VIEW_MATRIX;
    }

    /**
     * 保存当前世界渲染帧的 Projection 矩阵。
     */
    public static void captureProjectionMatrix(
            Matrix4f matrix
    ) {
        PROJECTION_MATRIX.set(matrix);
    }

    /**
     * 获取当前帧保存的 Projection 矩阵。
     */
    public static Matrix4f getProjectionMatrix() {
        return PROJECTION_MATRIX;
    }
}