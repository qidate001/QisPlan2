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
bool isInsideCuboid(vec3 worldPos) {

    return worldPos.x >= IsolationCuboidMin.x
    && worldPos.x <= IsolationCuboidMax.x
    && worldPos.y >= IsolationCuboidMin.y
    && worldPos.y <= IsolationCuboidMax.y
    && worldPos.z >= IsolationCuboidMin.z
    && worldPos.z <= IsolationCuboidMax.z;
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

        if (isInsideCuboid(sceneWorldPos)) {
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