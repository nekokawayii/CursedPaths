package cn.blockforge.generated.nnnnr1201forge2;

import character.kurisu.KurisuFx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 来栖华的客户端表现：只负责把 PLAY/STOP/HUD 转给 Photon 时间轴与 HUD。 */
@Mod.EventBusSubscriber(modid = "jujutsu_kaisen_cursed_paths", value = Dist.CLIENT)
public final class KurisuVisuals {
    private static int sanity=-1;
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
        var d=character.CharacterClient.selected();
        if(d==null||!"kurisu".equals(d.id()))sanity=-1;
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
