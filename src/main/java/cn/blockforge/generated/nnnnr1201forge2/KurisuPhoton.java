package cn.blockforge.generated.nnnnr1201forge2;

import character.kurisu.KurisuSkills;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/**
 * 来栖华 · 邪去悔天梯的 Photon 特效。
 * <p>同一套技能号下分四个变体（由 {@link character.kurisu.KurisuFx} 的 seed 带过来）：
 * <ul>
 *   <li>{@link #V_READY} 举角起手：脚下小法阵 + 角口聚光 + 两圈经文；</li>
 *   <li>{@link #V_TAP} 短按：沿准星射出的**持续金色光柱**，柱内符文飘飞；</li>
 *   <li>{@link #V_WINGS} 长按咏唱：金光包裹自身、经文环绕、羽毛上升（巨大羽翼由 KurisuHornRender 自绘）；</li>
 *   <li>{@link #V_PILLAR} 松手：通天彻地的巨型金色光柱 + 金色羽毛 + 经文 + 地面法阵（法阵/天裂见 KurisuHornRender）。</li>
 * </ul>
 * 跟随施法者的变体（{@link #V_WINGS}）用 {@code follow=true}，每刻把句柄挪到玩家身上，不做重建。
 */
public final class KurisuPhoton {
    private static final int GOLD=0xFFFFD77A, WHITE=0xFFFFF6D8, AMBER=0xFFFFE9A8;
    private static final String GLOW="textures/effect/glow.png", STREAK="textures/effect/streak.png",
        RING="textures/effect/ring.png", FEATHER="textures/effect/feather.png",
        RUNE1="textures/effect/rune1.png", RUNE2="textures/effect/rune2.png", RUNE3="textures/effect/rune3.png";
    private static final String[] RUNES={RUNE1,RUNE2,RUNE3};

    public static final int V_READY=0, V_TAP=1, V_WINGS=2, V_PILLAR=3;

    private record Active(PhotonFx.Handle handle,int skill,int variant,long end,int owner,boolean follow){}
    private static final List<Active> ACTIVE=new ArrayList<>();

    private static void add(int skill,int variant,PhotonFx.Handle h,int ticks,int owner,boolean follow){
        if(h!=null)ACTIVE.add(new Active(h,skill,variant,now()+Math.max(10,ticks),owner,follow));
    }
    private static String rune(int i){return RUNES[Math.floorMod(i,RUNES.length)];}

    public static void play(int skill,int variant,Vec3 at,Vec3 dir,int life,int owner){
        stop(skill,variant);
        int d=Math.max(12,life);
        switch(skill){
            case KurisuSkills.HORN -> horn(variant,at,dir,d,owner);
            default -> legacy(skill,at,dir,d);
        }
    }

    // ---------------------------------------------------------------- 号角：短按 / 咏唱 / 圣光柱
    private static void horn(int variant,Vec3 at,Vec3 dir,int life,int owner){
        switch(variant){
            case V_READY -> {
                // 举角：脚下一圈小法阵 + 角口聚光 + 两圈经文
                add(KurisuSkills.HORN,V_READY,PhotonFx.ring(at.add(0,.05,0),1.45,.55,GOLD,0xFFD77A,26,12,8),14,owner,true);
                add(KurisuSkills.HORN,V_READY,PhotonFx.sphere(at.add(dir.scale(.7)).add(0,1.2,0),.45,.42,WHITE,GOLD,GLOW,true,12,6,14,.2),14,owner,true);
                add(KurisuSkills.HORN,V_READY,PhotonFx.parts(at.add(0,1.0,0),
                    PhotonFx.Part.at(PhotonFx.RING,1.15,.5,.42,rune(0),0,0,AMBER,GOLD,true,12,10,7,0,1.5),
                    PhotonFx.Part.at(PhotonFx.RING,1.15,.5,.42,rune(1),0,0,AMBER,GOLD,true,12,10,7,0,-1.5)
                ),14,owner,true);
            }
            case V_TAP -> {
                // 短按：一道沿准星的持续光柱（最远 28 格），柱内符文顺柱飘飞
                Vec3 end=at.add(dir.scale(28));
                add(KurisuSkills.HORN,V_TAP,PhotonFx.columnBeam(at,end,.85,0x99FFF6D8,0x44FFD77A,GOLD,life,18,10,.8,7),life,owner,false);
                List<PhotonFx.Part> parts=new ArrayList<>();
                for(int i=0;i<4;i++){
                    double along=6+i*7;
                    parts.add(PhotonFx.Part.at(PhotonFx.SPHERE,1.25,1,.52,rune(i),0,0,AMBER,GOLD,true,life,9,6,0,.12)
                        .move(dir.x*along,dir.y*along,dir.z*along));
                }
                add(KurisuSkills.HORN,V_TAP,PhotonFx.parts(at,parts.toArray(new PhotonFx.Part[0])),life,owner,false);
                add(KurisuSkills.HORN,V_TAP,PhotonFx.sphere(end,1.5,1.05,0xAAFFF6D8,GOLD,GLOW,true,Math.min(life,20),8,18,.15),life,owner,false);
            }
            case V_WINGS -> {
                // 咏唱：金光包裹、经文环绕、羽毛上升（句柄跟随玩家）
                add(KurisuSkills.HORN,V_WINGS,PhotonFx.shell(at.add(0,1,0),2.5,0x0CFFF6D8,GOLD,life),life,owner,true);
                List<PhotonFx.Part> parts=new ArrayList<>();
                parts.add(PhotonFx.Part.at(PhotonFx.RING,1.55,.5,.5,rune(0),0,0,AMBER,GOLD,true,life,14,3,0,1.6).move(0,.45,0));
                parts.add(PhotonFx.Part.at(PhotonFx.RING,1.55,.5,.5,rune(1),0,0,AMBER,GOLD,true,life,14,3,0,-1.6).move(0,1.35,0));
                parts.add(PhotonFx.Part.at(PhotonFx.RING,1.35,.5,.46,rune(2),0,0,GOLD,GOLD,true,life,14,3,0,1.1).move(0,2.15,0));
                add(KurisuSkills.HORN,V_WINGS,PhotonFx.parts(at,parts.toArray(new PhotonFx.Part[0])),life,owner,true);
            }
            case V_PILLAR -> pillar(at,dir,life,owner);
            default -> {}
        }
    }

    /** 松手：天空裂开 → 通天光柱 → 羽毛/经文 → 地面法阵。 */
    private static void pillar(Vec3 at,Vec3 dir,int life,int owner){
        Vec3 top=at.add(0,120,0), base=at.add(0,-2,0);
        // ① 光柱主体：亮芯 + 巨大外晕（半径 4.8 / 2.2）
        add(KurisuSkills.HORN,V_PILLAR,PhotonFx.columnBeam(base,at.add(0,30,0),4.6,0x26FFE9A8,0x12FFD77A,0,life,26,16,0.55,12),life,owner,false);
        add(KurisuSkills.HORN,V_PILLAR,PhotonFx.columnBeam(at.add(0,26,0),at.add(0,74,0),3.8,0x1CFFE9A8,0x0CFFD77A,0,life,18,12,0.62,14),life,owner,false);
        add(KurisuSkills.HORN,V_PILLAR,PhotonFx.columnBeam(at.add(0,70,0),top.add(0,4,0),3.0,0x16FFE9A8,0x08FFD77A,0,life,12,8,0.7,16),life,owner,false);
        add(KurisuSkills.HORN,V_PILLAR,PhotonFx.columnBeam(base,top.add(0,4,0),1.15,0x88FFF6D8,0x33FFD77A,0,life,28,14,0.9,6),life,owner,false);
        // ② 羽毛：柱内向下飘落（分 6 层，避免全挤在一点）
        List<PhotonFx.Part> parts=new ArrayList<>();
        for(int i=0;i<6;i++){
            double h=4+i*5.5;
            parts.add(PhotonFx.Part.at(PhotonFx.SHELL,4.15,.9,.3,FEATHER,.34,1.15,AMBER,GOLD,true,life,26,4,-.30,.35)
                .move(0,h,0).turn(i*37,0));
        }
        // ③ 经文：柱内向上流转，三套字符轮换
        for(int i=0;i<6;i++){
            double h=2+i*5.5;
            parts.add(PhotonFx.Part.at(PhotonFx.SPHERE,3.7,1,.62,rune(i),0,0,AMBER,GOLD,true,life,12,4,.24,.42)
                .move(0,h,0));
        }
        // ④ 柱脚外溢的金色雾，接住地面法阵
        parts.add(PhotonFx.Part.at(PhotonFx.RING,6.4,.9,.7,GLOW,0,0,0x44FFE9A8,GOLD,true,life,14,10,.05,.25).move(0,.12,0));
        add(KurisuSkills.HORN,V_PILLAR,PhotonFx.parts(at,parts.toArray(new PhotonFx.Part[0])),life,owner,false);
        // ⑤ 地面法阵的粒子层（贴地圆环 ×2 + 升起符文）；自绘法阵在 KurisuHornRender 里
        add(KurisuSkills.HORN,V_PILLAR,PhotonFx.parts(at,
            PhotonFx.Part.at(PhotonFx.RING,10.5,.35,.5,GLOW,0,0,GOLD,GOLD,true,life,16,10,.02,.35).move(0,.06,0),
            PhotonFx.Part.at(PhotonFx.RING,13.6,.3,.45,GLOW,0,0,AMBER,GOLD,true,life,16,8,.02,-.22).move(0,.06,0),
            PhotonFx.Part.at(PhotonFx.SPHERE,9.5,1,.5,rune(0),0,0,AMBER,GOLD,true,life,14,4,.55,.1).move(0,.6,0),
            PhotonFx.Part.at(PhotonFx.SPHERE,9.5,1,.5,rune(2),0,0,GOLD,GOLD,true,life,14,3,.42,-.1).move(0,.6,0)
        ),life,owner,false);
        // ⑥ 落地冲击波
        add(KurisuSkills.HORN,V_PILLAR,PhotonFx.shockwave(at.add(0,.1,0),0xFFFFD77A,14,22),30,owner,false);
        add(KurisuSkills.HORN,V_PILLAR,PhotonFx.ring(at.add(0,.08,0),5.2,.9,WHITE,GOLD,60,18,10),24,owner,false);
    }

    // ---------------------------------------------------------------- 其余 10 式沿用旧画法
    private static void legacy(int skill,Vec3 at,Vec3 dir,int d){
        int life=d;
        switch(skill){
            case KurisuSkills.BAPTISM -> {
                add(skill,0,PhotonFx.beam(at.add(0,26,0),at,0.7,8),12,0,false);
                add(skill,0,PhotonFx.shell(at,4,0x66FFF6D8,0,20),24,0,false);
            }
            case KurisuSkills.FLAME -> {
                add(skill,0,PhotonFx.ring(at.add(0,.15,0),3.2,.55,GOLD,0,26,160,16),170,0,false);
                add(skill,0,PhotonFx.sphere(at.add(0,.6,0),2.2,.55,WHITE,0,GLOW,true,160,12,20,.5),170,0,false);
            }
            case KurisuSkills.SPEARS -> {
                for(int i=0;i<4;i++){
                    double a=i*Math.PI/2, r=1.6;
                    Vec3 base=at.add(Math.cos(a)*r,0,Math.sin(a)*r);
                    add(skill,0,PhotonFx.beam(base.add(0,20,0),base,0.35,6),10,0,false);
                }
                add(skill,0,PhotonFx.ring(at,2.6,.4,GOLD,0,30,20,12),24,0,false);
            }
            case KurisuSkills.GATE -> {
                add(skill,0,PhotonFx.ring(at,2.0,.6,GOLD,0xFFD77A,32,150,14),160,0,false);
                add(skill,0,PhotonFx.ring(at.add(0,.2,0),1.4,.35,WHITE,0,24,150,10),160,0,false);
            }
            case KurisuSkills.SWORD -> {
                add(skill,0,PhotonFx.sphere(at,.5,.5,WHITE,0xFFD77A,GLOW,true,260,8,18,.6),270,0,false);
                add(skill,0,PhotonFx.beam(at,at.add(dir.scale(2.2)),0.25,260),270,0,false);
            }
            case KurisuSkills.THUNDER -> {
                add(skill,0,PhotonFx.beam(at.add(0,30,0),at,2.0,8),12,0,false);
                add(skill,0,PhotonFx.shockwave(at,0xFFFFE9A8,5,14),18,0,false);
                add(skill,0,PhotonFx.ring(at,3.4,.6,GOLD,0xFFD77A,40,24,12),26,0,false);
            }
            case KurisuSkills.STORM -> {
                add(skill,0,PhotonFx.shell(at,7,0x55FFF6D8,0,30),34,0,false);
                add(skill,0,PhotonFx.ring(at,7.5,.7,GOLD,0xFFD77A,52,26,16),30,0,false);
            }
            case KurisuSkills.EXECUTE -> {
                add(skill,0,PhotonFx.sphere(at,.8,.9,WHITE,0xFFD77A,GLOW,true,10,6,26,.4),14,0,false);
                add(skill,0,PhotonFx.shockwave(at,0xFFFFD77A,3,12),16,0,false);
            }
            case KurisuSkills.SUPPER -> {
                for(int i=0;i<3;i++){
                    double a=i*Math.PI*2/3, r=5;
                    Vec3 base=at.add(Math.cos(a)*r,0,Math.sin(a)*r);
                    add(skill,0,PhotonFx.beam(base.add(0,30,0),base,1.4,8),12,0,false);
                }
                add(skill,0,PhotonFx.shockwave(at,0xFFFFD77A,7,16),18,0,false);
            }
            case KurisuSkills.DOMAIN -> {
                int dur=Math.max(60,Math.min(600,life));
                add(skill,0,PhotonFx.shell(at,22,0x44FFF6D8,0,dur),dur,0,false);
                add(skill,0,PhotonFx.ring(at,20,.7,WHITE,0xFFF6D8,36,dur,14),dur,0,false);
            }
            default -> {}
        }
        add(skill,0,PhotonFx.sphere(at,.35,.45,WHITE,0xFFD77A,GLOW,true,8,5,14,0),10,0,false);
    }

    // ---------------------------------------------------------------- 生命周期
    public static void stop(int skill){
        ACTIVE.removeIf(a->{
            if(a.skill()!=skill)return false;
            PhotonFx.stop(a.handle());
            return true;
        });
    }
    public static void stop(int skill,int variant){
        ACTIVE.removeIf(a->{
            if(a.skill()!=skill||a.variant()!=variant)return false;
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
        var level=Minecraft.getInstance().level;
        ACTIVE.removeIf(a->{
            if(a.end()<=now){PhotonFx.stop(a.handle());return true;}
            if(a.follow()&&a.owner()>0&&level!=null){
                Entity e=level.getEntity(a.owner());
                if(e!=null)PhotonFx.move(a.handle(),e.position().add(0,.1,0));
            }
            return false;
        });
    }
    private static long now(){var l=Minecraft.getInstance().level;return l==null?0:l.getGameTime();}
    private KurisuPhoton(){}
}
