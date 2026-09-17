#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D MainDepthSampler;

uniform mat4 GhostUmbrellaProjMat;
uniform mat4 GhostUmbrellaModelViewMat;

uniform vec3 GhostUmbrellaCameraPos;
uniform vec3 GhostUmbrellaDomainCenter;

uniform float GhostUmbrellaDomainRadius;
uniform float GhostUmbrellaDomainActive;
uniform float GhostUmbrellaDomainLayer;

in vec2 texCoord;

out vec4 fragColor;


void main() {

    // =========================================================
    // 读取场景
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
    // 屏幕坐标 → NDC
    // =========================================================

    vec2 ndcXY = texCoord * 2.0 - 1.0;


    // =========================================================
    // 构造近裁剪面 / 远裁剪面的 View Space 坐标
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
        inverse(GhostUmbrellaProjMat)
        * nearNDC;

    vec4 farView =
        inverse(GhostUmbrellaProjMat)
        * farNDC;

    nearView /= nearView.w;
    farView /= farView.w;


    // =========================================================
    // View Space → World Space
    // =========================================================

    vec4 nearWorld =
        inverse(GhostUmbrellaModelViewMat)
        * nearView;

    vec4 farWorld =
        inverse(GhostUmbrellaModelViewMat)
        * farView;

    nearWorld /= nearWorld.w;
    farWorld /= farWorld.w;


    vec3 rayStart =
        nearWorld.xyz
        + GhostUmbrellaCameraPos;

    vec3 rayEnd =
        farWorld.xyz
        + GhostUmbrellaCameraPos;


    // =========================================================
    // 当前像素对应的场景世界坐标
    // =========================================================

    vec4 sceneNDC =
        vec4(
            ndcXY,
            depth * 2.0 - 1.0,
            1.0
        );

    vec4 sceneView =
        inverse(GhostUmbrellaProjMat)
        * sceneNDC;

    sceneView /= sceneView.w;

    vec4 sceneWorld =
        inverse(GhostUmbrellaModelViewMat)
        * sceneView;

    sceneWorld /= sceneWorld.w;

    vec3 sceneWorldPos =
        sceneWorld.xyz
        + GhostUmbrellaCameraPos;


    // =========================================================
    // 构造世界空间射线
    // =========================================================

    vec3 rayDirection =
        normalize(
            rayEnd - rayStart
        );


    // =========================================================
    // 计算场景表面位于射线上的距离
    // =========================================================

    float sceneDistance =
        dot(
            sceneWorldPos - rayStart,
            rayDirection
        );


    // =========================================================
    // 射线起点相对于圆柱中心
    //
    // 圆柱无限延伸，因此只考虑 X / Z。
    // =========================================================

    vec3 cylinderOffset =
        rayStart
        - GhostUmbrellaDomainCenter;


    // =========================================================
    // 射线与无限圆柱求交
    //
    // x² + z² = r²
    // =========================================================

    vec2 rayXZ =
        rayDirection.xz;

    vec2 offsetXZ =
        cylinderOffset.xz;


    float a =
        dot(
            rayXZ,
            rayXZ
        );

    float b =
        2.0 *
        dot(
            offsetXZ,
            rayXZ
        );

    float c =
        dot(
            offsetXZ,
            offsetXZ
        )
        -
        GhostUmbrellaDomainRadius
        *
        GhostUmbrellaDomainRadius;


    float discriminant =
        b * b
        -
        4.0 * a * c;


    // =========================================================
    // 默认：当前像素不在鬼域空气中
    // =========================================================

    float inside = 0.0;


    if (
        discriminant >= 0.0
        &&
        a > 0.000001
        &&
        GhostUmbrellaDomainActive > 0.0
    ) {

        float sqrtD =
        sqrt(discriminant);


        float tEnter =
            ( -b - sqrtD ) / ( 2.0 * a );


        float tExit =
            ( -b + sqrtD ) / ( 2.0 * a );


        // =====================================================
        // 射线确实穿过圆柱
        // =====================================================

        if (tExit > 0.0) {

            tEnter =
                max(
                    tEnter,
                    0.0
                );


            // =================================================
            // 场景物体会截断鬼域
            // =================================================

            float visibleExit =
                min(
                    tExit,
                    sceneDistance
                );


            // =================================================
            // 只有鬼域在场景物体前面才可见
            // =================================================

            if (visibleExit > tEnter) {

                float domainLength =
                    visibleExit
                    - tEnter;


                // =================================================
                // 圆柱内部长度
                // =================================================

                inside =
                    clamp(
                        domainLength / ( GhostUmbrellaDomainRadius * 2.0 ),
                        0.0,
                        1.0
                    );
            }
        }
    }


    // =========================================================
    // 根据鬼域层数调整颜色强度
    // =========================================================

    float umbrellaStrength =
        0.45
        + ( GhostUmbrellaDomainLayer - 1.0 )
        * 0.10;

    umbrellaStrength =
        clamp(
            umbrellaStrength,
            0.30,
            0.85
        );


    // =========================================================
    // 鬼伞鬼域颜色
    //
    // 第一阶段先使用明显颜色，
    // 仅用于验证圆柱鬼域边界。
    // =========================================================

    vec3 umbrellaColor =
        vec3(
            0.0,
            0.0,
            0.0
        );


    vec3 ghostColor =
        mix(
            scene.rgb,
            umbrellaColor,
            umbrellaStrength
        );


    // =========================================================
    // 根据鬼域空气厚度进行混合
    // =========================================================

    float ghostStrength =
        clamp(
            inside * 1.6,
            0.0,
            1.0
        );


    vec3 finalColor =
        mix(
            scene.rgb,
            ghostColor,
            ghostStrength
        );


    // =========================================================
    // 输出
    // =========================================================

    fragColor =
        vec4(
            finalColor,
            scene.a
        );
}