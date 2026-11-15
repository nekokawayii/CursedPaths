#version 150
// 九十九由基·黑洞（世界空间版 r408）
// 射线在 3D 里真的被偏折、真的和倾斜薄盘求交，所以侧面/背面都成立。
// r408 修两处致命观感问题：
//   ① 背景重采样原来对**每个像素**都做 → 整屏天空被卷成"两个世界"；
//      现在用黑洞在屏幕上的投影算一个**局部遮罩**，只在它附近生效。
//   ② 盘面过去用"厚度带"判定，射线每一步都落在带里 → 叠出几十圈同心圆；
//      现在改成**检测射线穿过盘平面的那一瞬**，每次穿越只采一次色。
uniform sampler2D Sampler0;      // 场景颜色副本
uniform sampler2D Sampler1;      // 场景深度副本
uniform mat4 InvViewProjection;  // 屏幕→世界
uniform mat4 ViewProjection;     // 世界→屏幕
uniform vec3 CamPos;
uniform vec3 HoleCenter;
uniform vec3 DiskNormal;         // 盘法线（已含倾角）
uniform vec4 Params;             // x=视界半径 y=盘内缘 z=盘外缘 w=盘厚（仅用于软边）
uniform vec2 FrameSize;
uniform float Time;
uniform float Alpha;
uniform vec4 InnerColor;
uniform vec4 OuterColor;
in vec2 uv;
out vec4 fragColor;

const int STEPS = 44;
const float STEP_SCALE = 0.22;

vec3 worldFromDepth(vec2 screenUV, float d) {
    vec4 p = InvViewProjection * vec4(screenUV * 2.0 - 1.0, d * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

void main() {
    vec3 base = texture(Sampler0, uv).rgb;
    if (Alpha <= 0.001) { fragColor = vec4(base, 1.0); return; }

    float r = Params.x;
    float diskIn = Params.y;
    float diskOut = Params.z;
    float diskThick = max(Params.w, r * 0.02);

    // ---- 局部遮罩：只在黑洞投影附近改画面，避免整屏被卷 ----
    vec4 clipC = ViewProjection * vec4(HoleCenter, 1.0);
    if (clipC.w <= 0.01) { fragColor = vec4(base, 1.0); return; }
    vec2 screenC = clipC.xy / clipC.w * 0.5 + 0.5;
    float aspect = FrameSize.x / max(FrameSize.y, 1.0);
    vec2 md = (uv - screenC) * vec2(aspect, 1.0);
    float distScreen = length(md);
    float radiusOuter = abs(ViewProjection[1][1] * diskOut / clipC.w) * 0.5;   // uv 高度比例
    float mask = 1.0 - smoothstep(radiusOuter * 1.0, radiusOuter * 1.7, distScreen);
    if (mask <= 0.001) { fragColor = vec4(base, 1.0); return; }

    float depth = texture(Sampler1, uv).r;
    bool sky = depth >= 0.999999;
    vec3 surface = worldFromDepth(uv, sky ? 1.0 : depth);
    vec3 rayDir = normalize(surface - CamPos);

    vec3 toHole = HoleCenter - CamPos;
    float holeDist = length(toHole);
    // 前景挡住（看到的表面比黑洞更近）→ 该像素保持原样
    if (!sky && length(surface - CamPos) < holeDist - r * 1.05) { fragColor = vec4(base, 1.0); return; }

    vec3 nrm = normalize(DiskNormal);
    vec3 refAxis = abs(nrm.y) > 0.9 ? vec3(0.0, 0.0, 1.0) : vec3(0.0, 1.0, 0.0);
    vec3 tangent = normalize(cross(nrm, refAxis));
    vec3 bitangent = normalize(cross(nrm, tangent));

    vec3 pos = CamPos;
    vec3 dir = rayDir;
    vec3 color = vec3(0.0);
    float transmit = 1.0;
    bool swallowed = false;
    float closest = 1e9;
    float stepLen = r * STEP_SCALE;
    float hPrev = dot(CamPos - HoleCenter, nrm);

    for (int i = 0; i < STEPS; i++) {
        vec3 rel = HoleCenter - pos;
        float dist = length(rel);
        closest = min(closest, dist);

        // 引力偏折（弱场近似）
        float pull = 1.25 * r * r / max(dist * dist * dist, 1e-4);
        dir = normalize(dir + normalize(rel) * pull * stepLen);
        pos += dir * stepLen;

        if (length(pos - HoleCenter) < r) { swallowed = true; break; }

        // ---- 盘面：只在穿过平面的那一瞬采样（避免同心圆叠加）----
        float hCur = dot(pos - HoleCenter, nrm);
        if (hPrev * hCur < 0.0 && transmit > 0.03) {
            float t = hPrev / (hPrev - hCur);
            vec3 hit = mix(pos - dir * stepLen, pos, clamp(t, 0.0, 1.0));
            vec3 relHit = hit - HoleCenter;
            float rr = length(relHit);
            if (rr > diskIn && rr < diskOut) {
                float k = clamp((rr - diskIn) / max(diskOut - diskIn, 1e-3), 0.0, 1.0);
                vec3 diskColor = mix(InnerColor.rgb, OuterColor.rgb, smoothstep(0.25, 0.95, k));
                float ang = atan(dot(relHit, bitangent), dot(relHit, tangent));
                float streak = 0.74 + 0.26 * sin(ang * 6.0 + rr * 2.3 - Time * 1.6);
                // 内缘更亮更热
                float density = mix(1.25, 0.45, k) * streak;
                // 软边：靠近内/外缘淡出，避免硬切
                density *= smoothstep(0.0, diskThick * 8.0, rr - diskIn)
                         * (1.0 - smoothstep(diskOut - diskThick * 8.0, diskOut, rr));
                color += diskColor * density * transmit;
                transmit *= (1.0 - density * 0.7);
            }
        }
        hPrev = hCur;

        if (length(pos - CamPos) > holeDist + r * 22.0) break;
    }

    // ---- 背景：只在局部遮罩内、且没被吞掉时才做偏折重采样 ----
    vec3 bg = base;
    if (!swallowed) {
        vec4 clipB = ViewProjection * vec4(CamPos + dir * 2048.0, 1.0);
        if (clipB.w > 0.0) {
            vec2 s = clipB.xy / clipB.w * 0.5 + 0.5;
            if (s.x > 0.004 && s.x < 0.996 && s.y > 0.004 && s.y < 0.996) bg = texture(Sampler0, s).rgb;
        }
    } else {
        bg = vec3(0.0);
    }

    float photon = exp(-pow((closest / r - 1.0) * (swallowed ? 10.0 : 30.0), 2.0));
    vec3 ring = mix(InnerColor.rgb, vec3(1.0), 0.5) * photon * (swallowed ? 0.22 : 0.55);

    vec3 col = bg * transmit + color + ring;
    fragColor = vec4(clamp(mix(base, col, mask * Alpha), 0.0, 1.0), 1.0);
}
