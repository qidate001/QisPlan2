#version 150

uniform sampler2D MainDepthSampler;

uniform mat4 IsolationProjMat;
uniform mat4 IsolationModelViewMat;

uniform vec3 IsolationCameraPos;

uniform vec3 IsolationCuboidMin;
uniform vec3 IsolationCuboidMax;

uniform float IsolationCuboidActive;

in vec2 texCoord;

out vec4 fragColor;


/*
 * 判断一个世界坐标是否位于
 * 当前灵异隔绝 Cuboid 内。
 */
bool rayIntersectsCuboid(
        vec3 rayOrigin,
        vec3 rayEnd
) {
    vec3 direction =
    rayEnd - rayOrigin;

    /*
     * 射线与 Cuboid 的参数范围。
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
        if (rayOrigin.x < IsolationCuboidMin.x
            || rayOrigin.x > IsolationCuboidMax.x) {

            return false;
        }

    } else {

        float invDirection =
        1.0 / direction.x;

        float t1 =
        (IsolationCuboidMin.x - rayOrigin.x)
        * invDirection;

        float t2 =
        (IsolationCuboidMax.x - rayOrigin.x)
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

        if (rayOrigin.y < IsolationCuboidMin.y
            || rayOrigin.y > IsolationCuboidMax.y) {

            return false;
        }

    } else {

        float invDirection =
        1.0 / direction.y;

        float t1 =
        (IsolationCuboidMin.y - rayOrigin.y)
        * invDirection;

        float t2 =
        (IsolationCuboidMax.y - rayOrigin.y)
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

        if (rayOrigin.z < IsolationCuboidMin.z
            || rayOrigin.z > IsolationCuboidMax.z) {

            return false;
        }

    } else {

        float invDirection =
        1.0 / direction.z;

        float t1 =
        (IsolationCuboidMin.z - rayOrigin.z)
        * invDirection;

        float t2 =
        (IsolationCuboidMax.z - rayOrigin.z)
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
     * tMin / tMax 始终限制在：
     *
     * 0 ≤ t ≤ 1
     *
     * 因此这里只判断：
     * 摄像机到当前可见表面之间，
     * 是否经过了 Cuboid。
     */
    return tMax >= 0.0
    && tMin <= 1.0;
}


void main() {

    /*
     * 当前像素对应的深度。
     */
    float depth =
    texture(
            MainDepthSampler,
            texCoord
    ).r;


    /*
     * 屏幕坐标转换到 NDC。
     */
    vec2 ndcXY =
    texCoord * 2.0 - 1.0;


    /*
     * 构造当前像素的近裁剪面和远裁剪面。
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
     * NDC → View Space。
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
     * View Space → World Space。
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
     * 根据深度恢复当前像素真正对应的世界坐标。
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
     * 默认不是灵异隔绝空间。
     */
    float isolated = 0.0;


    /*
     * 当前 Cuboid 有效时，
     * 判断世界坐标。
     */
    if (IsolationCuboidActive > 0.5) {

        if (rayIntersectsCuboid(
                rayStart,
                sceneWorldPos
        )) {
            isolated = 1.0;
        }
    }


    /*
     * 调试阶段：
     *
     * 白色 = 灵异隔绝空间
     * 黑色 = 非灵异隔绝空间
     */
    fragColor =
        vec4(
            isolated,
            isolated,
            isolated,
            1.0
        );
}