#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D MainDepthSampler;

uniform mat4 ProjMat;

in vec2 texCoord;

out vec4 fragColor;

void main() {

    float depth = texture(MainDepthSampler, texCoord).r;

    // 屏幕坐标
    vec2 ndcXY = texCoord * 2.0 - 1.0;

    // OpenGL 深度范围：0 ~ 1
    // 转换成 NDC：-1 ~ 1
    float ndcZ = depth * 2.0 - 1.0;

    vec4 ndcPosition = vec4(
            ndcXY,
            ndcZ,
            1.0
    );

    // Projection Matrix 的逆矩阵
    vec4 viewPosition =
    inverse(ProjMat) * ndcPosition;

    // 齐次除法
    viewPosition /= viewPosition.w;

    // 摄像机空间中的距离
    float linearDepth = -viewPosition.z;

    // 暂时只是为了显示效果
    float value = 1.0 - linearDepth / 32.0;

    value = clamp(
            value,
            0.0,
            1.0
    );

    fragColor = vec4(
            value,
            value,
            value,
            1.0
    );
}