#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D MainDepthSampler;

uniform mat4 GhostEyeProjMat;
uniform mat4 GhostEyeModelViewMat;

uniform vec3 GhostEyeCameraPos;
uniform vec3 GhostEyeDomainCenter;

uniform float GhostEyeDomainRadius;
uniform float GhostEyeDomainActive;
uniform float GhostEyeDomainLayer;

in vec2 texCoord;

out vec4 fragColor;


void main() {

    // =========================================================
    // 1. 读取场景
    // =========================================================

    vec4 scene =
    texture(
            DiffuseSampler,
            texCoord
    );

    float depth =
    texture(
            MainDepthSampler,
            texCoord
    ).r;


    // =========================================================
    // 2. 屏幕坐标 → NDC
    // =========================================================

    vec2 ndcXY =
    texCoord * 2.0 - 1.0;


    // =========================================================
    // 3. 构造近裁剪面 / 远裁剪面的 View Space 坐标
    // =========================================================

    vec4 nearNDC =
    vec4(
            ndcXY,
            -1.0,
            1.0
    );

    vec4 farNDC =
    vec4(
            ndcXY,
            1.0,
            1.0
    );


    vec4 nearView =
    inverse(GhostEyeProjMat)
    * nearNDC;

    vec4 farView =
    inverse(GhostEyeProjMat)
    * farNDC;

    nearView /=
    nearView.w;

    farView /=
    farView.w;


    // =========================================================
    // 4. View Space → World Space
    // =========================================================

    vec4 nearWorld =
    inverse(GhostEyeModelViewMat)
    * nearView;

    vec4 farWorld =
    inverse(GhostEyeModelViewMat)
    * farView;

    nearWorld /=
    nearWorld.w;

    farWorld /=
    farWorld.w;


    vec3 rayStart =
    nearWorld.xyz
    + GhostEyeCameraPos;

    vec3 rayEnd =
    farWorld.xyz
    + GhostEyeCameraPos;


    // =========================================================
    // 5. 构造世界空间射线
    // =========================================================

    vec3 rayDirection =
    normalize(
            rayEnd - rayStart
    );


    // =========================================================
    // 6. 摄像机到射线起点的偏移
    // =========================================================

    vec3 sphereOffset =
    rayStart
    - GhostEyeDomainCenter;


    // =========================================================
    // 7. 射线与鬼域球体求交
    // =========================================================

    float b =
    dot(
            sphereOffset,
            rayDirection
    );

    float c =
    dot(
            sphereOffset,
            sphereOffset
    )
    -
    GhostEyeDomainRadius
    *
    GhostEyeDomainRadius;

    float discriminant =
    b * b - c;


    // =========================================================
    // 8. 默认：当前像素不在鬼域空气中
    // =========================================================

    float inside =
    0.0;


    if (
        discriminant >= 0.0
        &&
        GhostEyeDomainActive > 0.0
    ) {

        float sqrtD =
        sqrt(discriminant);

        float tEnter =
        -b - sqrtD;

        float tExit =
        -b + sqrtD;


        // =====================================================
        // 9. 判断射线是否真的穿过球体
        // =====================================================

        if (tExit > 0.0) {

            // -------------------------------------------------
            // 如果摄像机在球体内部，
            // 那么射线已经从鬼域内部开始。
            // -------------------------------------------------

            tEnter =
            max(
                    tEnter,
                    0.0
            );


            // -------------------------------------------------
            // 射线在球体中的长度
            // -------------------------------------------------

            float domainLength =
            max(
                    tExit - tEnter,
                    0.0
            );


            // -------------------------------------------------
            // 将穿过长度转换成 0~1 的浓度
            // -------------------------------------------------

            inside =
            clamp(
                    domainLength
                    /
                    (
                    GhostEyeDomainRadius
                    * 2.0
                    ),
                    0.0,
                    1.0
            );
        }
    }


    // =========================================================
    // 10. 根据鬼域层数调整颜色强度
    // =========================================================

    float redStrength =
    0.15
    +
    (
    GhostEyeDomainLayer - 1.0
    )
    * 0.10;

    redStrength =
    clamp(
            redStrength,
            0.15,
            0.65
    );


    // =========================================================
    // 11. 鬼域空气颜色
    // =========================================================

    vec3 red =
    vec3(
            1.0,
            0.0,
            0.0
    );

    vec3 ghostColor =
    mix(
            scene.rgb,
            red,
            redStrength
    );


    // =========================================================
    // 12. 根据空气厚度进行混合
    // =========================================================

    vec3 finalColor =
    mix(
            scene.rgb,
            ghostColor,
            inside
    );


    // =========================================================
    // 13. 输出
    // =========================================================

    fragColor =
    vec4(
            finalColor,
            scene.a
    );
}