package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import character.kurisu.KurisuSkills;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

/**
 * 邪去悔天梯的自绘层（Photon 画不了的部分）：
 * <ul>
 *   <li><b>长按咏唱</b>：背后展开的巨大天使羽翼虚影 + 金色包裹光晕（羽片用 feather.png 逐片摆成扇形，
 *       永远朝向相机，所以从任何角度都像"浮在身后的巨大虚影"）；</li>
 *   <li><b>松手圣光柱</b>：贴地的巨大金色法阵（sigil.png，双层反向旋转）+ 天空裂口 + 柱脚光晕。</li>
 * </ul>
 * 全部是加法混合的相机面片，不写深度；状态由 {@code KurisuFx.WINGS/PILLAR} 包驱动。
 */
public final class KurisuHornRender {
    private static final String NS="jujutsu_kaisen_cursed_paths";
    private static final ResourceLocation FEATHER=new ResourceLocation(NS,"textures/effect/feather.png");
    private static final ResourceLocation SIGIL=new ResourceLocation(NS,"textures/effect/sigil.png");
    private static final ResourceLocation GLOW=new ResourceLocation(NS,"textures/effect/glow.png");
    private static final ResourceLocation STREAK=new ResourceLocation(NS,"textures/effect/streak.png");
    private static final int GOLD=0xFFD77A, WHITE=0xFFF6D8, AMBER=0xFFE9A8;

    /** 羽翼：owner + 生效区间。 */
    private static int wingOwner=-1;
    private static long wingStart,wingEnd;
    /** 圣光柱：落点 + 生效区间。 */
    private static Vec3 sigilPos;
    private static long sigilStart,sigilEnd;
    /** 短按光柱：起点/方向 + 生效区间（每 8 刻由服务端续期一次）。 */
    private static Vec3 beamFrom;
    private static Vec3 beamDir;
    private static long beamEnd;

    /** 长按咏唱：羽翼与金光（每 8 刻由服务端续期一次，客户端不重建）。 */
    public static void channel(int owner,int life){
        long now=now();
        if(wingOwner!=owner||now>wingEnd)wingStart=now;
        wingOwner=owner;wingEnd=now+Math.max(6,life);
    }
    /** 松手圣光柱：地面法阵 + 天空裂口 + 通天柱体。 */
    public static void ritual(Vec3 pos,int life){
        long now=now();
        sigilPos=pos;sigilStart=now;sigilEnd=now+Math.max(20,life);
    }
    /** 短按光柱：柱体（自绘圆柱）+ 飘飞符文由 Photon 负责。 */
    public static void tapBeam(Vec3 from,Vec3 dir,int life){
        beamFrom=from;beamDir=dir==null?Vec3.ZERO:dir;
        beamEnd=now()+Math.max(6,life);
    }
    public static void clearFor(int skill){
        if(skill==KurisuSkills.HORN)clear();
    }
    public static void clear(){wingOwner=-1;wingEnd=0;sigilPos=null;sigilEnd=0;beamFrom=null;beamEnd=0;}

    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)return;
        var mc=Minecraft.getInstance();
        if(mc.level==null)return;
        long now=now();
        boolean wings=wingOwner>0&&now<wingEnd;
        boolean sigil=sigilPos!=null&&now<sigilEnd;
        boolean beam=beamFrom!=null&&now<beamEnd;
        if(!wings&&!sigil&&!beam)return;
        Vec3 cam=e.getCamera().getPosition();
        PoseStack ps=e.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x,-cam.y,-cam.z);
        Matrix4f m=ps.last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1,1,1,1);
        // 加法混合：金色光效在昼夜/光影下都不会变灰
        RenderSystem.blendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA,org.lwjgl.opengl.GL11.GL_ONE);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

        if(sigil)drawSigil(m,sigilPos,sigilStart,sigilEnd,now,cam);
        if(beam)drawTapBeam(m,now);
        if(wings)drawWings(m,wingOwner,wingStart,wingEnd,now,cam,e.getPartialTick());

        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ps.popPose();
    }

    // ---------------------------------------------------------------- 柱体（自绘圆柱，才撑得住"通天彻地"）
    /** 短按：一道沿准星、持续存在的金色光柱（半径 0.75 + 外晕 1.35）。 */
    private static void drawTapBeam(Matrix4f m,long now){
        float left=(beamEnd-now)/20f;
        float fade=Math.min(1,left/0.6f);
        Vec3 end=beamFrom.add(beamDir.scale(28));
        // 三层同心圆柱：外晕 + 亮芯 + 细芯
        shaft(m,beamFrom,end,1.25,AMBER,26,20,now,fade*0.85f);
        shaft(m,beamFrom,end,0.62,GOLD,52,38,now,fade);
        shaft(m,beamFrom,end,0.26,WHITE,110,70,now,fade);
        // 落点爆点
        RenderSystem.setShaderTexture(0,GLOW);
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
        billboard(b,m,end,2.6f,(int)(140*fade)<<24|(WHITE),beamFrom.add(0,0,0));
        BufferUploader.drawWithShader(b.end());
    }
    /**
     * 圆柱光柱（**纯色顶点色**，不贴图）：{@code seg} 段圆周 × {@code bands} 段高度，顶端渐隐。
     * 贴图版会把整张 soft 贴图压进每一小段，远看全是竖条纹；纯色加法混合反而更像"光"。
     * 叠 3~4 层不同半径就是"亮芯 + 外晕"的体积感。
     */
    private static void shaft(Matrix4f m,Vec3 from,Vec3 to,double radius,int rgb,int alphaBase,int alphaTop,
                              long now,float fade){
        Vec3 axis=to.subtract(from);double len=axis.length();
        if(len<1)return;
        Vec3 dir=axis.normalize();
        Vec3 u=new Vec3(0,1,0).cross(dir);
        if(u.lengthSqr()<1e-4)u=new Vec3(1,0,0);
        u=u.normalize();
        Vec3 v=dir.cross(u).normalize();
        int seg=32,bands=10;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        // 缓慢上升的亮带：让柱体看着在流动
        double flow=(now*0.03)%1.0;
        for(int band=0;band<bands;band++){
            double t0=band/(double)bands,t1=(band+1)/(double)bands;
            float bandPulse=(float)(0.78+0.22*Math.sin((t0*bands*0.9-flow*Math.PI*2)));
            float a0=(float)(alphaBase+(alphaTop-alphaBase)*t0)*fade*bandPulse;
            float a1=(float)(alphaBase+(alphaTop-alphaBase)*t1)*fade*bandPulse;
            int c0=(Math.min(255,(int)a0)<<24)|rgb, c1=(Math.min(255,(int)a1)<<24)|rgb;
            double y0=t0*len,y1=t1*len;
            for(int i=0;i<seg;i++){
                double a=2*Math.PI*i/seg, c=2*Math.PI*(i+1)/seg;
                Vec3 p00=from.add(dir.scale(y0)).add(u.scale(Math.cos(a)*radius)).add(v.scale(Math.sin(a)*radius));
                Vec3 p10=from.add(dir.scale(y0)).add(u.scale(Math.cos(c)*radius)).add(v.scale(Math.sin(c)*radius));
                Vec3 p11=from.add(dir.scale(y1)).add(u.scale(Math.cos(c)*radius)).add(v.scale(Math.sin(c)*radius));
                Vec3 p01=from.add(dir.scale(y1)).add(u.scale(Math.cos(a)*radius)).add(v.scale(Math.sin(a)*radius));
                b.vertex(m,(float)p00.x,(float)p00.y,(float)p00.z).color(c0).endVertex();
                b.vertex(m,(float)p10.x,(float)p10.y,(float)p10.z).color(c0).endVertex();
                b.vertex(m,(float)p11.x,(float)p11.y,(float)p11.z).color(c1).endVertex();
                b.vertex(m,(float)p01.x,(float)p01.y,(float)p01.z).color(c1).endVertex();
            }
        }
        BufferUploader.drawWithShader(b.end());
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
    }

    // ---------------------------------------------------------------- 地面金色法阵 + 天空裂口
    private static void drawSigil(Matrix4f m,Vec3 c,long start,long end,long now,Vec3 cam){
        float age=(now-start)/20f, left=(end-now)/20f;
        float fade=Math.min(1,Math.min(age/0.5f,left/1.2f));
        if(fade<=0.01f)return;
        // ① 通天柱体：四层同心圆柱（外晕 → 亮芯），纯色加法混合
        shaft(m,c.add(0,-1.5,0),c.add(0,118,0),5.2,AMBER,13,5,now,fade*0.9f);
        shaft(m,c.add(0,-1.5,0),c.add(0,118,0),4.4,GOLD,34,11,now,fade*0.95f);
        shaft(m,c.add(0,-1.5,0),c.add(0,118,0),2.4,WHITE,52,16,now,fade);
        shaft(m,c.add(0,-2.0,0),c.add(0,120,0),0.9,WHITE,96,26,now,fade);
        // ② 两层贴地法阵，反向旋转
        RenderSystem.setShaderTexture(0,SIGIL);
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
        float spin=now*0.018f;
        disc(b,m,c.add(0,.055,0),11.5f,spin,(int)(150*fade)<<24|(AMBER));
        disc(b,m,c.add(0,.075,0),7.4f,-spin*1.6f,(int)(110*fade)<<24|(GOLD));
        disc(b,m,c.add(0,.09,0),3.6f,spin*2.4f,(int)(90*fade)<<24|(WHITE));
        BufferUploader.drawWithShader(b.end());
        // ② 柱脚光晕：贴地的一片亮光，接住从天上砸下来的光柱
        RenderSystem.setShaderTexture(0,GLOW);
        b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
        disc(b,m,c.add(0,.12,0),6.5f,0f,(int)(60*fade)<<24|(WHITE));
        BufferUploader.drawWithShader(b.end());
        // ③ 天空裂口：前 30 刻内从天上劈下来的锯齿亮缝（加法混合，朝向相机）
        float crack=Math.min(1,Math.max(0,(end-now-60)/20f));
        if(crack>0.01f){
            RenderSystem.setShaderTexture(0,STREAK);
            b=Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
            Vec3 top=c.add(0,96,0);
            Vec3 view=cam.subtract(c).multiply(1,0,1);
            if(view.lengthSqr()<1e-4)view=new Vec3(0,0,1);
            view=view.normalize();
            Vec3 right=new Vec3(0,1,0).cross(view).normalize();
            for(int i=0;i<5;i++){
                double y0=64+i*7,y1=y0+7.5;
                double off=Math.sin(i*2.3)*(i%2==0?1.6:-1.9);
                double w=2.2-i*0.18;
                Vec3 a=c.add(right.x*off,y0,c.z+right.z*off);
                Vec3 bb=c.add(right.x*(off+w),y0+1.5,c.z+right.z*(off+w));
                Vec3 cc=c.add(right.x*(off+w*0.4),y1,c.z+right.z*(off+w*0.4));
                Vec3 dd=c.add(right.x*(off-w*0.4),y1,c.z+right.z*(off-w*0.4));
                int col=((int)(210*crack)<<24)|(i%2==0?WHITE:AMBER);
                tex(b,m,a,0,1,col);tex(b,m,bb,1,1,col);tex(b,m,cc,1,0,col);tex(b,m,dd,0,0,col);
            }
            BufferUploader.drawWithShader(b.end());
            // 裂口尽头的一团天光
            RenderSystem.setShaderTexture(0,GLOW);
            b=Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
            billboard(b,m,top,9.5f,(int)(150*crack)<<24|(WHITE),cam);
            BufferUploader.drawWithShader(b.end());
        }
    }

    // ---------------------------------------------------------------- 巨大天使羽翼虚影
    private static void drawWings(Matrix4f m,int ownerId,long start,long end,long now,Vec3 cam,float partial){
        Entity owner=Minecraft.getInstance().level==null?null:Minecraft.getInstance().level.getEntity(ownerId);
        if(owner==null)return;
        float age=(now-start)/20f, left=(end-now)/20f;
        float fade=Math.min(1,Math.min(age/0.35f,left/0.6f));
        if(fade<=0.01f)return;
        float pulse=0.86f+0.14f*(float)Math.sin(now*0.22);
        Vec3 base=owner.getPosition(partial).add(0,1.05,0);
        Vec3 view=cam.subtract(base);
        if(view.lengthSqr()<1e-4)return;
        view=view.normalize();
        Vec3 right=new Vec3(0,1,0).cross(view);
        if(right.lengthSqr()<1e-4)right=new Vec3(1,0,0);
        right=right.normalize();
        Vec3 up=view.cross(right).normalize();
        // ① 光环：身后一团金色柔光，把角色包住
        RenderSystem.setShaderTexture(0,GLOW);
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
        billboard(b,m,base,3.6f,(int)(28*fade*pulse)<<24|(AMBER),cam);
        BufferUploader.drawWithShader(b.end());
        // ② 羽片：每侧两层——主飞羽向外散开、覆羽填满肩部，才像"翅膀"而不是一圈放射光
        RenderSystem.setShaderTexture(0,FEATHER);
        b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
        for(int side=-1;side<=1;side+=2){
            // 覆羽（短、靠上、亮）
            for(int i=0;i<5;i++){
                float t=i/4f;
                double ang=Math.toRadians(20+42*t);
                Vec3 dir=up.scale(Math.cos(ang)).add(right.scale(side*Math.sin(ang))).normalize();
                double len=1.05+1.45*t, wid=0.32+0.16*t;
                Vec3 baseP=base.add(right.scale(side*(0.05+0.95*t)));
                int col=(Math.min(255,(int)((48+38*t)*fade*pulse))<<24)|(AMBER);
                featherQuad(b,m,view,baseP,dir,len,wid,col);
            }
            // 主飞羽（长、外移、尖端更亮）
            for(int i=0;i<8;i++){
                float t=i/7f;
                double ang=Math.toRadians(38+62*t);
                Vec3 dir=up.scale(Math.cos(ang)).add(right.scale(side*Math.sin(ang))).normalize();
                double len=2.0+2.9*t, wid=0.40+0.24*t;
                Vec3 baseP=base.add(right.scale(side*(0.10+1.60*t))).add(up.scale(0.40*t));
                int col=(Math.min(255,(int)((34+72*t)*fade*pulse))<<24)|(t>0.82f?WHITE:GOLD);
                featherQuad(b,m,view,baseP,dir,len,wid,col);
            }
        }        BufferUploader.drawWithShader(b.end());
    }

    // ---------------------------------------------------------------- 基础图元
    /** 贴地圆盘：{@code spin} 是旋转弧度，UV 铺满整张 sigil。 */
    private static void disc(BufferBuilder b,Matrix4f m,Vec3 c,float r,float spin,int color){
        double cos=Math.cos(spin),sin=Math.sin(spin);
        float[][] uv={{0,1},{1,1},{1,0},{0,0}};
        double[][] q={{-r,-r},{r,-r},{r,r},{-r,r}};
        for(int i=0;i<4;i++){
            double x=q[i][0],z=q[i][1];
            double rx=x*cos-z*sin,rz=x*sin+z*cos;
            b.vertex(m,(float)(c.x+rx),(float)c.y,(float)(c.z+rz)).uv(uv[i][0],uv[i][1]).color(color).endVertex();
        }
    }
    /** 朝向相机的正方形面片（光晕/天光）。 */
    private static void billboard(BufferBuilder b,Matrix4f m,Vec3 c,float size,int color,Vec3 cam){
        Vec3 view=cam.subtract(c);
        if(view.lengthSqr()<1e-4)view=new Vec3(0,0,1);
        view=view.normalize();
        Vec3 right=new Vec3(0,1,0).cross(view).normalize();
        Vec3 up=view.cross(right).normalize();
        float h=size*0.5f;
        Vec3[] pts={
            c.add(right.scale(-h)).add(up.scale(-h)),c.add(right.scale(h)).add(up.scale(-h)),
            c.add(right.scale(h)).add(up.scale(h)),c.add(right.scale(-h)).add(up.scale(h))};
        float[][] uv={{0,1},{1,1},{1,0},{0,0}};
        for(int i=0;i<4;i++){
            Vec3 p=pts[i];
            b.vertex(m,(float)p.x,(float)p.y,(float)p.z).uv(uv[i][0],uv[i][1]).color(color).endVertex();
        }
    }
    private static void tex(BufferBuilder b,Matrix4f m,Vec3 p,float u,float v,int color){
        b.vertex(m,(float)p.x,(float)p.y,(float)p.z).uv(u,v).color(color).endVertex();
    }
    /** 一根羽片：基部在 {@code base}，沿 {@code dir} 长 {@code len}、半宽 {@code wid}，贴图尖端朝外。 */
    private static void featherQuad(BufferBuilder b,Matrix4f m,Vec3 view,Vec3 base,Vec3 dir,double len,double wid,int color){
        Vec3 perp=view.cross(dir);
        if(perp.lengthSqr()<1e-6)perp=new Vec3(1,0,0);
        perp=perp.normalize();
        Vec3 inner=base, outer=base.add(dir.scale(len));
        Vec3 a0=inner.subtract(perp.scale(wid)),a1=inner.add(perp.scale(wid));
        Vec3 a2=outer.add(perp.scale(wid)),a3=outer.subtract(perp.scale(wid));
        tex(b,m,a0,0,1,color);tex(b,m,a1,1,1,color);tex(b,m,a2,1,0,color);tex(b,m,a3,0,0,color);
    }
    private static long now(){
        var l=Minecraft.getInstance().level;
        return l==null?0:l.getGameTime();
    }
    private KurisuHornRender(){}
}
