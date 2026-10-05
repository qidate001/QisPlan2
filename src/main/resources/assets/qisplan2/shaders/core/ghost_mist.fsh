#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D MainDepthSampler;
uniform sampler2D GhostMistRegionIdentity;

uniform mat4 GhostMistProjMat;
uniform mat4 GhostMistModelViewMat;

uniform vec3 GhostMistCameraPos;
uniform vec3 GhostMistDomainCenter;

uniform float GhostMistDomainRadius;
uniform float GhostMistDomainActive;
uniform float GhostMistDomainLayer;
uniform float GhostMistSourceRegion;
uniform float GhostMistTime;

in vec2 texCoord;

out vec4 fragColor;


/*
 * =========================================================
 * Hash
 * =========================================================
 *
 * 简单程序随机函数。
 *
 * 不需要额外 Noise 贴图。
 */
float hash(vec3 p) {

    return fract(
            sin(
                    dot(
                            p,
                            vec3(
                                    127.1,
                                    311.7,
                                    74.7
                            )
                    )
            )
            * 43758.5453123
    );
}


/*
 * =========================================================
 * Value Noise
 * =========================================================
 */
float noise(vec3 p) {

    vec3 i = floor(p);

    vec3 f = fract(p);

    f = f * f
        * (
            3.0
            - 2.0 * f
        );

    float n000 =
        hash(i + vec3(0.0, 0.0, 0.0));

    float n100 =
        hash(i + vec3(1.0, 0.0, 0.0));

    float n010 =
        hash(i + vec3(0.0, 1.0, 0.0));

    float n110 =
        hash(i + vec3(1.0, 1.0, 0.0));

    float n001 =
        hash(i + vec3(0.0, 0.0, 1.0));

    float n101 =
        hash(i + vec3(1.0, 0.0, 1.0));

    float n011 =
        hash(i + vec3(0.0, 1.0, 1.0));

    float n111 =
        hash(i + vec3(1.0, 1.0, 1.0));

    float nx00 =
        mix(
            n000,
            n100,
            f.x
        );

    float nx10 =
        mix(
            n010,
            n110,
            f.x
        );

    float nx01 =
        mix(
            n001,
            n101,
            f.x
        );

    float nx11 =
        mix(
            n011,
            n111,
            f.x
        );

    float nxy0 =
        mix(
            nx00,
            nx10,
            f.y
        );

    float nxy1 =
        mix(
            nx01,
            nx11,
            f.y
        );

    return
        mix(
            nxy0,
            nxy1,
            f.z
        );
}


/*
 * =========================================================
 * FBM
 * =========================================================
 *
 * 多层噪声叠加。
 */
float fbm(vec3 p) {

    float value = 0.0;
    float amplitude = 0.5;

    value +=
        noise(p)
        * amplitude;

    p *= 2.0;
    amplitude *= 0.5;

    value +=
        noise(p)
        * amplitude;

    p *= 2.0;
    amplitude *= 0.5;

    value +=
        noise(p)
        * amplitude;

    return value;
}


void main() {

    /*
     * =========================================================
     * 场景颜色
     * =========================================================
     */

    vec4 scene =
        texture(
            DiffuseSampler,
            texCoord
        );

    /*
     * =========================================================
     * 深度
     * =========================================================
     */

    float depth =
        texture(
            MainDepthSampler,
            texCoord
        ).r;

    /*
     * =========================================================
     * Region Identity
     * =========================================================
     */

    float pixelRegion =
        texture(
            GhostMistRegionIdentity,
            texCoord
        ).r;

    /*
     * =========================================================
     * 空间隔离
     * =========================================================
     *
     * 当前屏幕像素不属于鬼雾源头所在空间，
     * 则鬼雾完全不能传播到这里。
     */

    if (
        abs(
            pixelRegion
            - GhostMistSourceRegion
        )
        > 0.001
    ) {

        fragColor =
        scene;

        return;
    }


    /*
     * =========================================================
     * 屏幕坐标 → NDC
     * =========================================================
     */

    vec2 ndcXY =
        texCoord * 2.0
        - 1.0;


    /*
     * =========================================================
     * 近裁剪面
     * =========================================================
     */

    vec4 nearNDC =
        vec4(
            ndcXY,
            -1.0,
            1.0
        );


    /*
     * =========================================================
     * 远裁剪面
     * =========================================================
     */

    vec4 farNDC =
        vec4(
            ndcXY,
            1.0,
            1.0
        );


    /*
     * =========================================================
     * NDC → View Space
     * =========================================================
     */

    vec4 nearView =
        inverse(
            GhostMistProjMat
        )
        * nearNDC;

    vec4 farView =
        inverse(
            GhostMistProjMat
        )
        * farNDC;

    nearView /= nearView.w;

    farView /= farView.w;


    /*
     * =========================================================
     * View Space → World Space
     * =========================================================
     */

    vec4 nearWorld =
        inverse(
            GhostMistModelViewMat
        )
        * nearView;

    vec4 farWorld =
        inverse(
            GhostMistModelViewMat
        )
        * farView;

    nearWorld /= nearWorld.w;

    farWorld /= farWorld.w;


    /*
     * =========================================================
     * 构造世界空间射线
     * =========================================================
     */

    vec3 rayStart =
        nearWorld.xyz
        + GhostMistCameraPos;

    vec3 rayEnd =
        farWorld.xyz
        + GhostMistCameraPos;

    vec3 rayDirection =
        normalize(
            rayEnd - rayStart
        );


    /*
     * =========================================================
     * 当前场景表面世界坐标
     * =========================================================
     */

    vec4 sceneNDC =
        vec4(
            ndcXY,
            depth * 2.0 - 1.0,
            1.0
        );

    vec4 sceneView =
        inverse(
            GhostMistProjMat
        )
        * sceneNDC;

    sceneView /= sceneView.w;

    vec4 sceneWorld =
        inverse(
            GhostMistModelViewMat
        )
        * sceneView;

    sceneWorld /= sceneWorld.w;

    vec3 sceneWorldPos =
        sceneWorld.xyz
        + GhostMistCameraPos;


    /*
     * =========================================================
     * 场景表面距离
     * =========================================================
     */

    float sceneDistance =
        dot(
            sceneWorldPos - rayStart,
            rayDirection
        );


    /*
     * =========================================================
     * 射线与球形鬼域求交
     * =========================================================
     */

    vec3 sphereOffset =
        rayStart
        - GhostMistDomainCenter;

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
        GhostMistDomainRadius
        *
        GhostMistDomainRadius;

    float discriminant =
        b * b - c;


    /*
     * =========================================================
     * 默认没有雾
     * =========================================================
     */

    float fogAmount = 0.0;


    if (
        discriminant >= 0.0
        &&
        GhostMistDomainActive > 0.0
    ) {

        float sqrtD =
            sqrt(
                discriminant
            );

        float tEnter =
            -b - sqrtD;

        float tExit =
            -b + sqrtD;


        /*
         * =====================================================
         * 射线确实穿过鬼域
         * =====================================================
         */

        if (tExit > 0.0) {

            tEnter =
                max(
                    tEnter,
                    0.0
                );


            /*
             * =================================================
             * 场景物体截断鬼域
             * =================================================
             */

            float visibleExit =
                min(
                    tExit,
                    sceneDistance
                );


            if (
                visibleExit
                > tEnter
            ) {

                float fogLength =
                    visibleExit
                    - tEnter;


                /*
                 * =================================================
                 * 计算雾气采样位置
                 * =================================================
                 */

                float sampleT =
                    tEnter
                    + fogLength * 0.5;

                vec3 samplePosition =
                    rayStart
                    + rayDirection
                    * sampleT;


                /*
                 * =================================================
                 * 程序雾
                 * =================================================
                 *
                 * 时间变化让雾气缓慢移动。
                 */

                vec3 noisePosition =
                    samplePosition
                    * 0.045;

                noisePosition.x +=
                    GhostMistTime
                    * 0.012;

                noisePosition.y +=
                    GhostMistTime
                    * 0.006;

                noisePosition.z +=
                    GhostMistTime
                    * 0.009;


                float fogNoise =
                    fbm(
                        noisePosition
                    );


                /*
                 * =================================================
                 * 雾噪声重新映射
                 * =================================================
                 */

                fogNoise =
                    smoothstep(
                        0.25,
                        0.80,
                        fogNoise
                    );


                /*
                 * =================================================
                 * 鬼域层数
                 * =================================================
                 *
                 * 层数越高，鬼雾越浓。
                 */

                float layerStrength =
                    1.0 + (GhostMistDomainLayer - 1.0) * 0.18;

                layerStrength =
                    clamp(
                        layerStrength,
                        1.0,
                        2.5
                    );


                /*
                 * =================================================
                 * 基础雾密度
                 * =================================================
                 */

                float density = 0.045 * layerStrength;


                /*
                 * =================================================
                 * Beer-Lambert 风格雾化
                 * =================================================
                 */

                float baseFog =
                1.0
                -
                    exp(
                        -fogLength
                        * density
                    );


                /*
                 * =================================================
                 * 噪声影响雾密度
                 * =================================================
                 */

                float noisyFog =
                    baseFog
                    * (
                        0.65
                        +
                        fogNoise
                        * 0.55
                    );


                /*
                 * =================================================
                 * 球体边缘轻微衰减
                 * =================================================
                 *
                 * 越接近鬼域边界，
                 * 雾越柔和。
                 */

                vec3 cameraToDomain =
                    rayStart
                    - GhostMistDomainCenter;

                float cameraDistance =
                    length(
                        cameraToDomain
                    );

                float boundaryFade =
                    clamp(
                        (
                        GhostMistDomainRadius
                        - cameraDistance
                        )
                        /
                        12.0,
                        0.0,
                        1.0
                    );

                /*
                 * 摄像机在球外时，
                 * 不直接削掉雾。
                 *
                 * 只有靠近边界时进行轻微柔化。
                 */
                if (
                    cameraDistance
                    > GhostMistDomainRadius
                ) {

                    boundaryFade =
                        clamp(
                            (
                            GhostMistDomainRadius
                            - cameraDistance
                            )
                            /
                            10.0
                            + 1.0,
                            0.0,
                            1.0
                        );
                }

                noisyFog *= boundaryFade;


                fogAmount =
                    clamp(
                        noisyFog,
                        0.0,
                        0.92
                    );
            }
        }
    }


    /*
     * =========================================================
     * 鬼雾颜色
     * =========================================================
     *
     * 不使用纯白。
     *
     * 稍微偏灰，
     * 让它更像真正的浓雾。
     */

    vec3 mistColor =
        vec3(
            0.78,
            0.80,
            0.80
        );


    /*
     * =========================================================
     * 最终混合
     * =========================================================
     */

    vec3 finalColor =
        mix(
            scene.rgb,
            mistColor,
            fogAmount
        );


    /*
     * =========================================================
     * 输出
     * =========================================================
     */

    fragColor =
        vec4(
            finalColor,
            scene.a
        );
}