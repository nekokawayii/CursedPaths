package cn.blockforge.generated.nnnnr1201forge2;

import character.kurisu.KurisuFx;
import character.kurisu.KurisuHorn;
import character.kurisu.KurisuSkills;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.io.File;

/** 来栖华的客户端表现：把 PLAY/STOP/HUD/WINGS/PILLAR 转给 Photon 时间轴、自绘层与 HUD。 */

public final class KurisuVisuals {
    private static int sanity=-1;
    // 视觉自检：-Dkurisu.selftest=true 时自动依次播放 11 个技能并截图
    private static final boolean SELFTEST=Boolean.getBoolean("kurisu.selftest");
    // 号角专项自检：-Dkurisu.hornselftest=true 只跑"举角 → 短按光柱 → 羽翼咏唱 → 圣光柱"并逐个截图
    private static final boolean HORN_TEST=Boolean.getBoolean("kurisu.hornselftest");
    private static int stTick, stIndex=-1, stCapture=-1;
    private static final int[] ST_SKILLS={character.kurisu.KurisuSkills.BAPTISM,character.kurisu.KurisuSkills.FLAME,
        character.kurisu.KurisuSkills.SPEARS,character.kurisu.KurisuSkills.GATE,character.kurisu.KurisuSkills.SWORD,
        character.kurisu.KurisuSkills.THUNDER,character.kurisu.KurisuSkills.STORM,character.kurisu.KurisuSkills.EXECUTE,
        character.kurisu.KurisuSkills.HORN,character.kurisu.KurisuSkills.SUPPER,character.kurisu.KurisuSkills.DOMAIN};
    private static net.minecraft.world.level.Level lastLevel;
    private static int hornTick, hornShot=-1, hornStage=-1;
    private static Vec3 hornGround;

    public static void receive(Network.Effect e){
        switch(e.kind()){
            case KurisuFx.PLAY -> {
                int skill=(int)e.size(),variant=(int)e.seed();
                if(skill==KurisuSkills.HORN&&variant==KurisuHorn.V_TAP)
                    KurisuHornRender.tapBeam(e.position(),e.direction(),Math.max(8,e.life()));
                KurisuPhoton.play(skill,variant,e.position(),e.direction(),Math.max(20,e.life()),e.owner());
            }
            case KurisuFx.STOP -> {KurisuPhoton.stop((int)e.size());KurisuHornRender.clearFor((int)e.size());}
            case KurisuFx.HUD -> sanity=(int)e.size();
            case KurisuFx.WINGS -> KurisuHornRender.channel(e.owner(),Math.max(10,e.life()));
            case KurisuFx.PILLAR -> KurisuHornRender.ritual(e.position(),Math.max(20,e.life()));
            default -> {}
        }
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||!mc.player.isAlive()){
            KurisuPhoton.stopAll();KurisuHornRender.clear();
        }else if(mc.player.level()!=lastLevel){if(lastLevel!=null){KurisuPhoton.stopAll();KurisuHornRender.clear();}lastLevel=mc.player.level();}
        KurisuPhoton.tick();
        if(mc.player!=null&&mc.level!=null&&HORN_TEST)hornSelfTest(mc);
        else if(mc.player!=null&&mc.level!=null&&(SELFTEST||"Dev".equals(mc.player.getGameProfile().getName())))selfTest(mc);
        var d=character.CharacterClient.selected();
        if(d==null||!"kurisu".equals(d.id()))sanity=-1;
    }

    private static void selfTest(Minecraft mc){
        if(mc.screen!=null)mc.setScreen(null);
        if(!mc.player.isAlive()){
            try{mc.player.respawn();}catch(Throwable ignored){}
            return;
        }
        stTick++;
        if(stTick==2||stTick==100){
            try{
                mc.player.connection.sendCommand("gamemode creative");
                mc.player.connection.sendCommand("effect give @s minecraft:resistance 9999 9 true");
                mc.player.connection.sendCommand("effect give @s minecraft:regeneration 9999 4 true");
            }catch(Throwable ignored){}
        }
        if(stIndex>=0&&stCapture>0&&--stCapture==0){
            shot(mc,"selftest_"+ST_SKILLS[stIndex]);
        }
        if(stTick%45!=0)return;
        stIndex++;
        if(stIndex>=ST_SKILLS.length){stIndex=-1;return;}
        int skill=ST_SKILLS[stIndex];
        KurisuPhoton.stopAll();
        KurisuPhoton.play(skill,0,mc.player.position().add(0,2,0).add(mc.player.getLookAngle().scale(12)),
            mc.player.getLookAngle(),120,-1);
        stCapture=6;
    }

    /**
     * 号角专项自检：把四个阶段按时间轴跑一遍并各截一张图（第三视角，光柱侧看）。
     * <ol>
     *   <li>t=20 举角（READY）→ 28 截图；</li>
     *   <li>t=40 短按光柱（TAP）→ 46 转身 90° 侧看 → 54 截图；</li>
     *   <li>t=80 羽翼咏唱（WINGS）→ 92 / 122 两张；</li>
     *   <li>t=140 收羽翼 → t=150 圣光柱（PILLAR + 地面法阵/天裂）→ 158 / 182 / 208 三张。</li>
     * </ol>
     */
    private static final String[] HORN_STAGES={"ready","tap","wings","wings2","pillar_a","pillar_b","pillar_c",
        "live_wings","live_pillar_a","live_pillar_b"};
    private static void hornSelfTest(Minecraft mc){
        if(mc.screen!=null)mc.setScreen(null);
        if(!mc.player.isAlive()){
            try{mc.player.respawn();}catch(Throwable ignored){}
            return;
        }
        hornTick++;
        // r464：命令在测试存档里可能没开作弊，改用客户端侧控制（相机/视角都是客户端权威的）
        if(hornTick==2){
            try{mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);}catch(Throwable ignored){}
            mc.player.setXRot(22f);
            com.mojang.logging.LogUtils.getLogger().info("[来栖华自检] 起点 "+mc.player.position()+" 俯仰 "+mc.player.getXRot());
        }
        if(hornShot>0&&--hornShot==0)shot(mc,"horn_"+HORN_STAGES[Math.max(0,Math.min(HORN_STAGES.length-1,hornStage))]);
        if(hornTick==20){
            hornStage=0;
            KurisuPhoton.play(KurisuSkills.HORN,KurisuHorn.V_READY,mc.player.position(),mc.player.getLookAngle(),20,mc.player.getId());
            hornShot=8;
        }else if(hornTick==40){
            hornStage=1;
            KurisuHornRender.tapBeam(mc.player.getEyePosition(),mc.player.getLookAngle(),60);
            KurisuPhoton.play(KurisuSkills.HORN,KurisuHorn.V_TAP,mc.player.getEyePosition(),mc.player.getLookAngle(),60,mc.player.getId());
        }else if(hornTick==46){
            // 光柱方向固定在施法那一刻，人转 90° 就能从侧面看到"柱"
            mc.player.setYRot(mc.player.getYRot()+90f);
            hornShot=8;
        }else if(hornTick==80){
            hornStage=2;
            KurisuHornRender.channel(mc.player.getId(),120);
            KurisuPhoton.play(KurisuSkills.HORN,KurisuHorn.V_WINGS,mc.player.position(),mc.player.getLookAngle(),120,mc.player.getId());
            hornShot=12;
        }else if(hornTick==110){
            hornStage=3;
            hornShot=12;
        }else if(hornTick==140){
            KurisuHornRender.clear();
            KurisuPhoton.stop(KurisuSkills.HORN,KurisuHorn.V_WINGS);
            mc.player.setXRot(30f);
        }else if(hornTick==150){
            hornStage=4;
            hornGround=groundPoint(mc,9);
            com.mojang.logging.LogUtils.getLogger().info("[来栖华自检] 圣光柱落点 "+hornGround);
            KurisuHornRender.ritual(hornGround,120);
            KurisuPhoton.play(KurisuSkills.HORN,KurisuHorn.V_PILLAR,hornGround,Vec3.ZERO,120,mc.player.getId());
            hornShot=8;
        }else if(hornTick==176){
            hornStage=5;
            hornShot=6;
        }else if(hornTick==196){
            hornStage=6;
            mc.player.setXRot(48f);
            hornShot=12;
        }else if(hornTick==250){
            // 真实链路：走服务端命令 → KurisuHorn.autoTest（按下/心跳/松手都在服务端跑）
            KurisuPhoton.stopAll();KurisuHornRender.clear();
            mc.player.setXRot(18f);
            try{mc.player.connection.sendCommand("jjksp kurisuhorn auto");}catch(Throwable t){
                com.mojang.logging.LogUtils.getLogger().error("[来栖华自检] auto 命令发送失败",t);
            }
        }else if(hornTick==258){
            hornStage=7;   // +8 刻服务端刚展开羽翼
            hornShot=10;
        }else if(hornTick==288){
            hornStage=8;   // 松手后光柱已落下
            hornShot=8;
        }else if(hornTick==320){
            hornStage=9;
            hornShot=8;
        }
    }

    /** 客户端侧求准星落点：前方 {@code dist} 格处向下探地面，光柱才不会悬空/糊脸。 */
    private static Vec3 groundPoint(Minecraft mc,double dist){
        Vec3 look=mc.player.getLookAngle();
        Vec3 flat=new Vec3(look.x,0,look.z);
        if(flat.lengthSqr()<1e-4)flat=new Vec3(0,0,1);
        flat=flat.normalize();
        Vec3 base=mc.player.position().add(flat.scale(dist));
        Vec3 from=base.add(0,10,0);
        var hit=mc.level.clip(new ClipContext(from,from.add(0,-42,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player));
        return hit.getType()==HitResult.Type.MISS?base:hit.getLocation();
    }

    private static void shot(Minecraft mc,String name){
        try{
            new File(mc.gameDirectory,"screenshots").mkdirs();
            net.minecraft.client.Screenshot.grab(mc.gameDirectory,name+".png",mc.getMainRenderTarget(),v->{});
            com.mojang.logging.LogUtils.getLogger().info("[来栖华自检] 截图 "+name);
        }catch(Throwable t){com.mojang.logging.LogUtils.getLogger().error("[来栖华自检] 截图失败",t);}
    }

    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){
        if(sanity<0)return;
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.options.hideGui)return;
        GuiGraphics g=e.getGuiGraphics();
        int w=g.guiWidth(), h=g.guiHeight();
        int barW=120, x=(w-barW)/2, y=h-52;
        g.fill(x-1,y-1,x+barW+1,y+7,0xAA000000);
        int fill=(int)(barW*(sanity/100.0));
        g.fill(x,y,x+fill,y+6,0xFFFFD77A);
        g.drawString(mc.font,"理智 "+sanity+"%",x,y-11,0xFFFFF6D8,true);
    }
    private KurisuVisuals(){}
}
