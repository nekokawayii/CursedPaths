package character.jogo;

import cn.blockforge.generated.nnnnr1201forge2.Client;
import cn.blockforge.generated.nnnnr1201forge2.Network;
import cn.blockforge.generated.nnnnr1201forge2.PhotonFx;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** 漏壶的 PhotonFx 表现层：火焰只用模组自己的加法光效，不用原版粒子。极之番·陨 另加贴图尾焰与天空穹顶。 */
@Mod.EventBusSubscriber(modid="jujutsu_kaisen_cursed_paths",value=Dist.CLIENT)
public final class JogoFxClient {
    private static final String GLOW="textures/effect/glow.png", STREAK="textures/effect/streak.png", RING="textures/effect/ring.png";
    private static final int FIRE=0xFFFF7A18, FIRE_BLOOM=0xFFFF3A00, LAVA=0xFFFFB024, EMBER=0xFFFFD08A, ASH=0xFF2A1A18;
    /** ComfyUI 生成的陨石黑红尾焰（黑底已转透明）。 */
    private static final ResourceLocation METEOR_TEX=new ResourceLocation("jujutsu_kaisen_cursed_paths","textures/effect/jogo_meteor_trail.png");
    public record Domain(int ownerId,Vec3 center,double radius,long end) {}
    /** 蓄力：施术者脚下位置 + 结束刻。 */
    private record Charge(int ownerId,Vec3 base,long end) {}
    /** 坠落：落点 + 直径 + 起止刻。 */
    private record Fall(Vec3 to,double size,long start,long end) {}
    /** 落地后的蘑菇云：中心 + 半径 + 起止刻。 */
    private record Bloom(Vec3 at,double radius,long start,long end) {}
    private static final List<Domain> DOMAINS=new ArrayList<>();
    private static final List<Charge> CHARGES=new ArrayList<>();
    private static final List<Fall> FALLS=new ArrayList<>();
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
                    // 脚下火柱：从地面竖直窜起 10 格（粗芯 + 外焰 + 分段爆点）
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
            case JogoFx.JIYAN_CHARGE -> {
                CHARGES.removeIf(c->c.ownerId()==e.owner());
                CHARGES.add(new Charge(e.owner(),pos,now+Math.max(1,e.life())));
                PhotonFx.spark(pos.add(0,2.4,0),2.2,1.6,FIRE,FIRE_BLOOM,GLOW,true,4,12,40,0.8);
                PhotonFx.ring(pos.add(0,0.2,0),7.0,0.8,FIRE_BLOOM,FIRE_BLOOM,40,3,14);
                Client.shake(6);
            }
            case JogoFx.JIYAN_LAUNCH -> {
                CHARGES.clear();
                PhotonFx.beam(pos,pos.add(0,40,0),2.4,14);
                PhotonFx.spark(pos,3.0,2.2,LAVA,FIRE_BLOOM,GLOW,true,3,14,60,1.2);
                Client.shake(10);
            }
            case JogoFx.JIYAN_FALL -> {
                double size=Math.max(8,e.size());
                FALLS.add(new Fall(pos,size,now,now+Math.max(4,e.life())));
                PhotonFx.spark(pos.add(0,60,0),10,8,FIRE,FIRE_BLOOM,GLOW,true,4,20,80,1.4);
            }
            case JogoFx.JIYAN_IMPACT -> {
                FALLS.clear();
                BLOOMS.add(new Bloom(pos,Math.max(8,e.size()),now,now+70));
                PhotonFx.spark(pos,18,12,LAVA,FIRE_BLOOM,GLOW,true,5,20,220,2.0);
                PhotonFx.shockwave(pos,0xFFFF7A18,34,26);
                for(int i=0;i<8;i++){
                    double h=i*2.4;
                    PhotonFx.ring(pos.add(0,h,0),6+i*3.2,2.0,FIRE,FIRE_BLOOM,30,3,10);
                }
                PhotonFx.beam(pos,pos.add(0,34,0),9.5,40);
                Client.shake(30);
                Client.flash(9);
            }
            case JogoFx.JIYAN_SEA -> {
                PhotonFx.spark(pos,Math.max(4,e.size()),2.4,LAVA,FIRE_BLOOM,GLOW,true,3,16,90,0.9);
                PhotonFx.ring(pos,Math.max(4,e.size()),0.7,FIRE,FIRE_BLOOM,30,3,18);
            }
            default -> {}
        }
    }

    /** 每刻：岩浆泡破裂 + 上升火山灰；极之番的蓄力/坠落/蘑菇云持续冒光。 */
    public static void tick() {
        var mc=Minecraft.getInstance();
        if(mc.level==null)return;
        long now=mc.level.getGameTime();
        DOMAINS.removeIf(d->now>=d.end());
        CHARGES.removeIf(c->now>=c.end());
        FALLS.removeIf(f->now>=f.end());
        BLOOMS.removeIf(b->now>=b.end());
        for(Charge c:CHARGES){
            Vec3 head=c.base().add(0,2.5,0);
            if(now%3==0)PhotonFx.spark(head,1.3+(c.end()-now)*0.02,0.9,FIRE,FIRE_BLOOM,GLOW,true,3,9,30,0.7);
            if(now%6==0)PhotonFx.spark(c.base().add(0,0.2,0),4.5,0.5,ASH,0,GLOW,false,4,14,16,0.3);
        }
        for(Fall f:FALLS){
            Vec3 at=meteorPos(f,now);
            PhotonFx.spark(at,7.0,5.0,FIRE,FIRE_BLOOM,GLOW,true,2,7,70,1.1);
            PhotonFx.spark(at.add(0,10,0),4.5,3.0,ASH,0,GLOW,false,3,10,24,0.4);
        }
        for(Bloom bl:BLOOMS){
            double t=(now-bl.start())/(double)Math.max(1,bl.end()-bl.start());
            if(now%4==0)PhotonFx.ring(bl.at().add(0,2+t*26,0),6+t*22,1.6,FIRE,FIRE_BLOOM,26,3,8);
            if(now%6==0)PhotonFx.spark(bl.at().add(0,4+t*22,0),5+t*8,3.0,LAVA,FIRE_BLOOM,GLOW,true,4,12,40,0.7);
        }
    }

    private static Vec3 meteorPos(Fall f,long now){
        double t=Math.min(1,Math.max(0,(now-f.start())/(double)Math.max(1,f.end()-f.start())));
        double drop=120*(1-t)*(1-t);
        return new Vec3(f.to().x,f.to().y+drop,f.to().z);
    }

    // ---------------------------------------------------------------- 自绘：天空穹顶 / 火球 / 陨石 / 蘑菇云
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null)return;
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)return;
        long now=mc.level.getGameTime();
        if(CHARGES.isEmpty()&&FALLS.isEmpty()&&BLOOMS.isEmpty())return;
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
        if(!CHARGES.isEmpty()||!FALLS.isEmpty())sky(b,ps,cam,now);
        for(Charge c:CHARGES)orb(b,ps,c,now);
        for(Fall f:FALLS){
            Vec3 at=meteorPos(f,now);
            rock(b,ps,at,f.size());
            rift(b,ps,f,now);
        }
        for(Bloom bl:BLOOMS)cloud(b,ps,bl,now);
        BufferUploader.drawWithShader(b.end());
        if(!FALLS.isEmpty())tail(ps,cam,now);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        ps.popPose();
    }

    /** 天空变暗红：头顶一块巨大的暗红穹顶（深度测试开着，会被地形挡住，只盖住天空）。 */
    private static void sky(BufferBuilder b,PoseStack ps,Vec3 cam,long now){
        double y=cam.y+110,r=280;
        float pulse=(float)(0.82+0.18*Math.sin(now*0.06));
        int base=((int)(200*pulse)<<24)|0x2A0000;
        quad(b,ps,new Vec3(cam.x-r,y,cam.z-r),new Vec3(cam.x+r,y,cam.z-r),new Vec3(cam.x+r,y,cam.z+r),new Vec3(cam.x-r,y,cam.z+r),base);
        quad(b,ps,new Vec3(cam.x-r,y-8,cam.z-r),new Vec3(cam.x+r,y-8,cam.z-r),new Vec3(cam.x+r,y-8,cam.z+r),new Vec3(cam.x-r,y-8,cam.z+r),0x38FF2A00);
    }

    /** 蓄力：双手托举的黑红火球，越到后面越大越亮。 */
    private static void orb(BufferBuilder b,PoseStack ps,Charge c,long now){
        double grow=Math.min(1,(c.end()-now)/40.0);
        double s=0.45+grow*1.05;
        Vec3 at=c.base().add(0,2.5+grow*0.35,0);
        int core=((int)(230*(0.5+grow*0.5))<<24)|0x2A0500;
        int rim=((int)(180*(0.4+grow*0.6))<<24)|0xFF5A18;
        crossed(b,ps,at,s*1.9,rim);
        crossed(b,ps,at,s,core);
    }

    /** 陨石本体：三张互交的深色面片，给球形体积感。 */
    private static void rock(BufferBuilder b,PoseStack ps,Vec3 at,double size){
        double h=size*0.30;
        crossed(b,ps,at,h,0xFF120800);
        crossed(b,ps,at,h*0.72,0xFF050200);
    }

    /** 天裂：落点上方一道竖着的暗红裂缝，陨石穿出来。 */
    private static void rift(BufferBuilder b,PoseStack ps,Fall f,long now){
        double t=Math.min(1,Math.max(0,(now-f.start())/(double)Math.max(1,f.end()-f.start())));
        double w=2.0+t*5.0;
        double y0=f.to().y+70,y1=f.to().y+150;
        int col=((int)(150*(1-t)+40)<<24)|0x8A1200;
        quad(b,ps,new Vec3(f.to().x-w,y0,f.to().z),new Vec3(f.to().x+w,y0,f.to().z),
            new Vec3(f.to().x+w,y1,f.to().z),new Vec3(f.to().x-w,y1,f.to().z),col);
        quad(b,ps,new Vec3(f.to().x,y0,f.to().z-w),new Vec3(f.to().x,y0,f.to().z+w),
            new Vec3(f.to().x,y1,f.to().z+w),new Vec3(f.to().x,y1,f.to().z-w),col);
    }

    /** 蘑菇云：一根上升的柱体 + 顶部不断变大的盘。 */
    private static void cloud(BufferBuilder b,PoseStack ps,Bloom bl,long now){
        double t=(now-bl.start())/(double)Math.max(1,bl.end()-bl.start());
        double h=8+t*30,rad=5+t*13;
        int alpha=(int)(150*(1-t*0.85));
        int col=(alpha<<24)|0xFF6A20,cap=(alpha<<24)|0xFFB060;
        Vec3 base=bl.at();
        for(int i=0;i<10;i++){
            double a=i*Math.PI/5,z=(i+1)*Math.PI/5;
            Vec3 p0=base.add(Math.cos(a)*rad,0,Math.sin(a)*rad),p1=base.add(Math.cos(z)*rad,0,Math.sin(z)*rad);
            Vec3 p2=p1.add(0,h,0),p3=p0.add(0,h,0);
            quad(b,ps,p0,p1,p2,p3,col);
        }
        disc(b,ps,base.add(0,h,0),rad*(1+t*0.6),cap);
    }

    /** 贴图尾焰：加法混合的相机面片（UV 铺满生成的火焰图），黑红渐变由顶点色压暗。 */
    private static void tail(PoseStack ps,Vec3 cam,long now){
        Minecraft mc=Minecraft.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0,METEOR_TEX);
        RenderSystem.blendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA,org.lwjgl.opengl.GL11.GL_ONE);
        RenderSystem.depthMask(false);
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
        for(Fall f:FALLS){
            Vec3 at=meteorPos(f,now);
            Vec3 look=cam.subtract(at);
            if(look.lengthSqr()<1e-4)continue;
            look=look.normalize();
            Vec3 right=new Vec3(0,1,0).cross(look);
            if(right.lengthSqr()<1e-4)continue;
            right=right.normalize();
            double w=f.size()*0.42,h=f.size()*1.7;
            Vec3 p0=at.add(right.scale(-w));
            Vec3 p1=at.add(right.scale(w));
            Vec3 p2=p1.add(0,h,0);
            Vec3 p3=p0.add(0,h,0);
            tex(b,ps,p0,0,1,0xFF7A3018);
            tex(b,ps,p1,1,1,0xFF7A3018);
            tex(b,ps,p2,1,0,0xFFFF9A40);
            tex(b,ps,p3,0,0,0xFFFF9A40);
        }
        BufferUploader.drawWithShader(b.end());
        RenderSystem.defaultBlendFunc();
    }

    private static void disc(BufferBuilder b,PoseStack ps,Vec3 c,double r,int color){
        int seg=18;
        for(int i=0;i<seg;i++){
            double a=i*Math.PI*2/seg,z=(i+1)*Math.PI*2/seg;
            quad(b,ps,c,c.add(Math.cos(a)*r,0,Math.sin(a)*r),c.add(Math.cos(z)*r,0,Math.sin(z)*r),c,color);
        }
    }

    /** 三张互交面片（XY / ZY / XZ），从任何角度看都有体积。 */
    private static void crossed(BufferBuilder b,PoseStack ps,Vec3 c,double s,int color){
        quad(b,ps,c.add(-s,-s,0),c.add(s,-s,0),c.add(s,s,0),c.add(-s,s,0),color);
        quad(b,ps,c.add(0,-s,-s),c.add(0,-s,s),c.add(0,s,s),c.add(0,s,-s),color);
        quad(b,ps,c.add(-s,0,-s),c.add(-s,0,s),c.add(s,0,s),c.add(s,0,-s),color);
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

    public static void clear(){DOMAINS.clear();CHARGES.clear();FALLS.clear();BLOOMS.clear();}
    private JogoFxClient() {}
}
