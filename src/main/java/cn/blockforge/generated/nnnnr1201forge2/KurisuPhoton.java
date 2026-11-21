package cn.blockforge.generated.nnnnr1201forge2;

import character.kurisu.KurisuSkills;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/**
 * 来栖华的 Photon 特效。用 PhotonFx 里已被五条悟验证过的 beam / ring / shockwave / domain
 * 原语组合，避免自建 cylinder 的朝向/拉伸问题，也避免大面片叠加过曝。
 */
public final class KurisuPhoton {
    private static final int GOLD=0xFFFFD77A, WHITE=0xFFFFF6D8;
    private static final String GLOW="textures/effect/glow.png";
    private record Active(PhotonFx.Handle handle,int skill,long end){}
    private static final List<Active> ACTIVE=new ArrayList<>();

    private static void add(int skill,PhotonFx.Handle h,int ticks){
        if(h!=null)ACTIVE.add(new Active(h,skill,now()+Math.max(10,ticks)));
    }

    public static void play(int skill,Vec3 at,Vec3 dir,int life){
        stop(skill);
        int d=Math.max(20,life);
        switch(skill){
            case KurisuSkills.BAPTISM -> {
                add(skill,PhotonFx.beam(at.add(0,26,0),at,0.7,8),12);
                add(skill,PhotonFx.shell(at,4,0x66FFF6D8,0,20),24);
            }
            case KurisuSkills.FLAME -> {
                add(skill,PhotonFx.ring(at.add(0,.15,0),3.2,.55,GOLD,0,26,160,16),170);
                add(skill,PhotonFx.sphere(at.add(0,.6,0),2.2,.55,WHITE,0,GLOW,true,160,12,20,.5),170);
            }
            case KurisuSkills.SPEARS -> {
                for(int i=0;i<4;i++){
                    double a=i*Math.PI/2, r=1.6;
                    Vec3 base=at.add(Math.cos(a)*r,0,Math.sin(a)*r);
                    add(skill,PhotonFx.beam(base.add(0,20,0),base,0.35,6),10);
                }
                add(skill,PhotonFx.ring(at,2.6,.4,GOLD,0,30,20,12),24);
            }
            case KurisuSkills.GATE -> {
                add(skill,PhotonFx.ring(at,2.0,.6,GOLD,0xFFD77A,32,150,14),160);
                add(skill,PhotonFx.ring(at.add(0,.2,0),1.4,.35,WHITE,0,24,150,10),160);
            }
            case KurisuSkills.SWORD -> {
                add(skill,PhotonFx.sphere(at,.5,.5,WHITE,0xFFD77A,GLOW,true,260,8,18,.6),270);
                add(skill,PhotonFx.beam(at,at.add(dir.scale(2.2)),0.25,260),270);
            }
            case KurisuSkills.THUNDER -> {
                add(skill,PhotonFx.beam(at.add(0,30,0),at,2.0,8),12);
                add(skill,PhotonFx.shockwave(at,0xFFFFE9A8,5,14),18);
                add(skill,PhotonFx.ring(at,3.4,.6,GOLD,0xFFD77A,40,24,12),26);
            }
            case KurisuSkills.STORM -> {
                add(skill,PhotonFx.shell(at,7,0x55FFF6D8,0,30),34);
                add(skill,PhotonFx.ring(at,7.5,.7,GOLD,0xFFD77A,52,26,16),30);
            }
            case KurisuSkills.EXECUTE -> {
                add(skill,PhotonFx.sphere(at,.8,.9,WHITE,0xFFD77A,GLOW,true,10,6,26,.4),14);
                add(skill,PhotonFx.shockwave(at,0xFFFFD77A,3,12),16);
            }
            case KurisuSkills.HORN -> {
                add(skill,PhotonFx.beam(at.add(0,40,0),at.add(0,-2,0),3.2,60),64);
                add(skill,PhotonFx.ring(at,3.0,.6,WHITE,0xFFD77A,44,60,14),64);
                add(skill,PhotonFx.ring(at.add(0,.5,0),1.8,.4,GOLD,0,32,60,10),64);
            }
            case KurisuSkills.SUPPER -> {
                for(int i=0;i<3;i++){
                    double a=i*Math.PI*2/3, r=5;
                    Vec3 base=at.add(Math.cos(a)*r,0,Math.sin(a)*r);
                    add(skill,PhotonFx.beam(base.add(0,30,0),base,1.4,8),12);
                }
                add(skill,PhotonFx.shockwave(at,0xFFFFD77A,7,16),18);
            }
            case KurisuSkills.DOMAIN -> {
                int dur=Math.max(60,Math.min(600,d));
                add(skill,PhotonFx.shell(at,22,0x44FFF6D8,0,dur),dur);
                add(skill,PhotonFx.ring(at,20,.7,WHITE,0xFFF6D8,36,dur,14),dur);
            }
            default -> {}
        }
        add(skill,PhotonFx.sphere(at,.35,.45,WHITE,0xFFD77A,GLOW,true,8,5,14,0),10);
    }

    public static void stop(int skill){
        ACTIVE.removeIf(a->{
            if(a.skill()!=skill)return false;
            PhotonFx.stop(a.handle());
            return true;
        });
    }
    public static void stopAll(){
        for(var a:ACTIVE)PhotonFx.stop(a.handle());
        ACTIVE.clear();
    }
    public static void tick(){
        long now=now();
        ACTIVE.removeIf(a->{
            if(a.end()>now)return false;
            PhotonFx.stop(a.handle());
            return true;
        });
    }
    private static long now(){var l=Minecraft.getInstance().level;return l==null?0:l.getGameTime();}
    private KurisuPhoton(){}
}
