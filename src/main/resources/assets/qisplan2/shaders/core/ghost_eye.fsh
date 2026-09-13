#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D MainDepthSampler;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;

uniform vec3 CameraPos;
uniform vec3 GhostEyeDomainCenter;

uniform float GhostEyeDomainRadius;
uniform float GhostEyeDomainActive;
uniform float GhostEyeDomainLayer;

in vec2 texCoord;

out vec4 fragColor;

void main() {

    // -----------------------------
    // 1. 读取深度
    // -----------------------------

    float depth =
    texture(MainDepthSampler, texCoord).r;


    // -----------------------------
    // 2. 屏幕坐标 → NDC
    // -----------------------------

    vec2 ndcXY =
    texCoord * 2.0 - 1.0;

    float ndcZ =
    depth * 2.0 - 1.0;

    vec4 ndcPosition =
    vec4(
            ndcXY,
            ndcZ,
            1.0
    );


    // -----------------------------
    // 3. NDC → View Space
    // -----------------------------

    vec4 viewPosition =
        inverse(ProjMat)
        * ndcPosition;

    viewPosition /=
    viewPosition.w;


    // -----------------------------
    // 4. View Space → World Space
    // -----------------------------

    vec4 worldPosition =
        inverse(ModelViewMat)
        * viewPosition;

    worldPosition /=
    worldPosition.w;


    // -----------------------------
    // 5. 加回摄像机世界坐标
    // -----------------------------

    vec3 worldPos =
        worldPosition.xyz
        + CameraPos;


    // -----------------------------
    // 6. 测试世界坐标
    // -----------------------------

    vec3 delta =
        worldPos - GhostEyeDomainCenter;

    float distance =
        length(delta);

    float inside =
    step(
            distance,
            GhostEyeDomainRadius
    );

    inside *= GhostEyeDomainActive;

    vec4 scene =
    texture(DiffuseSampler, texCoord);

    vec3 red =
        vec3(1.0, 0.0, 0.0);

    float redStrength =
        0.15 + (GhostEyeDomainLayer - 1.0) * 0.10;

    redStrength =
        clamp(
                redStrength,
                0.15,
                0.65
        );

    vec3 ghostColor =
        mix(
            scene.rgb,
            red,
            redStrength
        );

    vec3 finalColor =
        mix(
                scene.rgb,
                ghostColor,
                inside
        );

    fragColor = vec4(
            finalColor,
            scene.a
    );
}