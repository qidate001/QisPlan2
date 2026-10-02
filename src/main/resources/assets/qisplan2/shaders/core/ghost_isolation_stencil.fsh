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
 */
bool rayPassesThroughCuboid(
        vec3 rayOrigin,
        vec3 rayEnd,
        vec3 cuboidMin,
        vec3 cuboidMax
) {

    vec3 direction = rayEnd - rayOrigin;

    float tMin = 0.0;
    float tMax = 1.0;


    /*
     * X
     */
    if (abs(direction.x) < 0.000001) {

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
     * Y
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
     * Z
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

    return tMax >= 0.0
    && tMin <= 1.0;
}


/*
 * ============================================================
 * 获取 Cuboid Min
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
 * 获取 Cuboid Max
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
     * 当前像素深度
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
    vec2 ndcXY =
    texCoord * 2.0 - 1.0;


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
     * Minecraft 的 ModelView
     * 使用相对于 Camera 的坐标。
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
     * 根据深度恢复当前像素的世界坐标。
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
     * 默认不是隔绝区域。
     * ========================================================
     */
    bool isolated = false;


    /*
     * ========================================================
     * 遍历所有 Cuboid。
     * ========================================================
     */
    int count =
    int(IsolationCuboidCount);

    const int MAX_CUBOIDS = 256;

    for (int i = 0; i < MAX_CUBOIDS; i++) {

        if (i >= count) {
            break;
        }

        vec3 cuboidMin =
        getCuboidMin(i);

        vec3 cuboidMax =
        getCuboidMax(i);

        if (rayPassesThroughCuboid(
                rayStart,
                sceneWorldPos,
                cuboidMin,
                cuboidMax
        )) {

            isolated = true;

            break;
        }
    }


    /*
     * ========================================================
     * Stencil 写入 Pass
     * ========================================================
     *
     * 隔绝区域：
     *
     *     正常输出
     *     → OpenGL StencilOp = REPLACE
     *     → Stencil = 1
     *
     * 非隔绝区域：
     *
     *     discard
     *     → 不执行 Stencil REPLACE
     */
    if (isolated) {

        fragColor =
        vec4(
                1.0,
                1.0,
                1.0,
                1.0
        );

        return;
    }

    discard;
}