package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 黑闪标记的客户端渲染/瞄准（从 BlackFlashMarks 拆出，专用服务端不会加载）。 */
public final class BlackFlashClient {
    public record Mark(int entityId,Vec3 offset,long until){}
    public static Vec3 point(Mark mark){var level=Minecraft.getInstance().level;var target=level==null?null:level.getEntity(mark.entityId);return target==null?null:target.position().add(mark.offset);}
    public static final List<Mark> CLIENT=new ArrayList<>();
    private static final List<Mark> DESYNC=new ArrayList<>();
    /** 左键上一刻是否按下（用于取上升沿，避开原版攻击把 consumeClick 吃掉）。 */
    private static boolean attackDown;

    public static void receive(Network.Effect e){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        var target=mc.level.getEntity(e.owner());if(target==null)return;CLIENT.removeIf(m->m.entityId()==e.owner());CLIENT.add(new Mark(e.owner(),e.position().subtract(target.position()),mc.level.getGameTime()+Math.max(4,e.life())));
    }

    /** 客户端每刻：过期清理 + 准星是否压在某个红圈上。 */
    public static void clientTick(){
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null){CLIENT.clear();return;}
        long now=mc.level.getGameTime();
        CLIENT.removeIf(m->now>m.until());
        if(ThousandMileChainClient.holding()){attackDown=mc.options.keyAttack.isDown();return;}
        if(!Client.progress.fist()||mc.screen!=null)return;
        Mark aimed=aimed();
        boolean down=mc.options.keyAttack.isDown();
        Mark aimedNow=aimed();
        if(down&&!attackDown&&aimedNow!=null){
            com.mojang.logging.LogUtils.getLogger().info("[黑闪] 瞄准红圈+左键 → 发送 strike target={} dist={:.2f}",aimedNow.entityId(),mc.player.position().distanceTo(mc.level.getEntity(aimedNow.entityId())==null?mc.player.position():mc.level.getEntity(aimedNow.entityId()).position()));
            Network.CHANNEL.sendToServer(new Network.BlackFlashStrike(aimedNow.entityId()));
        }
        attackDown=down;
    }
    /** 准星射线与红圈（半径 0.3 + 0.15 容差）求交；取最近的那个。 */
    public static Mark aimed(){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return null;
        Vec3 eye=mc.player.getEyePosition(1f),look=mc.player.getViewVector(1f);
        double best=Double.MAX_VALUE;Mark hit=null;
        for(Mark m:CLIENT){
            if(mc.level.getEntity(m.entityId())==null)continue;
            Vec3 position=point(m);if(position==null)continue;Vec3 d=position.subtract(eye);
            double t=d.dot(look);
            if(t<=0||t>6.0)continue;
            double dist=d.subtract(look.scale(t)).length();
            if(dist<=BlackFlashMarks.MARK_RADIUS+0.45&&t<best){best=t;hit=m;}
        }
        return hit;
    }

    /** 客户端渲染：关深度测试，**始终画在生物之上**（用户要求的最高优先级）。 */
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(CLIENT.isEmpty())return;
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        long now=mc.level.getGameTime();
        var cam=e.getCamera().getPosition();
        float t=mc.level.getGameTime()+e.getPartialTick();
        RenderSystem.disableDepthTest();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        var ps=e.getPoseStack();ps.pushPose();ps.translate(-cam.x,-cam.y,-cam.z);
        for(Mark m:CLIENT){
            float left=(m.until()-t)/BlackFlashMarks.MARK_TICKS;                        // 0~1 剩余
            if(left<0)continue;
            int a=(int)((0.55+0.45*Math.abs(Math.sin(t*0.6)))*255*Math.min(1,left*3));
            Vec3 position=point(m);if(position!=null)ring(b,ps,position,BlackFlashMarks.MARK_RADIUS,a,cam);
        }
        BufferUploader.drawWithShader(b.end());
        ps.popPose();RenderSystem.enableCull();RenderSystem.enableDepthTest();RenderSystem.disableBlend();
    }
    /** 一个水平红圈（朝向相机的细环，用两圈三角带近似）。 */
    private static void ring(BufferBuilder b,PoseStack ps,Vec3 c,double r,int alpha,Vec3 cam){
        // 竖着画（面向相机的竖直圆环）——横圈贴着地面根本瞄不到
        Vec3 n=cam.subtract(c);if(n.lengthSqr()<1e-6)n=new Vec3(0,0,1);n=n.normalize();
        Vec3 up0=new Vec3(0,1,0).subtract(n.scale(n.y));
        if(up0.lengthSqr()<1e-4)up0=new Vec3(1,0,0);
        Vec3 right=n.cross(up0).normalize(),upv=up0.normalize();
        int color=(alpha<<24)|0xFF2A18;
        int seg=24;
        for(int i=0;i<seg;i++){
            double a0=i*Math.PI*2/seg,a1=(i+1)*Math.PI*2/seg;
            Vec3 p0=c.add(right.scale(Math.cos(a0)*r)).add(upv.scale(Math.sin(a0)*r));
            Vec3 p1=c.add(right.scale(Math.cos(a1)*r)).add(upv.scale(Math.sin(a1)*r));
            Vec3 p0b=c.add(right.scale(Math.cos(a0)*r*0.72)).add(upv.scale(Math.sin(a0)*r*0.72));
            Vec3 p1b=c.add(right.scale(Math.cos(a1)*r*0.72)).add(upv.scale(Math.sin(a1)*r*0.72));
            quad(b,ps,p0,p1,p1b,p0b,color);
        }
    }
    private static void quad(BufferBuilder b,PoseStack ps,Vec3 p0,Vec3 p1,Vec3 p2,Vec3 p3,int color){
        b.vertex(ps.last().pose(),(float)p0.x,(float)p0.y,(float)p0.z).color(color).endVertex();
        b.vertex(ps.last().pose(),(float)p1.x,(float)p1.y,(float)p1.z).color(color).endVertex();
        b.vertex(ps.last().pose(),(float)p2.x,(float)p2.y,(float)p2.z).color(color).endVertex();
        b.vertex(ps.last().pose(),(float)p3.x,(float)p3.y,(float)p3.z).color(color).endVertex();
    }
}
