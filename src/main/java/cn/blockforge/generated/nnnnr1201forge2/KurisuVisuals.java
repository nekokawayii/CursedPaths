package cn.blockforge.generated.nnnnr1201forge2;

import character.kurisu.KurisuFx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.io.File;

/** 来栖华的客户端表现：只负责把 PLAY/STOP/HUD 转给 Photon 时间轴与 HUD。 */

public final class KurisuVisuals {
    private static int sanity=-1;
    // 视觉自检：-Dkurisu.selftest=true 时自动依次播放 11 个技能并截图
    private static final boolean SELFTEST=Boolean.getBoolean("kurisu.selftest");
    private static int stTick, stIndex=-1, stCapture=-1;
    private static final int[] ST_SKILLS={character.kurisu.KurisuSkills.BAPTISM,character.kurisu.KurisuSkills.FLAME,
        character.kurisu.KurisuSkills.SPEARS,character.kurisu.KurisuSkills.GATE,character.kurisu.KurisuSkills.SWORD,
        character.kurisu.KurisuSkills.THUNDER,character.kurisu.KurisuSkills.STORM,character.kurisu.KurisuSkills.EXECUTE,
        character.kurisu.KurisuSkills.HORN,character.kurisu.KurisuSkills.SUPPER,character.kurisu.KurisuSkills.DOMAIN};
    private static net.minecraft.world.level.Level lastLevel;

    public static void receive(Network.Effect e){
        switch(e.kind()){
            case KurisuFx.PLAY -> KurisuPhoton.play((int)e.size(),e.position(),e.direction(),Math.max(20,e.life()));
            case KurisuFx.STOP -> KurisuPhoton.stop((int)e.size());
            case KurisuFx.HUD -> sanity=(int)e.size();
            default -> {}
        }
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||!mc.player.isAlive())KurisuPhoton.stopAll();
        else if(mc.player.level()!=lastLevel){if(lastLevel!=null)KurisuPhoton.stopAll();lastLevel=mc.player.level();}
        KurisuPhoton.tick();
        if(mc.player!=null&&mc.level!=null&&(SELFTEST||"Dev".equals(mc.player.getGameProfile().getName())))selfTest(mc);
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
            try{
                new File(mc.gameDirectory,"screenshots").mkdirs();
                net.minecraft.client.Screenshot.grab(mc.gameDirectory, "selftest_"+ST_SKILLS[stIndex]+".png", mc.getMainRenderTarget(), v->{});
                com.mojang.logging.LogUtils.getLogger().info("[来栖华自检] 截图 skill="+ST_SKILLS[stIndex]);
            }catch(Throwable t){com.mojang.logging.LogUtils.getLogger().error("[来栖华自检] 截图失败",t);}
        }
        if(stTick%45!=0)return;
        stIndex++;
        if(stIndex>=ST_SKILLS.length){stIndex=-1;return;}
        int skill=ST_SKILLS[stIndex];
        KurisuPhoton.stopAll();
        KurisuPhoton.play(skill,mc.player.position().add(0,2,0).add(mc.player.getLookAngle().scale(12)),mc.player.getLookAngle(),120);
        stCapture=6;
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