#version 150
// 九十九由基·黑洞（世界空间版，r405）
// 与 r390~r404 的屏幕空间版不同：这里**真的在 3D 里走射线**——
//   ① 用深度重建每个像素的世界坐标与射线方向；
//   ② 沿射线做引力偏折（向黑洞中心弯），射线真的会绕开/绕后；
//   ③ 与一张**真实倾斜的薄盘**求交，所以盘子有近侧挡住核心、远侧被弯到上方；
//   ④ 背景用偏折后的方向重新投影回已渲染画面采样（拿不到就退回原像素）。
// 因此从侧面、背面看都成立，不再是"贴在镜头前的椭圆"。
uniform sampler2D Sampler0;      // 场景颜色副本
uniform sampler2D Sampler1;      // 场景深度副本
uniform mat4 InvViewProjection;  // 屏幕→世界
uniform mat4 ViewProjection;     // 世界→屏幕
uniform vec3 CamPos;             // 相机世界坐标
uniform vec3 HoleCenter;         // 黑洞中心（世界坐标）
uniform vec3 DiskNormal;         // 盘法线（已含倾角）
uniform vec4 Params;             // x=视界半径 y=盘内缘 z=盘外缘 w=盘厚
uniform vec2 FrameSize;
uniform float Time;
uniform float Alpha;
uniform vec4 InnerColor;         // 冰蓝内盘
uniform vec4 OuterColor;         // 橙色外盘
in vec2 uv;
out vec4 fragColor;

const int STEPS = 56;
const float STEP_SCALE = 0.16;   // 步长 = 视界半径 × 该系数

vec3 worldFromDepth(vec2 screenUV, float d) {
    vec4 p = InvViewProjection * vec4(screenUV * 2.0 - 1.0, d * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

// 偏折后的方向重新投影回屏幕，采样已经渲染好的场景当背景。
vec3 sampleBackground(vec3 eye, vec3 dir, vec2 fallbackUV) {
    vec4 clip = ViewProjection * vec4(eye + dir * 2048.0, 1.0);
    if (clip.w <= 0.0) return texture(Sampler0, fallbackUV).rgb;
    vec2 s = clip.xy / clip.w * 0.5 + 0.5;
    if (s.x < 0.002 || s.x > 0.998 || s.y < 0.002 || s.y > 0.998) return texture(Sampler0, fallbackUV).rgb;
    return texture(Sampler0, s).rgb;
}

void main() {
    vec3 base = texture(Sampler0, uv).rgb;
    if (Alpha <= 0.001) { fragColor = vec4(base, 1.0); return; }

    float depth = texture(Sampler1, uv).r;
    bool sky = depth >= 0.999999;
    vec3 surface = worldFromDepth(uv, sky ? 1.0 : depth);

    // 相机 → 像素的世界射线
    vec3 rayDir = normalize(surface - CamPos);
    vec3 toHole = HoleCenter - CamPos;
    float holeDist = length(toHole);

    // 这道像素本来看到的东西比黑洞更近 → 被前景挡住，保持原样（黑洞不会透墙）。
    if (!sky && length(surface - CamPos) < holeDist - Params.x * 1.05) {
        fragColor = vec4(base, 1.0);
        return;
    }

    float r = Params.x;                 // 视界半径
    float diskIn = Params.y;
    float diskOut = Params.z;
    float diskThick = Params.w;
    float stepLen = r * STEP_SCALE;

    vec3 pos = CamPos;
    vec3 dir = rayDir;
    vec3 color = vec3(0.0);
    float transmit = 1.0;
    bool swallowed = false;
    float closest = 1e9;

    for (int i = 0; i < STEPS; i++) {
        vec3 rel = HoleCenter - pos;
        float dist = length(rel);
        closest = min(closest, dist);

        // 引力偏折：给射线加一个朝中心的加速度（弱场近似，够出"绕过去"的观感）
        float pull = 1.35 * r * r / max(dist * dist * dist, 1e-4);
        dir = normalize(dir + normalize(rel) * pull * stepLen);

        pos += dir * stepLen;

        // 视界：吸进去
        if (length(pos - HoleCenter) < r) { swallowed = true; break; }

        // 与薄盘求交（绝对值厚度带）
        float h = dot(pos - HoleCenter, DiskNormal);
        if (abs(h) < diskThick) {
            vec3 relPos = pos - HoleCenter;
            float rr = length(relPos);
            if (rr > diskIn && rr < diskOut) {
                float t = clamp((rr - diskIn) / max(diskOut - diskIn, 1e-3), 0.0, 1.0);
                // 内圈冰蓝，外圈橙；中间平滑过渡
                vec3 diskColor = mix(InnerColor.rgb, OuterColor.rgb, smoothstep(0.25, 0.95, t));
                // 角向条纹 + 径向细纹（程序化，不依赖贴图）
                float ang = atan(dot(relPos, normalize(cross(DiskNormal, vec3(0.0, 1.0, 0.0)) + 1e-4)),
                                 dot(relPos, normalize(cross(vec3(0.0, 1.0, 0.0), DiskNormal) + 1e-4)));
                float streak = 0.72 + 0.28 * sin(ang * 7.0 + rr * 3.1 - Time * 1.7);
                float density = (0.55 - 0.35 * smoothstep(0.55, 1.0, t)) * streak;
                // 越靠内越亮（贴近视界被加热）
                density *= mix(1.35, 0.55, t);
                color += diskColor * density * transmit;
                transmit *= (1.0 - density * 0.65);
                if (transmit < 0.03) break;
            }
        }

        if (length(pos - CamPos) > holeDist + r * 24.0) break;
    }

    vec3 bg = swallowed ? vec3(0.0) : sampleBackground(CamPos, dir, uv);
    // 视界边缘一圈很细的"光子环"：射线擦过视界时会绕过，这里用最近距离近似提亮
    float photon = exp(-pow((closest / r - 1.0) * (swallowed ? 9.0 : 26.0), 2.0));
    vec3 ring = mix(InnerColor.rgb, vec3(1.0), 0.5) * photon * (swallowed ? 0.25 : 0.6);

    vec3 col = bg * transmit + color + ring * Alpha;
    // 黑洞吸光：背景整体稍压暗，避免白天把我们叠的盘冲白
    col *= mix(1.0, 0.82, Alpha * smoothstep(1.0, 3.0, length(surface - HoleCenter) / r));
    fragColor = vec4(clamp(mix(base, col, Alpha), 0.0, 1.0), 1.0);
}
