#version 150

uniform sampler2D MainDepthSampler;

/*
 * 灵异隔绝 Cuboid GPU 数据。
 *
 * Texture：
 *
 * width  = 2
 * height = MAX_CUBOIDS
 *
 * x = 0 → Min
 * x = 1 → Max
 *
 * y = Cuboid index
 */
uniform sampler2D IsolationCuboidData;

/*
 * 当前有效 Cuboid 数量。
 */
uniform float IsolationCuboidCount;

uniform mat4 IsolationProjMat;
uniform mat4 IsolationModelViewMat;

uniform vec3 IsolationCameraPos;

in vec2 texCoord;

out vec4 fragColor;


/*
 * ============================================================
 * 判断一条射线是否穿过指定 Cuboid。
 * ============================================================
 *
 * rayOrigin：
 *     摄像机附近的射线起点
 *
 * rayEnd：
 *     当前像素对应的可见世界位置
 *
 * Cuboid：
 *     [min, max)
 *
 * 如果摄像机到当前可见表面之间
 * 穿过 Cuboid，则返回 true。
 */
bool rayPassesThroughCuboid(
        vec3 rayOrigin,
        vec3 rayEnd,
        vec3 cuboidMin,
        vec3 cuboidMax
) {

    vec3 direction = rayEnd - rayOrigin;

    /*
     * 射线参数范围：
     *
     * t = 0：
     * 摄像机位置
     *
     * t = 1：
     * 当前像素对应的可见世界位置
     */
    float tMin = 0.0;
    float tMax = 1.0;


    /*
     * ========================================================
     * X 轴
     * ========================================================
     */
    if (abs(direction.x) < 0.000001) {

        /*
         * 射线几乎平行于 X 轴。
         *
         * 如果当前 X 不在 Cuboid 内，
         * 那么不可能穿过 Cuboid。
         */
        if (rayOrigin.x < cuboidMin.x
            || rayOrigin.x >= cuboidMax.x) {

            return false;
        }

    } else {

        float invDirection =
            1.0 / direction.x;

        float t1 =
            (cuboidMin.x - rayOrigin.x)
            * invDirection;

        float t2 =
            (cuboidMax.x - rayOrigin.x)
            * invDirection;

        if (t1 > t2) {
            float temp = t1;
            t1 = t2;
            t2 = temp;
        }

        tMin = max(tMin, t1);
        tMax = min(tMax, t2);

        if (tMin > tMax) {
            return false;
        }
    }


    /*
     * ========================================================
     * Y 轴
     * ========================================================
     */
    if (abs(direction.y) < 0.000001) {

        if (rayOrigin.y < cuboidMin.y
            || rayOrigin.y >= cuboidMax.y) {

            return false;
        }

    } else {

        float invDirection =
            1.0 / direction.y;

        float t1 =
            (cuboidMin.y - rayOrigin.y)
            * invDirection;

        float t2 =
            (cuboidMax.y - rayOrigin.y)
            * invDirection;

        if (t1 > t2) {
            float temp = t1;
            t1 = t2;
            t2 = temp;
        }

        tMin = max(tMin, t1);
        tMax = min(tMax, t2);

        if (tMin > tMax) {
            return false;
        }
    }


    /*
     * ========================================================
     * Z 轴
     * ========================================================
     */
    if (abs(direction.z) < 0.000001) {

        if (rayOrigin.z < cuboidMin.z
            || rayOrigin.z >= cuboidMax.z) {

            return false;
        }

    } else {

        float invDirection =
            1.0 / direction.z;

        float t1 =
            (cuboidMin.z - rayOrigin.z)
            * invDirection;

        float t2 =
            (cuboidMax.z - rayOrigin.z)
            * invDirection;

        if (t1 > t2) {
            float temp = t1;
            t1 = t2;
            t2 = temp;
        }

        tMin = max(tMin, t1);
        tMax = min(tMax, t2);

        if (tMin > tMax) {
            return false;
        }
    }


    /*
     * 因为 tMin / tMax 已经被限制在：
     *
     * 0 ≤ t ≤ 1
     *
     * 所以只要存在有效交集，
     * 就说明摄像机到当前可见表面之间
     * 穿过了这个 Cuboid。
     */
    return tMax >= 0.0
        && tMin <= 1.0;
}


/*
 * ============================================================
 * 获取 Cuboid Min。
 * ============================================================
 */
vec3 getCuboidMin(int index) {

    return texelFetch(
            IsolationCuboidData,
            ivec2(0, index),
            0
    ).xyz;
}


/*
 * ============================================================
 * 获取 Cuboid Max。
 * ============================================================
 */
vec3 getCuboidMax(int index) {

    return texelFetch(
            IsolationCuboidData,
            ivec2(1, index),
            0
    ).xyz;
}


void main() {

    /*
     * ========================================================
     * 当前像素对应的深度。
     * ========================================================
     */
    float depth =
        texture(
            MainDepthSampler,
            texCoord
        ).r;


    /*
     * ========================================================
     * 屏幕坐标 → NDC
     * ========================================================
     */
    vec2 ndcXY = texCoord * 2.0 - 1.0;


    /*
     * ========================================================
     * 构造近裁剪面 / 远裁剪面。
     * ========================================================
     */
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


    /*
     * ========================================================
     * NDC → View Space
     * ========================================================
     */
    vec4 nearView =
        inverse(
            IsolationProjMat
        ) * nearNDC;

    vec4 farView =
        inverse(
            IsolationProjMat
        ) * farNDC;

    nearView /= nearView.w;
    farView /= farView.w;


    /*
     * ========================================================
     * View Space → World Space
     * ========================================================
     */
    vec4 nearWorld =
        inverse(
            IsolationModelViewMat
        ) * nearView;

    vec4 farWorld =
        inverse(
            IsolationModelViewMat
        ) * farView;

    nearWorld /= nearWorld.w;
    farWorld /= farWorld.w;


    /*
     * Minecraft 当前矩阵使用的是
     * 相对于 Camera 的坐标。
     *
     * 因此重新加回 Camera Position。
     */
    vec3 rayStart =
        nearWorld.xyz
        + IsolationCameraPos;

    vec3 rayEnd =
        farWorld.xyz
        + IsolationCameraPos;


    /*
     * ========================================================
     * 根据深度恢复当前像素真正对应的世界坐标。
     * ========================================================
     */
    vec4 sceneNDC =
        vec4(
            ndcXY,
            depth * 2.0 - 1.0,
            1.0
        );

    vec4 sceneView =
        inverse(
            IsolationProjMat
        ) * sceneNDC;

    sceneView /= sceneView.w;

    vec4 sceneWorld =
        inverse(
            IsolationModelViewMat
        ) * sceneView;

    sceneWorld /= sceneWorld.w;

    vec3 sceneWorldPos =
        sceneWorld.xyz
        + IsolationCameraPos;


    /*
     * ========================================================
     * 默认：
     *
     * 不是灵异隔绝空间。
     * ========================================================
     */
    float isolated = 0.0;


    /*
     * ========================================================
     * 遍历所有 Cuboid。
     * ========================================================
     *
     * GPU 数据纹理最多有 MAX_CUBOIDS 个 Cuboid。
     *
     * 这里使用固定循环上限，
     * 通过 IsolationCuboidCount 控制实际读取数量。
     */
    int count =
    int(IsolationCuboidCount);

    const int MAX_CUBOIDS = 256;

    for (int i = 0; i < MAX_CUBOIDS; i++) {

        /*
         * 已经超过当前实际 Cuboid 数量。
         */
        if (i >= count) {
            break;
        }


        /*
         * 从 GPU Texture 获取
         * 当前 Cuboid 的 Min / Max。
         */
        vec3 cuboidMin = getCuboidMin(i);

        vec3 cuboidMax = getCuboidMax(i);


        /*
         * 判断摄像机到当前可见表面
         * 是否穿过这个 Cuboid。
         */
        if (rayPassesThroughCuboid(
                rayStart,
                sceneWorldPos,
                cuboidMin,
                cuboidMax
        )) {

            isolated = 1.0;

            /*
             * 一个 Cuboid 命中即可。
             */
            break;
        }
    }


    /*
     * ========================================================
     * 调试阶段：
     *
     * 白色 = 灵异隔绝空间
     * 黑色 = 非灵异隔绝空间
     * ========================================================
     */
    fragColor =
        vec4(
            isolated,
            isolated,
            isolated,
            1.0
        );
}