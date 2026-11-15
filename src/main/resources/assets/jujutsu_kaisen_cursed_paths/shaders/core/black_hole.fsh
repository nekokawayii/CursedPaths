#version 150
// 九十九由基·黑洞：冰蓝视界 + 冰蓝内盘 + 橙色外盘 + 边缘透镜。
// 全屏后期：从主 framebuffer 的彩色/深度副本取景，只做屏幕空间透镜与盘面，
// 不改世界几何、不写方块、不影响任何游戏逻辑。
uniform sampler2D Sampler0;   // 场景颜色
uniform sampler2D Sampler1;   // 场景深度
uniform vec2 FrameSize;
uniform float Time;           // 秒
uniform float Alpha;          // 0..1 淡入淡出
uniform vec4 Lens;            // xy=屏幕位置(0..1) z=半径(uv 高比例) w=透镜强度
uniform vec4 InnerColor;      // 内盘/视界边缘（冰蓝）
uniform vec4 OuterColor;      // 外盘（橙）
in vec2 uv;
out vec4 fragColor;

const float INNER_IN = 1.02;
const float INNER_OUT = 1.92;
const float OUTER_IN = 2.12;
const float OUTER_OUT = 3.35;

float bandNoise(float angle, float radius, float speed) {
    // 角向条纹 + 径向细纹，整体缓慢旋转；不用贴图，避免依赖粒子素材。
    float spin = angle * 5.0 - Time * speed;
    float a = 0.5 + 0.5 * sin(spin + radius * 21.0);
    float b = 0.5 + 0.5 * sin(angle * 13.0 - radius * 34.0 + Time * speed * 1.7);
    return mix(a, b, 0.35);
}

void main() {
    vec2 center = Lens.xy;
    float radius = max(Lens.z, 0.0005);
    float aspect = FrameSize.x / max(FrameSize.y, 1.0);
    vec2 delta = (uv - center) * vec2(aspect, 1.0);
    float r = length(delta) / radius;

    vec3 base = texture(Sampler0, uv).rgb;
    if (Alpha <= 0.001) { fragColor = vec4(base, 1.0); return; }

    // ---- 事件视界：明显的暗核，边缘一道极窄的冰蓝光子环 ----
    if (r < 1.0) {
        vec3 core = vec3(0.003, 0.006, 0.014);
        float rim = smoothstep(0.90, 1.0, r);
        core += InnerColor.rgb * rim * 0.85;
        fragColor = vec4(mix(base, core, Alpha), 1.0);
        return;
    }

    // ---- 透镜：径向外推背景（r395：修正方向，之前按分量除 aspect 会把位移拉成竖的）----
    // 屏幕径向单位向量在 uv 空间要 x/aspect；直接乘 delta 再除 aspect 会把 x 除两次，
    // 导致位移偏竖直、看起来像"竖向拉伸"。这里先取径向单位再回 uv 空间，保证是圆形径向。
    vec2 radial = normalize(delta + vec2(1e-5));
    vec2 radialUV = vec2(radial.x / aspect, radial.y);
    float bend = clamp(Lens.w * 4.50 / (r * r + 0.03), 0.0, 6.0);
    vec2 warped = clamp(uv - radialUV * (bend * radius * 0.55),
                        1.0 / FrameSize, 1.0 - 1.0 / FrameSize);
    vec3 lensed = texture(Sampler0, warped).rgb;
    // 爱因斯坦环：视界外侧一圈被强烈压缩的背景，单独取一次并提亮。
    float ringBand = exp(-pow((r - 1.12) * 7.5, 2.0));
    if (ringBand > 0.02) {
        vec2 ringUV = clamp(uv - radialUV * (bend * radius * 1.35),
                            1.0 / FrameSize, 1.0 - 1.0 / FrameSize);
        lensed = mix(lensed, texture(Sampler0, ringUV).rgb * 1.35, ringBand * 0.32);
    }

    // ---- 双层吸积盘（屏幕空间环带）----
    // r397：把 y 乘大才是"压扁"成横向椭圆（乘小反而纵向更长）。
    // r399：光子环/光晕改用 rDisk，避免它们仍是正圆。
    // r400 失败记录：用 `diskDelta.y += ±curve`（按 y 的符号分别加减）会在中轴处产生**接缝**，
    //   画出来就是"白色四角星"——千万不要用带符号分支去弯盘面。
    // r401：改成**连续的径向放大**来表现盘面被引力弯：越靠近视界，盘面坐标被放得越大，
    //   等值线整体外扩且平滑，没有接缝。
    vec2 diskDelta = delta * (1.0 + 0.75 * exp(-r * r * 1.6));
    diskDelta.y *= 2.0;
    float rDisk = length(diskDelta) / radius;
    float angle = atan(diskDelta.y, diskDelta.x);
    float inner = smoothstep(INNER_IN, INNER_IN + 0.10, rDisk)
                * (1.0 - smoothstep(INNER_OUT - 0.14, INNER_OUT, rDisk));
    float outer = smoothstep(OUTER_IN, OUTER_IN + 0.16, rDisk)
                * (1.0 - smoothstep(OUTER_OUT - 0.30, OUTER_OUT, rDisk));

    float innerTex = bandNoise(angle, rDisk, 2.1);
    float outerTex = bandNoise(angle + 0.9, rDisk, 0.85);

    // 内盘更亮更密（冰蓝），外盘偏暗偏暖（橙），中间留一条窄缝让暗核轮廓更清楚。
    vec3 disk = InnerColor.rgb * inner * (0.65 + 1.00 * innerTex)
              + OuterColor.rgb * outer * (0.42 + 0.78 * outerTex);

    // ---- 光子环与外侧光晕（全部跟着盘的椭圆走）----
    float photonRing = exp(-pow((rDisk - 1.05) * 34.0, 2.0));
    float innerGlow = exp(-pow((rDisk - 1.30) * 3.2, 2.0)) * 0.34;
    float outerGlow = exp(-max(0.0, rDisk - 1.95) * 1.45) * 0.18;
    vec3 halo = InnerColor.rgb * (photonRing * 1.10 + innerGlow)
              + mix(InnerColor.rgb, OuterColor.rgb, 0.72) * outerGlow;

    // ---- 合成：背景先被透镜替换，再叠盘面与光晕 ----
    vec3 col = mix(base, lensed, Alpha * 0.94) + (disk + halo) * Alpha;
    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
