package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * 照搬 skill（minecraft-black-hole）的世界透镜着色器 r412。
 * <p>着色器本体是原样拷来的 `world.fsh`（73KB），这里只做 Java 侧的「契约填充」——
 * uniform 名、纹理槽位、相机相对坐标、光学眼点都按 skill 的 {@code BlackHoleWorld.render} 抄：
 * <ul>
 *   <li>相机量一律用**相机相对坐标**：`CenterRelative = 中心 - 相机`，`CameraOriginRelative = 光学眼点`（`inverseVP × (0,0,1,0)` 反投影，含 bob/hurt 平移）；</li>
 *   <li>场景颜色/深度副本喂 Sampler0/1，环境副本缺省时 Sampler3/4 也指向同一份（`EnvironmentReady=0` 让着色器走退化路径）；</li>
 *   <li>盘面/光晕/色调全部用固定配方，不再走它的 recipe 系统。</li>
 * </ul>
 * 我们不用它的事务系统，只借它的渲染。
 */
final class SkillBlackHole {
    private SkillBlackHole() { }

    static ShaderInstance world;
    private static TextureTarget snapshot;
    private static Matrix4f frameView, frameProjection;
    private static Vec3 frameCamera;
    private static float framePartial, worldScale = 4.31f;
    private static boolean logged, heard;

    /** 视界半径（格）→ 着色器的 WorldScale（它内部 core 默认 0.58 局部单位）。 */
    static void setRadius(float worldRadius) { worldScale = worldRadius / 0.58f; }

    static void register(RegisterShadersEvent e) {
        try {
            e.registerShader(new ShaderInstance(e.getResourceProvider(),
                new ResourceLocation("jujutsu_kaisen_cursed_paths", "world"),
                DefaultVertexFormat.POSITION_TEX), s -> {
                    world = s;
                    if (snapshot != null) { snapshot.destroyBuffers(); snapshot = null; }
                    com.mojang.logging.LogUtils.getLogger().info("[黑洞] skill 世界透镜着色器已就绪");
                });
        } catch (java.io.IOException | RuntimeException ex) {
            com.mojang.logging.LogUtils.getLogger().warn("[黑洞] skill 世界透镜着色器加载失败", ex);
        }
    }

    /** AFTER_PARTICLES：存这一帧的世界视图/投影/相机。 */
    static void capture(RenderLevelStageEvent e) {
        frameView = new Matrix4f(e.getPoseStack().last().pose());
        frameProjection = new Matrix4f(e.getProjectionMatrix());
        frameCamera = e.getCamera().getPosition();
        framePartial = e.getPartialTick();
    }

    /** AFTER_LEVEL：把场景拷到副本，再画 skill 的世界透镜。返回是否真的画了。 */
    static boolean render(Vec3 center, float alpha, float time) {
        var mc = Minecraft.getInstance();
        var main = mc.getMainRenderTarget();
        if (world == null || frameView == null || frameProjection == null || frameCamera == null) return false;
        if (alpha <= 0.001f) return true;
        if (main.width <= 0 || main.height <= 0 || main.getDepthTextureId() < 0) return false;
        // 外部光影接管时不动别人 framebuffer
        if (GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING) != main.frameBufferId) return false;

        ShaderInstance s = world;
        try {
            if (snapshot == null || snapshot.width != main.width || snapshot.height != main.height
                    || snapshot.isStencilEnabled() != main.isStencilEnabled()) {
                if (snapshot != null) snapshot.destroyBuffers();
                snapshot = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
                if (main.isStencilEnabled()) snapshot.enableStencil();
                snapshot.setFilterMode(GL11.GL_LINEAR);
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, snapshot.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, main.width, main.height, 0, 0, main.width, main.height,
                GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
            main.bindWrite(true);

            Matrix4f vp = new Matrix4f(frameProjection).mul(frameView);
            Matrix4f inverse = new Matrix4f(vp).invert();
            if (!inverse.isFinite()) return false;
            Vec3 origin = eye(inverse);
            Vec3 camera = frameCamera;
            Vec3 rel = center.subtract(camera);

            // ---- 配方（黑模式默认 + 冰蓝内盘 / 橙色外盘）----
            set(s, "uTime", time);
            set(s, "uCore", 0.58f);
            set(s, "uInner", 0.94f);
            set(s, "uOuter", 2.90f);
            set(s, "uThickness", 0.105f);
            set(s, "uTilt", 0f);
            set(s, "uBend", 1.10f);
            set(s, "uDensity", 6f);
            set(s, "uSpeed", 0.55f);
            set(s, "uSeed", 17f);
            set(s, "uInfall", 0.22f);
            set(s, "uSteps", 96);
            set(s, "uExposure", 1.10f);
            set3(s, "uHot", 0.42f, 0.86f, 1.00f);      // 内侧：冰蓝
            set3(s, "uCool", 1.00f, 0.52f, 0.14f);     // 外侧：橙
            set3(s, "uCoreColor", 0.004f, 0.006f, 0.012f);
            set3(s, "uCoreEdgeColor", 0.05f, 0.28f, 0.42f);
            set3(s, "uDiskMid", 0.55f, 0.78f, 0.95f);
            set(s, "uBeaming", 0f);
            set(s, "uDiskGradientPosition", 0.46f);
            set(s, "uCoreGradientStrength", 0.495f);
            set(s, "uBandDetail", 12f);
            set(s, "uDustScale", 4f);
            set(s, "uDustFill", 0.375f);
            set(s, "uFilament", 0f);
            set(s, "uSpiral", 0.32f);
            set(s, "uHaloRadius", 0.70f);
            set(s, "uHaloWidth", 0.010f);
            set(s, "uHaloSharpness", 8f);
            set(s, "uHaloGradientMix", 1f);
            set(s, "uHaloDiskGradientMix", 1f);
            set(s, "uHaloAuraGradientMix", 0f);
            set(s, "JetStrength", 0f);
            set(s, "JetLength", 5.5f);
            set(s, "JetWidth", 0.24f);
            set(s, "uShellStrength", 0f);
            set(s, "uShellRadius", 5f);
            set(s, "uShellWidth", 0.12f);
            set(s, "uCoreGlow", 0f);
            set(s, "uCoreGlowRadius", 1.8f);
            set(s, "uCoreEmission", 1f);
            set(s, "uObjectMode", 0f);
            set(s, "uGranular", 0);                    // 0 = 默认盘
            set3(s, "uHaloGradientStart", 0.0f, 0.0f, 0.0f);
            set3(s, "uHaloGradientEnd", 0.0f, 0.0f, 0.0f);
            set3(s, "uHaloDiskInnerColor", 0.42f, 0.86f, 1.00f);
            set3(s, "uHaloDiskOuterColor", 1.00f, 0.52f, 0.14f);
            set3(s, "uHaloAuraInnerColor", 0.42f, 0.86f, 1.00f);
            set3(s, "uHaloAuraOuterColor", 1.00f, 0.52f, 0.14f);
            set3(s, "uJetBaseColor", 0.42f, 0.86f, 1.00f);
            set3(s, "uJetTipColor", 1.00f, 0.52f, 0.14f);
            set3(s, "uShellColor", 0.42f, 0.86f, 1.00f);
            set(s, "uOrbitPhase", 0f);
            set(s, "uRadialPhase", 0f);
            set(s, "uClassicRadialPhase", 0f);
            set(s, "uShearLocal", 0f);
            set(s, "uShearParity", 0f);
            set(s, "uDustDriftA", 0f);
            set(s, "uDustDriftB", 0f);
            set(s, "uJetClock", 0f);
            set(s, "uShellClock", 0f);

            set(s, "ViewProjection", vp);
            set(s, "InvViewProjection", inverse);
            set3(s, "CenterRelative", (float) rel.x, (float) rel.y, (float) rel.z);
            set3(s, "CameraOriginRelative", (float) origin.x, (float) origin.y, (float) origin.z);
            set(s, "WorldScale", worldScale);
            set(s, "LensStrength", 0f);          // 0 = 不重投影背景（我们这套环境副本不可靠）
            set(s, "LensProbeMode", 0f);
            set(s, "LensRadius", 2.4f);
            set(s, "EnvironmentReady", 0f);
            set(s, "ForegroundCount", 0);
            set(s, "HaloStrength", 1f);
            set(s, "HaloRing", 1f);
            set(s, "HaloDisk", 1f);
            set(s, "HaloAura", 1f);

            // 贴图槽：0/1 场景副本，2/3/4 同样回落到副本（前景与环境都不用）
            var noise = mc.getTextureManager().getTexture(
                new ResourceLocation("jujutsu_kaisen_cursed_paths", "textures/effect/ribbon-noise.png"));
            noise.setFilter(true, false);
            s.setSampler("RibbonNoise", noise.getId());

            RenderSystem.setShader(() -> s);
            RenderSystem.setShaderTexture(0, snapshot.getColorTextureId());
            RenderSystem.setShaderTexture(1, snapshot.getDepthTextureId());
            RenderSystem.setShaderTexture(2, snapshot.getColorTextureId());
            RenderSystem.setShaderTexture(3, snapshot.getColorTextureId());
            RenderSystem.setShaderTexture(4, snapshot.getDepthTextureId());
            RenderSystem.disableDepthTest(); RenderSystem.depthMask(false);
            RenderSystem.disableBlend(); RenderSystem.disableCull();
            RenderSystem.colorMask(true, true, true, true);

            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            b.vertex(-1, -1, 0).uv(0, 0).endVertex();
            b.vertex(1, -1, 0).uv(1, 0).endVertex();
            b.vertex(1, 1, 0).uv(1, 1).endVertex();
            b.vertex(-1, 1, 0).uv(0, 1).endVertex();
            BufferUploader.drawWithShader(b.end());

            if (!logged) {
                logged = true;
                com.mojang.logging.LogUtils.getLogger().info(
                    "[黑洞] skill 世界透镜绘制 centerRel=({},{},{}) scale={}", rel.x, rel.y, rel.z, worldScale);
            }
            return true;
        } catch (RuntimeException ex) {
            if (!heard) {
                heard = true;
                com.mojang.logging.LogUtils.getLogger().warn("[黑洞] skill 世界透镜绘制失败", ex);
            }
            return false;
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            frameView = null; frameProjection = null; frameCamera = null;
        }
    }

    /** 透视眼点：inverseVP × (0,0,1,0)，含 bob/hurt 平移（skill 的做法）。 */
    private static Vec3 eye(Matrix4f inverse) {
        Vector4f p = new Vector4f(0, 0, 1, 0).mul(inverse);
        if (!Float.isFinite(p.w) || Math.abs(p.w) < 1e-8f) return Vec3.ZERO;
        return new Vec3(p.x / p.w, p.y / p.w, p.z / p.w);
    }

    private static void set(ShaderInstance s, String name, float v) {
        var u = s.getUniform(name);
        if (u != null) u.set(v);
    }

    private static void set(ShaderInstance s, String name, int v) {
        var u = s.getUniform(name);
        if (u != null) u.set(v);
    }

    private static void set(ShaderInstance s, String name, Matrix4f m) {
        var u = s.getUniform(name);
        if (u != null) u.set(m);
    }

    private static void set3(ShaderInstance s, String name, float x, float y, float z) {
        var u = s.getUniform(name);
        if (u != null) u.set(x, y, z);
    }
}
