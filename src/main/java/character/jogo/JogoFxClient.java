package character.jogo;

import cn.blockforge.generated.nnnnr1201forge2.Client;
import cn.blockforge.generated.nnnnr1201forge2.Network;
import cn.blockforge.generated.nnnnr1201forge2.PhotonFx;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 漏壶的 PhotonFx 表现层。
 * 极之番·陨：一颗 60 格半径的熔岩球缓慢下坠（贴图球面 + 尾焰），玩家被托在球顶漂浮观看；落地后是蘑菇云。
 */
@Mod.EventBusSubscriber(modid="jujutsu_kaisen_cursed_paths",value=Dist.CLIENT)
public final class JogoFxClient {
    private static final String GLOW="textures/effect/glow.png";
    private static final int FIRE=0xFFFF7A18, FIRE_BLOOM=0xFFFF3A00, LAVA=0xFFFFB024, EMBER=0xFFFFD08A, ASH=0xFF2A1A18;
    /** 用户提供的岩浆块面（陨石本体贴图）。 */
    private static final ResourceLocation ROCK_TEX=new ResourceLocation("jujutsu_kaisen_cursed_paths","textures/effect/jogo_meteor_rock.png");
    /** ComfyUI 生成的黑红尾焰（黑底已转透明）。 */
    private static final ResourceLocation TRAIL_TEX=new ResourceLocation("jujutsu_kaisen_cursed_paths","textures/effect/jogo_meteor_trail.png");
    public record Domain(int ownerId,Vec3 center,double radius,long end) {}
    /** 陨石下坠：起点球心 / 终点球心 / 半径 / 起止刻。 */
    private record Ride(Vec3 start,Vec3 end,double radius,long startAt,long landAt) {}
    /** 落地后的蘑菇云：中心 + 半径 + 起止刻。 */
    private record Bloom(Vec3 at,double radius,long start,long end) {}
    private static final List<Domain> DOMAINS=new ArrayList<>();
    private static final List<Ride> RIDES=new ArrayList<>();
    private static final List<Bloom> BLOOMS=new ArrayList<>();
    public static List<Domain> domains(){return DOMAINS;}
    private static final Random RANDOM=new Random();

    public static void receive(Network.Effect e) {
        var mc=Minecraft.getInstance();
        long now=mc.level==null?0:mc.level.getGameTime();
        Vec3 pos=e.position();
        switch(e.kind()) {
            case JogoFx.BUG -> PhotonFx.spark(pos,0.55,0.42,FIRE,FIRE_BLOOM,GLOW,true,1,6,26,0.3);
            case JogoFx.BUG_BURST -> {
                PhotonFx.spark(pos,2.6,1.6,LAVA,FIRE_BLOOM,GLOW,true,2,8,44,0.8);
                PhotonFx.ring(pos,3.0,0.9,FIRE,FIRE_BLOOM,26,2,8);
                PhotonFx.shockwave(pos,FIRE_BLOOM,3.0,10);
            }
            case JogoFx.METEOR_TRAIL -> {
                PhotonFx.spark(pos,1.5,1.0,FIRE,FIRE_BLOOM,GLOW,true,2,7,30,0.5);
                PhotonFx.spark(pos.add(0,1.2,0),0.8,0.5,ASH,0,GLOW,false,3,12,14,0.2);
            }
            case JogoFx.METEOR_IMPACT -> {
                PhotonFx.spark(pos,5.0,3.0,LAVA,FIRE_BLOOM,GLOW,true,3,10,60,1.0);
                PhotonFx.ring(pos,5.2,1.4,FIRE,FIRE_BLOOM,34,3,10);
                PhotonFx.shockwave(pos,FIRE_BLOOM,5.0,14);
                for(int i=0;i<10;i++){
                    double a=RANDOM.nextDouble()*Math.PI*2;
                    PhotonFx.spark(pos.add(Math.cos(a)*RANDOM.nextDouble()*3.5,0.6,Math.sin(a)*RANDOM.nextDouble()*3.5),1.1,0.7,EMBER,0,GLOW,true,2,12,14,0.3);
                }
            }
            case JogoFx.PILLAR -> {
                if(e.direction().lengthSqr()<1e-4){
                    Vec3 top=pos.add(0,10,0);
                    PhotonFx.beam(pos,top,1.15,10);
                    PhotonFx.beam(pos,top,2.0,6);
                    for(int i=0;i<5;i++){
                        double h=i*2.0;
                        PhotonFx.spark(pos.add(0,h,0),1.5+i*0.15,1.1,LAVA,FIRE_BLOOM,GLOW,true,3,9,28,0.35);
                        PhotonFx.ring(pos.add(0,h,0),1.3+i*0.12,0.35,FIRE,FIRE_BLOOM,12,4,4);
                    }
                    PhotonFx.spark(pos.add(0,10,0),2.2,1.4,FIRE,FIRE_BLOOM,GLOW,true,4,12,26,0.5);
                }else{
                    Vec3 dir=e.direction().normalize();
                    Vec3 end=pos.add(dir.scale(1.6));
                    PhotonFx.beam(pos,end,0.55,5);
                    PhotonFx.spark(pos,1.2,0.8,LAVA,FIRE_BLOOM,GLOW,true,2,7,24,0.4);
                }
            }
            case JogoFx.DOMAIN -> {
                double radius=Math.max(6,e.size());
                int life=Math.max(40,e.life());
                DOMAINS.removeIf(d->d.ownerId()==e.owner());
                DOMAINS.add(new Domain(e.owner(),pos,radius,now+life));
            }
            case JogoFx.DOMAIN_END -> DOMAINS.removeIf(d->d.ownerId()==e.owner());
            case JogoFx.JIYAN_RIDE -> {
                double radius=Math.max(8,e.size());
                long life=Math.max(20,e.life());
                Vec3 impact=e.direction().lengthSqr()<1e-4?pos:e.direction();
                RIDES.clear();
                RIDES.add(new Ride(pos,impact.add(0,radius*0.4,0),radius,now,now+life));
                PhotonFx.spark(pos,radius*0.9,radius*0.5,LAVA,FIRE_BLOOM,GLOW,true,6,24,180,1.6);
                PhotonFx.ring(impact,radius*0.55,2.0,FIRE_BLOOM,FIRE_BLOOM,40,4,20);
                Client.shake(10);
            }
            case JogoFx.JIYAN_IMPACT -> {
                double radius=Math.max(8,e.size());
                RIDES.clear();
                BLOOMS.add(new Bloom(pos,radius,now,now+90));
                PhotonFx.spark(pos,radius*0.5,radius*0.32,LAVA,FIRE_BLOOM,GLOW,true,6,24,260,2.4);
                PhotonFx.shockwave(pos,0xFFFF7A18,(int)(radius*1.9),30);
                for(int i=0;i<10;i++){
                    double h=i*radius*0.10;
                    PhotonFx.ring(pos.add(0,h,0),radius*0.20+i*radius*0.10,2.2,FIRE,FIRE_BLOOM,34,3,12);
                }
                PhotonFx.beam(pos,pos.add(0,radius*0.9,0),radius*0.22,50);
                Client.shake(40);
                Client.flash(10);
            }
            case JogoFx.JIYAN_SEA -> {
                PhotonFx.spark(pos,Math.max(4,e.size()),3.0,LAVA,FIRE_BLOOM,GLOW,true,3,18,120,0.9);
                PhotonFx.ring(pos,Math.max(4,e.size()),0.8,FIRE,FIRE_BLOOM,34,3,20);
            }
            default -> {}
        }
    }

    /** 每刻：下坠中的陨石持续冒火、落地后的蘑菇云持续升起。 */
    public static void tick() {
        var mc=Minecraft.getInstance();
        if(mc.level==null)return;
        long now=mc.level.getGameTime();
        DOMAINS.removeIf(d->now>=d.end());
        RIDES.removeIf(r->now>=r.landAt());
        BLOOMS.removeIf(b->now>=b.end());
        for(Ride r:RIDES){
            Vec3 c=rideCenter(r,now);
            PhotonFx.spark(c.add(0,r.radius()*0.55,0),r.radius()*0.55,r.radius()*0.30,FIRE,FIRE_BLOOM,GLOW,true,3,10,90,1.0);
            PhotonFx.spark(c.add(0,-r.radius()*0.9,0),r.radius()*0.4,r.radius()*0.28,ASH,0,GLOW,false,4,12,30,0.5);
        }
        for(Bloom bl:BLOOMS){
            double t=(now-bl.start())/(double)Math.max(1,bl.end()-bl.start());
            if(now%4==0)PhotonFx.ring(bl.at().add(0,2+t*bl.radius()*0.8,0),bl.radius()*0.2+t*bl.radius()*0.9,2.4,FIRE,FIRE_BLOOM,30,3,10);
            if(now%6==0)PhotonFx.spark(bl.at().add(0,4+t*bl.radius()*0.7,0),bl.radius()*0.3+t*bl.radius()*0.3,4.0,LAVA,FIRE_BLOOM,GLOW,true,4,12,50,0.8);
        }
    }

    private static Vec3 rideCenter(Ride r,long now){
        double t=Math.min(1,Math.max(0,(now-r.startAt())/(double)Math.max(1,r.landAt()-r.startAt())));
        return r.start().lerp(r.end(),t);
    }

    // ---------------------------------------------------------------- 自绘：暗红天空 / 熔岩球 / 蘑菇云
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null)return;
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)return;
        long now=mc.level.getGameTime();
        if(RIDES.isEmpty()&&BLOOMS.isEmpty())return;
        Vec3 cam=e.getCamera().getPosition();
        PoseStack ps=e.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x,-cam.y,-cam.z);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1,1,1,1);
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        if(!RIDES.isEmpty())sky(b,ps,cam,now);
        for(Bloom bl:BLOOMS)cloud(b,ps,bl,now);
        BufferUploader.drawWithShader(b.end());
        if(!RIDES.isEmpty())meteorQuads(ps,cam,now);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        ps.popPose();
    }

    /** 天空变暗红：头顶一块巨大的暗红穹顶（深度测试开着，会被地形挡住，只盖住天空）。 */
    private static void sky(BufferBuilder b,PoseStack ps,Vec3 cam,long now){
        double y=cam.y+150,r=420;
        float pulse=(float)(0.82+0.18*Math.sin(now*0.06));
        int base=((int)(205*pulse)<<24)|0x2A0000;
        quad(b,ps,new Vec3(cam.x-r,y,cam.z-r),new Vec3(cam.x+r,y,cam.z-r),new Vec3(cam.x+r,y,cam.z+r),new Vec3(cam.x-r,y,cam.z+r),base);
        quad(b,ps,new Vec3(cam.x-r,y-10,cam.z-r),new Vec3(cam.x+r,y-10,cam.z-r),new Vec3(cam.x+r,y-10,cam.z+r),new Vec3(cam.x-r,y-10,cam.z+r),0x40FF2A00);
    }

    /** 熔岩球：经纬球面贴用户给的岩浆图（u 方向平铺 3 次），外加一层暗红轮廓光。 */
    private static void meteorQuads(PoseStack ps,Vec3 cam,long now){
        Minecraft mc=Minecraft.getInstance();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        // ① 球体：贴图
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0,ROCK_TEX);
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
        for(Ride r:RIDES){
            Vec3 c=rideCenter(r,now);
            sphere(b,ps,c,r.radius(),0xFFFFFFFF,now*0.0006);
        }
        BufferUploader.drawWithShader(b.end());
        // ② 尾焰：加法混合的相机面片，拖在球体上方
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0,TRAIL_TEX);
        RenderSystem.blendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA,org.lwjgl.opengl.GL11.GL_ONE);
        b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
        for(Ride r:RIDES){
            Vec3 c=rideCenter(r,now);
            Vec3 look=cam.subtract(c);
            if(look.lengthSqr()<1e-4)continue;
            look=look.normalize();
            Vec3 right=new Vec3(0,1,0).cross(look);
            if(right.lengthSqr()<1e-4)continue;
            right=right.normalize();
            double w=r.radius()*0.55,h=r.radius()*1.25;
            Vec3 base=c.add(0,r.radius()*0.55,0);
            Vec3 p0=base.add(right.scale(-w)),p1=base.add(right.scale(w));
            Vec3 p2=p1.add(0,h,0),p3=p0.add(0,h,0);
            tex(b,ps,p0,0,1,0xFF8A3A20);
            tex(b,ps,p1,1,1,0xFF8A3A20);
            tex(b,ps,p2,1,0,0xFFFF9A40);
            tex(b,ps,p3,0,0,0xFFFF9A40);
        }
        BufferUploader.drawWithShader(b.end());
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
    }

    /** 经纬球面（u 平铺 3 次、v 平铺 1.5 次），UV 随时间缓慢滚动。 */
    private static void sphere(BufferBuilder b,PoseStack ps,Vec3 c,double radius,int color,double scroll){
        int lon=28,lat=14;
        for(int i=0;i<lat;i++){
            double v0=i/(double)lat,v1=(i+1)/(double)lat;
            double phi0=Math.PI*(v0-0.5),phi1=Math.PI*(v1-0.5);
            for(int j=0;j<lon;j++){
                double u0=j/(double)lon,u1=(j+1)/(double)lon;
                double th0=u0*Math.PI*2,th1=u1*Math.PI*2;
                Vec3 a=onSphere(c,radius,phi0,th0),bb=onSphere(c,radius,phi0,th1);
                Vec3 cc=onSphere(c,radius,phi1,th1),d=onSphere(c,radius,phi1,th0);
                float uu0=(float)(u0*3+scroll),uu1=(float)(u1*3+scroll);
                float vv0=(float)(v0*1.5),vv1=(float)(v1*1.5);
                tex(b,ps,a,uu0,vv0,color);
                tex(b,ps,bb,uu1,vv0,color);
                tex(b,ps,cc,uu1,vv1,color);
                tex(b,ps,d,uu0,vv1,color);
            }
        }
    }
    private static Vec3 onSphere(Vec3 c,double radius,double phi,double theta){
        double cos=Math.cos(phi);
        return c.add(Math.cos(theta)*cos*radius,Math.sin(phi)*radius,Math.sin(theta)*cos*radius);
    }

    /** 蘑菇云：上升柱 + 顶部扩张盘，尺寸按陨石半径缩放。 */
    private static void cloud(BufferBuilder b,PoseStack ps,Bloom bl,long now){
        double t=(now-bl.start())/(double)Math.max(1,bl.end()-bl.start());
        double r=bl.radius();
        double h=r*(0.25+t*0.85),rad=r*(0.20+t*0.42);
        int alpha=(int)(170*(1-t*0.85));
        int col=(alpha<<24)|0xFF6A20,cap=(alpha<<24)|0xFFB060;
        Vec3 base=bl.at();
        for(int i=0;i<14;i++){
            double a=i*Math.PI/7,z=(i+1)*Math.PI/7;
            Vec3 p0=base.add(Math.cos(a)*rad,0,Math.sin(a)*rad),p1=base.add(Math.cos(z)*rad,0,Math.sin(z)*rad);
            Vec3 p2=p1.add(0,h,0),p3=p0.add(0,h,0);
            quad(b,ps,p0,p1,p2,p3,col);
        }
        disc(b,ps,base.add(0,h,0),rad*(1.25+t*0.5),cap);
    }

    private static void disc(BufferBuilder b,PoseStack ps,Vec3 c,double r,int color){
        int seg=20;
        for(int i=0;i<seg;i++){
            double a=i*Math.PI*2/seg,z=(i+1)*Math.PI*2/seg;
            quad(b,ps,c,c.add(Math.cos(a)*r,0,Math.sin(a)*r),c.add(Math.cos(z)*r,0,Math.sin(z)*r),c,color);
        }
    }

    /** 项目里的自绘约定：世界坐标已经减过相机，顶点直接写世界坐标。 */
    private static void quad(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 c,Vec3 d,Vec3 e,int color){
        vec(b,ps,a,color);vec(b,ps,c,color);vec(b,ps,d,color);vec(b,ps,e,color);
    }
    private static void vec(BufferBuilder b,PoseStack ps,Vec3 p,int color){
        b.vertex(ps.last().pose(),(float)p.x,(float)p.y,(float)p.z).color(color).endVertex();
    }
    private static void tex(BufferBuilder b,PoseStack ps,Vec3 p,float u,float v,int color){
        b.vertex(ps.last().pose(),(float)p.x,(float)p.y,(float)p.z).color(color).uv(u,v).endVertex();
    }

    public static void clear(){DOMAINS.clear();RIDES.clear();BLOOMS.clear();}
    private JogoFxClient() {}
}
