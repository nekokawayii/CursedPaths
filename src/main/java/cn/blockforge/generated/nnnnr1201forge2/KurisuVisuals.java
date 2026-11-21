package cn.blockforge.generated.nnnnr1201forge2;

import character.kurisu.KurisuFx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;

/** 来栖华的客户端表现：金色圣光、光柱、法阵、羽状光带。 */
@Mod.EventBusSubscriber(modid = "jujutsu_kaisen_cursed_paths", value = Dist.CLIENT)
public final class KurisuVisuals {
    /** -1 = 未选来栖华；客户端由 HUD 包更新。 */
    private static int sanity=-1;
    private static final int GOLD=0xFFFFD77A, WHITE=0xFFFFF6D8, DEEP=0xFFB98A2A;
    private static final String GLOW="textures/effect/glow.png", DUST="textures/effect/dust.png";

    public static void receive(Network.Effect e) {
        Vec3 at=e.position(), dir=e.direction();
        float size=Math.max(.3f,e.size());
        switch(e.kind()){
            case KurisuFx.BEAM -> {
                if(PhotonFx.ready()){
                    PhotonFx.beam(at,dir,Math.max(.6,size*.5),6);
                    PhotonFx.spark(at,1.2,.8,WHITE,GOLD,GLOW,true,6,10,40,.3);
                }
                Client.shake(3);
            }
            case KurisuFx.SIGIL -> {
                if(PhotonFx.ready()){
                    PhotonFx.ring(at,size,.55,GOLD,DEEP,26,8,14);
                    PhotonFx.spark(at,size*.5,.6,WHITE,GOLD,GLOW,true,8,12,30,.2);
                }
            }
            case KurisuFx.FLAME -> {
                if(PhotonFx.ready())PhotonFx.spark(at,size,.9,WHITE,GOLD,GLOW,true,6,12,60,.5);
            }
            case KurisuFx.SPEAR -> {
                if(PhotonFx.ready()){
                    PhotonFx.beam(at.add(0,18,0),at,1.1,8);
                    PhotonFx.spark(at,1.0,.7,WHITE,GOLD,GLOW,true,5,8,26,.2);
                }
                Client.shake(2);
            }
            case KurisuFx.GATE -> {
                if(PhotonFx.ready()){
                    PhotonFx.ring(at,size,1.0,GOLD,WHITE,30,10,16);
                    PhotonFx.spark(at,size*.4,.9,GOLD,WHITE,GLOW,true,10,16,24,.1);
                }
            }
            case KurisuFx.SWORD -> {
                if(PhotonFx.ready())PhotonFx.spark(at,1.0,.5,WHITE,GOLD,GLOW,true,8,10,20,.1);
            }
            case KurisuFx.THUNDER -> {
                if(PhotonFx.ready()){
                    PhotonFx.beam(at.add(0,24,0),at,3.2,10);
                    PhotonFx.shockwave(at,0xFFFFE9A8,(int)(size*2.2),10);
                }
                Client.shake(8);
            }
            case KurisuFx.STORM -> {
                if(PhotonFx.ready()){
                    PhotonFx.ring(at,size,.8,GOLD,WHITE,40,12,18);
                    PhotonFx.shockwave(at,0xFFFFD77A,(int)size,10);
                }
                Client.shake(5);
            }
            case KurisuFx.EXECUTE -> {
                if(PhotonFx.ready())PhotonFx.spark(at,2.4,1.6,WHITE,GOLD,GLOW,true,8,14,60,.9);
                Client.shake(6);
            }
            case KurisuFx.HORN -> {
                if(PhotonFx.ready()){
                    PhotonFx.beam(at,at.add(0,-40,0),6.0,10);
                    PhotonFx.ring(at,size,.9,WHITE,GOLD,46,14,20);
                }
                Client.shake(7);
            }
            case KurisuFx.SUPPER -> {
                if(PhotonFx.ready()){
                    PhotonFx.beam(at.add(0,26,0),at,2.6,8);
                    PhotonFx.spark(at,1.6,1.0,WHITE,GOLD,GLOW,true,8,12,40,.5);
                }
                Client.shake(4);
            }
            case KurisuFx.DOMAIN -> {
                if(PhotonFx.ready()){
                    PhotonFx.ring(at,size,1.4,WHITE,GOLD,60,20,24);
                    PhotonFx.domain(at,size,e.life());
                }
            }
            case KurisuFx.HUD -> sanity=(int)e.size();
            case KurisuFx.MARK -> {
                if(PhotonFx.ready())PhotonFx.spark(at,1.4,.9,WHITE,GOLD,GLOW,true,10,14,30,.2);
            }
            default -> {}
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
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

    private KurisuVisuals() {}
}
