#version 150
// 九十九由基·黑洞（世界空间版 r410）
// 射线在 3D 里被偏折、与真实倾斜薄盘求交，因此侧面/背面都成立。
// r410 的决定性简化（r408/r409 失败的教训）：
//   之前用「投影到屏幕算遮罩 / 把偏折后的方向重投影回去采样」，
//   但本工程的 ViewProjection 投影结果不可信（日志里算出屏幕半径 = 4.6 个屏高），
//   于是要么整屏被卷成两个世界、要么遮罩为 0 什么都不画。
//   现在**彻底不做屏幕空间投影**：
//     · 背景保持原样，只在射线真的擦过视界时压暗（吸光），被吞掉就是纯黑；
//     · 盘面靠射线与平面求交自然出现，不需要任何遮罩；
//     · 唯一的光是盘面 + 光子环。可预期的效果优先于"看起来更物理"。
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform mat4 InvViewProjection;
uniform mat4 ViewProjection;
uniform vec3 CamPos;
uniform vec3 HoleCenter;
uniform vec3 DiskNormal;
uniform vec4 Params;             // x=视界半径 y=盘内缘 z=盘外缘 w=盘厚
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

    float depth = texture(Sampler1, uv).r;
    bool sky = depth >= 0.999999;
    vec3 surface = worldFromDepth(uv, sky ? 1.0 : depth);
    vec3 rayDir = normalize(surface - CamPos);

    vec3 toHole = HoleCenter - CamPos;
    float holeDist = length(toHole);
    // 前面的地形比黑洞更近 → 保持原样（不透墙）
    if (!sky && length(surface - CamPos) < holeDist - r * 1.05) { fragColor = vec4(base, 1.0); return; }

    vec3 nrm = normalize(DiskNormal);
    vec3 refAxis = abs(nrm.y) > 0.9 ? vec3(0.0, 0.0, 1.0) : vec3(0.0, 1.0, 0.0);
    vec3 tangent = normalize(cross(nrm, refAxis));
    vec3 bitangent = normalize(cross(nrm, tangent));

    vec3 pos = CamPos;
    vec3 dir = rayDir;
    vec3 color = vec3(0.0);
    bool swallowed = false;
    float closest = 1e9;
    float stepLen = r * STEP_SCALE;
    float hPrev = dot(CamPos - HoleCenter, nrm);

    for (int i = 0; i < STEPS; i++) {
        vec3 rel = HoleCenter - pos;
        float dist = length(rel);
        closest = min(closest, dist);

        float pull = 1.25 * r * r / max(dist * dist * dist, 1e-4);
        dir = normalize(dir + normalize(rel) * pull * stepLen);
        pos += dir * stepLen;

        if (length(pos - HoleCenter) < r) { swallowed = true; break; }

        // 盘面：只在射线穿过平面的那一瞬采样一次
        float hCur = dot(pos - HoleCenter, nrm);
        if (hPrev * hCur < 0.0) {
            float t = clamp(hPrev / (hPrev - hCur), 0.0, 1.0);
            vec3 hit = mix(pos - dir * stepLen, pos, t);
            vec3 relHit = hit - HoleCenter;
            float rr = length(relHit);
            if (rr > diskIn && rr < diskOut) {
                float k = clamp((rr - diskIn) / max(diskOut - diskIn, 1e-3), 0.0, 1.0);
                vec3 diskColor = mix(InnerColor.rgb, OuterColor.rgb, smoothstep(0.25, 0.95, k));
                float ang = atan(dot(relHit, bitangent), dot(relHit, tangent));
                float streak = 0.74 + 0.26 * sin(ang * 6.0 + rr * 2.3 - Time * 1.6);
                // 细盘：只有射线正好擦过薄层才亮，避免整片糊成面
                float thin = exp(-pow(dot(hit - HoleCenter, nrm) / max(diskThick, 1e-3), 2.0) * 0.15);
                float density = mix(1.30, 0.40, k) * streak * thin;
                density *= smoothstep(0.0, diskThick * 10.0, rr - diskIn)
                         * (1.0 - smoothstep(diskOut - diskThick * 10.0, diskOut, rr));
                color += diskColor * density;
            }
        }
        hPrev = hCur;

        if (length(pos - CamPos) > holeDist + r * 22.0) break;
    }

    // ---- 背景：不投影、不重采样，只在擦过视界时压暗；被吞掉纯黑 ----
    vec3 bg;
    if (swallowed) {
        bg = vec3(0.0);
    } else {
        float shade = mix(0.08, 1.0, smoothstep(1.0, 5.5, closest / r));
        bg = base * shade;
    }

    float photon = exp(-pow((closest / r - 1.0) * (swallowed ? 9.0 : 26.0), 2.0));
    vec3 ring = mix(InnerColor.rgb, vec3(1.0), 0.5) * photon * (swallowed ? 0.20 : 0.45);

    vec3 col = bg + color + ring;
    fragColor = vec4(clamp(mix(base, col, Alpha), 0.0, 1.0), 1.0);
}
