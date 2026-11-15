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
    private static boolean warned, externalNoted;
    private static long tick;
    private static Object world;
    private static Matrix4f worldView, worldProjection;

    public static final class Registration {
        @SubscribeEvent public static void shaders(RegisterShadersEvent e) {
            shader = null; warned = false;
            try {
                e.registerShader(new ShaderInstance(e.getResourceProvider(),
                    new ResourceLocation("jujutsu_kaisen_cursed_paths", "black_hole"),
                    DefaultVertexFormat.POSITION), s -> { shader = s; warned = false; });
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
    }

    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent e) {
        if (e.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        var level = Minecraft.getInstance().level;
        if (world != level) { clear(); world = level; }
        tick++;
        HOLES.removeIf(h -> tick > h.until);
    }

    public static void clear() {
        HOLES.clear(); tick = 0; warned = false; externalNoted = false; world = null;
        worldView = worldProjection = null;
        if (scene != null) { scene.destroyBuffers(); scene = null; }
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent e) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            worldView = new Matrix4f(e.getPoseStack().last().pose());
            worldProjection = new Matrix4f(e.getProjectionMatrix());
            return;
        }
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (shader == null || warned || HOLES.isEmpty() || worldView == null || worldProjection == null) return;

        var main = mc.getMainRenderTarget();
        if (main.width <= 0 || main.height <= 0 || main.getDepthTextureId() < 0) return;

        Vec3 camera = e.getCamera().getPosition();
        Hole hole = HOLES.stream().filter(h -> h.pos.distanceToSqr(camera) < 128 * 128)
            .min(Comparator.comparingDouble(h -> h.pos.distanceToSqr(camera))).orElse(null);
        if (hole == null) return;

        Matrix4f vp = new Matrix4f(worldProjection).mul(worldView);
        Vec3 rel = hole.pos.subtract(camera);
        Vector4f projected = new Vector4f((float) rel.x, (float) rel.y, (float) rel.z, 1).mul(vp);
        if (projected.w <= 0.08f) return;   // 黑洞在镜头后面
        float cx = projected.x / projected.w * 0.5f + 0.5f;
        float cy = projected.y / projected.w * 0.5f + 0.5f;
        float screenRadius = Math.abs(worldProjection.m11() * hole.radius / projected.w) * 0.5f;
        screenRadius = Math.max(0.02f, Math.min(0.85f, screenRadius));
        if (cx < -1 || cx > 2 || cy < -1 || cy > 2) return;

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

            shader.safeGetUniform("FrameSize").set((float) main.width, (float) main.height);
            shader.safeGetUniform("Time").set((tick + e.getPartialTick()) / 20f);
            shader.safeGetUniform("Alpha").set(alpha);
            shader.safeGetUniform("Lens").set(cx, cy, screenRadius, 1.0f);
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
