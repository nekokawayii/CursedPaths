package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.*;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL30;

/**
 * 九十九由基 · 黑洞后期（r390）。
 * <p>取景方式沿用本工程已验证的「主 framebuffer 复制 + 全屏后期」路径（同 {@link GojoOpticsClient}）：
 * <ol>
 *   <li>在 {@code AFTER_PARTICLES} 存下世界视图/投影矩阵（{@code AFTER_LEVEL} 传来的不是世界视图栈）；</li>
 *   <li>在 {@code AFTER_LEVEL} 把主 framebuffer 的彩色+深度 blit 到自有 {@link TextureTarget}；</li>
 *   <li>画全屏四边形，由 {@code black_hole} 着色器做视界暗核、背景透镜、冰蓝内盘与橙色外盘。</li>
 * </ol>
 * 检测到外部光影接管画面时会跳过，只保留原有粒子，不会去改写别人的 framebuffer。
 * <p>注意：本类**不**带 {@code @Mod.EventBusSubscriber}，由 {@link ClientBootstrap} 手工注册，
 * 否则会和自动扫描重复注册、导致 tick 每刻走两次（淡入淡出会算错）。
 */
public final class YukiBlackHoleClient {
    /** 冰蓝（内盘 / 视界边缘）。 */
    private static final float[] INNER = { 0.42f, 0.86f, 1.00f };
    /** 橙色（外盘）。 */
    private static final float[] OUTER = { 1.00f, 0.52f, 0.14f };
    /** 淡入/淡出的刻数。 */
    private static final int FADE_IN = 8, FADE_OUT = 16;

    private static final List<Hole> HOLES = new ArrayList<>();
    private static final class Hole {
        Vec3 pos; float radius; long born; long until; int owner;
        Hole(int owner, Vec3 pos, float radius, long born, long until) {
            this.owner = owner; this.pos = pos; this.radius = radius; this.born = born; this.until = until;
        }
    }

    private static ShaderInstance shader;
    private static TextureTarget scene;
    private static boolean warned, externalNoted, loggedDraw, triedLazy;

    /**
     * 兜底：如果 {@code RegisterShadersEvent} 没把着色器交上来（实测在 Oculus/Embeddium 环境里遇到过
     * 事件已注册但回调没跑），就在第一帧要做后期时按需构造一个。构造仍然走原版 ShaderInstance，
     * 失败会打一条明确的日志，不会静默。
     */
    private static void ensureShader() {
        if (shader != null || triedLazy) return;
        triedLazy = true;
        try {
            var mc = Minecraft.getInstance();
            shader = new ShaderInstance(mc.getResourceManager(),
                new ResourceLocation("jujutsu_kaisen_cursed_paths", "black_hole"),
                DefaultVertexFormat.POSITION);
            com.mojang.logging.LogUtils.getLogger().info("[黑洞] 着色器按需加载成功（事件回调未触发）");
        } catch (Throwable t) {
            warned = true;
            com.mojang.logging.LogUtils.getLogger().warn("[黑洞] 着色器按需加载失败：{}", t.toString());
        }
    }
    private static long tick;
    private static Object world;
    private static Matrix4f worldView, worldProjection;

    public static final class Registration {
        @SubscribeEvent public static void shaders(RegisterShadersEvent e) {
            shader = null; warned = false;
            com.mojang.logging.LogUtils.getLogger().info("[黑洞] 开始注册 black_hole 着色器");
            try {
                e.registerShader(new ShaderInstance(e.getResourceProvider(),
                    new ResourceLocation("jujutsu_kaisen_cursed_paths", "black_hole"),
                    DefaultVertexFormat.POSITION), s -> {
                        shader = s; warned = false;
                        com.mojang.logging.LogUtils.getLogger().info("[黑洞] black_hole 着色器已就绪");
                    });
            } catch (java.io.IOException | RuntimeException ex) {
                warned = true;
                com.mojang.logging.LogUtils.getLogger().warn("黑洞后期着色器加载失败，保留粒子版本", ex);
            }
        }
    }

    /** 服务端广播进来的黑洞（kind = {@link YukiBlackHole#FX}）。 */
    public static void receive(Network.Effect e) {
        if (Minecraft.getInstance().level == null) return;
        long until = tick + Math.max(1, e.life());
        // 同一施术者连续施放时替换旧的那个，避免叠出多个互相打架的后期。
        HOLES.removeIf(h -> h.owner == e.owner());
        HOLES.add(new Hole(e.owner(), e.position(), Math.max(0.5f, e.size()), tick, until));
        if (HOLES.size() > 2) HOLES.remove(0);
        com.mojang.logging.LogUtils.getLogger().info("[黑洞] 客户端收到 radius={} life={} shader={}", e.size(), e.life(), shader != null);
        // 直接在动作栏告诉玩家这一发走的是哪条路径，不用翻日志也能判断"竖着的"是不是兜底粒子。
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                shader != null ? "黑洞渲染：着色器" : "黑洞渲染：粒子兜底"), true);
        }
    }

    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent e) {
        if (e.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (world != level) { clear(); world = level; }
        tick++;
        // 自动测试：-Ddsh.blackhole.autotest=1 时，每 60 刻在眼前 10 格自动放一个黑洞，
        // 供 runClient 截屏迭代用（只影响本地这个客户端，不发给服务端）。
        if (AUTOTEST && level != null && mc.player != null && tick % 60 == 20) {
            Vec3 eye = mc.player.getEyePosition();
            Vec3 at = eye.add(mc.player.getLookAngle().scale(10));
            HOLES.removeIf(h -> h.owner == -1);
            HOLES.add(new Hole(-1, at, YukiBlackHole.RADIUS, tick, tick + 200));
            com.mojang.logging.LogUtils.getLogger().info("[黑洞] 自动测试投放 @{}", at);
        }
        HOLES.removeIf(h -> tick > h.until);
    }

    /** runClient 自动测试开关。 */
    private static final boolean AUTOTEST = !System.getProperty("dsh.blackhole.autotest", "").isEmpty();

    public static void clear() {
        HOLES.clear(); tick = 0; warned = false; externalNoted = false; loggedDraw = false; world = null;
        worldView = worldProjection = null;
        if (scene != null) { scene.destroyBuffers(); scene = null; }
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent e) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            worldView = new Matrix4f(e.getPoseStack().last().pose());
            worldProjection = new Matrix4f(e.getProjectionMatrix());
            SkillBlackHole.capture(e);   // r412：skill 世界透镜要这一帧的相机量
            return;
        }
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        ensureShader();
        if (shader == null || warned || HOLES.isEmpty() || worldView == null || worldProjection == null) return;

        var main = mc.getMainRenderTarget();
        if (main.width <= 0 || main.height <= 0 || main.getDepthTextureId() < 0) return;

        Vec3 camera = e.getCamera().getPosition();
        Hole hole = HOLES.stream().filter(h -> h.pos.distanceToSqr(camera) < 128 * 128)
            .min(Comparator.comparingDouble(h -> h.pos.distanceToSqr(camera))).orElse(null);
        if (hole == null) return;

        // r412：优先走 skill 的 world.fsh（照搬），失败/不可用再落到下面自写的世界空间版
        SkillBlackHole.setRadius(hole.radius);
        if (SkillBlackHole.render(hole.pos, fade(hole), (tick + e.getPartialTick()) / 20f)) return;

        Matrix4f vp = new Matrix4f(worldProjection).mul(worldView);
        Vec3 rel = hole.pos.subtract(camera);
        // 世界遮挡：被墙/地形挡住时不画（否则它会像贴在镜头上的一层贴纸）。
        var blockHit = mc.level.clip(new net.minecraft.world.level.ClipContext(camera, hole.pos,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE, mc.player));
        if (blockHit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) return;
        Vector4f projected = new Vector4f((float) rel.x, (float) rel.y, (float) rel.z, 1).mul(vp);
        if (projected.w <= 0.08f) return;   // 黑洞在镜头后面
        float cx = projected.x / projected.w * 0.5f + 0.5f;
        float cy = projected.y / projected.w * 0.5f + 0.5f;
        float screenRadius = Math.abs(worldProjection.m11() * hole.radius / projected.w) * 0.5f;
        if (cx < -1 || cx > 2 || cy < -1 || cy > 2) return;
        if (!loggedDraw) {
            loggedDraw = true;
            com.mojang.logging.LogUtils.getLogger().info("[黑洞] 开始绘制(世界空间) screen=({},{}) r={} alpha={}",
                cx, cy, screenRadius, fade(hole));
        }

        float alpha = fade(hole);
        if (alpha <= 0.001f) return;

        int previousRead = GL30.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDraw = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        if (previousDraw != main.frameBufferId) {
            if (!externalNoted) {
                externalNoted = true;
                com.mojang.logging.LogUtils.getLogger().warn("检测到外部光影画面，跳过黑洞后期（粒子仍保留）");
            }
            return;
        }

        boolean depth = GL30.glIsEnabled(GL30.GL_DEPTH_TEST), blend = GL30.glIsEnabled(GL30.GL_BLEND), cull = GL30.glIsEnabled(GL30.GL_CULL_FACE);
        boolean mask = GL30.glGetBoolean(GL30.GL_DEPTH_WRITEMASK);
        ShaderInstance previousShader = RenderSystem.getShader();
        float[] previousColor = RenderSystem.getShaderColor().clone();
        int[] previousTextures = { RenderSystem.getShaderTexture(0), RenderSystem.getShaderTexture(1), RenderSystem.getShaderTexture(2) };
        try {
            if (scene == null) scene = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
            else if (scene.width != main.width || scene.height != main.height) scene.resize(main.width, main.height, Minecraft.ON_OSX);
            if (main.isStencilEnabled() && !scene.isStencilEnabled()) scene.enableStencil();

            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, scene.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, main.width, main.height, 0, 0, main.width, main.height,
                GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, GL30.GL_NEAREST);
            main.bindWrite(true);

            // ---- 世界空间版 uniform（r405）：不再有屏幕坐标/屏幕半径，全部走 3D ----
            shader.safeGetUniform("ViewProjection").set(vp);
            shader.safeGetUniform("InvViewProjection").set(new Matrix4f(vp).invert());
            shader.safeGetUniform("CamPos").set((float) camera.x, (float) camera.y, (float) camera.z);
            shader.safeGetUniform("HoleCenter").set((float) hole.pos.x, (float) hole.pos.y, (float) hole.pos.z);
            // 盘法线：默认水平盘，绕 X 轴倾斜 20° → 侧面看得到盘的厚度与近远侧
            double tilt = Math.toRadians(20.0);
            shader.safeGetUniform("DiskNormal").set(0f, (float) Math.cos(tilt), (float) Math.sin(tilt));
            float rr = hole.radius;
            shader.safeGetUniform("Params").set(rr, rr * 1.7f, rr * 5.4f, rr * 0.10f);
            shader.safeGetUniform("FrameSize").set((float) main.width, (float) main.height);
            shader.safeGetUniform("Time").set((tick + e.getPartialTick()) / 20f);
            shader.safeGetUniform("Alpha").set(alpha);
            shader.safeGetUniform("InnerColor").set(INNER[0], INNER[1], INNER[2], 1f);
            shader.safeGetUniform("OuterColor").set(OUTER[0], OUTER[1], OUTER[2], 1f);

            RenderSystem.disableDepthTest(); RenderSystem.depthMask(false); RenderSystem.disableCull(); RenderSystem.disableBlend();
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, scene.getColorTextureId());
            RenderSystem.setShaderTexture(1, scene.getDepthTextureId());
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            b.vertex(-1, -1, 0).endVertex(); b.vertex(1, -1, 0).endVertex();
            b.vertex(1, 1, 0).endVertex(); b.vertex(-1, 1, 0).endVertex();
            BufferUploader.drawWithShader(b.end());
        } catch (RuntimeException ex) {
            if (!warned) {
                com.mojang.logging.LogUtils.getLogger().warn("黑洞后期暂停，保留粒子版本", ex);
                warned = true;
            }
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
            RenderSystem.depthMask(mask);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            RenderSystem.setShader(() -> previousShader);
            for (int i = 0; i < 3; i++) RenderSystem.setShaderTexture(i, previousTextures[i]);
            RenderSystem.setShaderColor(previousColor[0], previousColor[1], previousColor[2], previousColor[3]);
        }
    }

    /** 入场淡入、收尾淡出，避免后期突然出现/消失。 */
    private static float fade(Hole hole) {
        long age = tick - hole.born, left = hole.until - tick;
        float in = Math.min(1f, age / (float) FADE_IN);
        float out = Math.min(1f, left / (float) FADE_OUT);
        return Math.max(0f, Math.min(in, out));
    }

    private YukiBlackHoleClient() { }
}
