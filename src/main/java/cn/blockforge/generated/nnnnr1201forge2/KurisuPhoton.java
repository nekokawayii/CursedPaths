package cn.blockforge.generated.nnnnr1201forge2;

import character.kurisu.KurisuSkills;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/**
 * 来栖华的 Photon 时间轴：每个技能 = 一个 FX，内部用多个带 delay 的发射器串成
 * 「起手 → 主体 → 余波」；触发时只 emmit 一次，结束/中断时统一 destroy。
 */
public final class KurisuPhoton {
    private static final String GLOW="textures/effect/glow.png", DUST="textures/effect/dust.png", STREAK="textures/effect/streak.png";
    private static final int GOLD=0xFFFFD77A, WHITE=0xFFFFF6D8, DEEP=0x66B98A2A, FAINT=0x33FFF6D8;
    private record Active(PhotonFx.Handle handle,int skill,long end){}
    private static final List<Active> ACTIVE=new ArrayList<>();

    public static void play(int skill,Vec3 at,Vec3 dir,int life){
        stop(skill);
        long end=now()+Math.max(10,life);
        PhotonFx.Handle h=switch(skill){
            case KurisuSkills.BAPTISM -> baptism(at);
            case KurisuSkills.FLAME -> flame(at);
            case KurisuSkills.SPEARS -> spears(at);
            case KurisuSkills.GATE -> gate(at);
            case KurisuSkills.SWORD -> sword(at);
            case KurisuSkills.THUNDER -> thunder(at);
            case KurisuSkills.STORM -> storm(at);
            case KurisuSkills.EXECUTE -> execute(at);
            case KurisuSkills.HORN -> horn(at);
            case KurisuSkills.SUPPER -> supper(at);
            case KurisuSkills.DOMAIN -> domain(at,life);
            default -> null;
        };
        if(h!=null)ACTIVE.add(new Active(h,skill,end));
    }

    // 1 剥夺洗礼：天降金线 → 命中闪光 → 余波环
    private static PhotonFx.Handle baptism(Vec3 at){
        return PhotonFx.staged(at,
            PhotonFx.Stage.of(PhotonFx.CYLINDER,.18,1.1,WHITE,0xFFD77A,GLOW,true,8,6,26,0,0,0).along(new Vec3(0,-1,0)).beam(46),
            PhotonFx.Stage.of(PhotonFx.DOT,.3,1.6,WHITE,0xFFD77A,GLOW,true,10,5,18,0,0,4),
            PhotonFx.Stage.of(PhotonFx.RING,1.2,.5,GOLD,0xFFD77A,GLOW,true,12,10,40,6,0,6),
            PhotonFx.Stage.of(PhotonFx.SHELL,2.4,.4,GOLD,0,DUST,false,26,18,24,2.2,.8,8));
    }
    // 2 赎罪之炎：贴地金环 → 上升火舌 → 长期低焰
    private static PhotonFx.Handle flame(Vec3 at){
        return PhotonFx.staged(at.add(0,.15,0),
            PhotonFx.Stage.of(PhotonFx.RING,3.4,.6,GOLD,0xFFD77A,GLOW,true,30,16,60,1.6,0,0),
            PhotonFx.Stage.of(PhotonFx.CYLINDER,4.6,.7,WHITE,0xFFD77A,GLOW,true,200,20,34,2.2,.5,10),
            PhotonFx.Stage.of(PhotonFx.SHELL,6.0,.5,GOLD,0,DUST,false,200,22,20,1.1,.9,20),
            PhotonFx.Stage.of(PhotonFx.DOT,5.5,1.2,DEEP,0,DUST,false,200,25,14,1.4,.3,30));
    }
    // 3 圣光穿刺：数根光枪依次落下 → 落点闪 → 钉地环
    private static PhotonFx.Handle spears(Vec3 at){
        List<PhotonFx.Stage> list=new ArrayList<>();
        for(int i=0;i<6;i++){
            double a=i*Math.PI/3, r=1.2+(i%2)*.9;
            Vec3 off=new Vec3(Math.cos(a)*r,0,Math.sin(a)*r);
            list.add(PhotonFx.Stage.of(PhotonFx.CYLINDER,.12,.7,WHITE,0xFFD77A,GLOW,true,10,5,20,0,0,i*2).at(off).along(new Vec3(0,-1,0)).beam(26));
            list.add(PhotonFx.Stage.of(PhotonFx.RING,.8,.35,GOLD,0xFFD77A,GLOW,true,14,8,30,4,0,4+i*2).at(off));
        }
        list.add(PhotonFx.Stage.of(PhotonFx.SHELL,3.2,.4,GOLD,0,DUST,false,30,20,22,1.8,.6,10));
        return PhotonFx.staged(at,list.toArray(new PhotonFx.Stage[0]));
    }
    // 4 天国之门：旋转光环 → 第二圈脉冲 → 淡金穹壳
    private static PhotonFx.Handle gate(Vec3 at){
        return PhotonFx.staged(at,
            PhotonFx.Stage.of(PhotonFx.RING,2.2,.7,GOLD,0xFFD77A,GLOW,true,160,14,46,0,2.4,0),
            PhotonFx.Stage.of(PhotonFx.RING,1.6,.4,WHITE,0xFFD77A,GLOW,true,160,10,30,0,-1.8,12),
            PhotonFx.Stage.of(PhotonFx.SHELL,2.6,.3,FAINT,0,DUST,false,160,16,18,0,.7,6));
    }
    // 5 裁决光剑：剑身光核 + 环绕光丝 + 上下扫描闪
    private static PhotonFx.Handle sword(Vec3 at){
        return PhotonFx.staged(at,
            PhotonFx.Stage.of(PhotonFx.DOT,.35,1.0,WHITE,0xFFD77A,GLOW,true,300,8,26,0,0,0),
            PhotonFx.Stage.of(PhotonFx.CYLINDER,1.3,.4,WHITE,0xFFD77A,GLOW,true,300,7,34,0,0,0).along(new Vec3(0,1,0)).beam(3.2),
            PhotonFx.Stage.of(PhotonFx.SHELL,1.1,.3,GOLD,0xFFD77A,STREAK,true,300,9,30,0,3.2,6));
    }
    // 6 神罚落雷：天空光柱 → 落点爆闪 → 地面冲击环 → 余留电弧
    private static PhotonFx.Handle thunder(Vec3 at){
        return PhotonFx.staged(at,
            PhotonFx.Stage.of(PhotonFx.CYLINDER,.5,2.6,WHITE,0xFFD77A,GLOW,true,10,7,60,0,0,0).along(new Vec3(0,-1,0)).beam(52),
            PhotonFx.Stage.of(PhotonFx.DOT,.6,2.4,WHITE,0xFFD77A,GLOW,true,8,5,40,0,0,2),
            PhotonFx.Stage.of(PhotonFx.RING,4.2,.8,GOLD,0xFFD77A,GLOW,true,16,12,60,8,0,4),
            PhotonFx.Stage.of(PhotonFx.SHELL,3.4,.4,GOLD,0,DUST,false,40,26,26,1.2,1.4,8));
    }
    // 7 净化风暴：通天金柱（自转+上升）→ 外扩环 → 落尘
    private static PhotonFx.Handle storm(Vec3 at){
        return PhotonFx.staged(at,
            PhotonFx.Stage.of(PhotonFx.CYLINDER,7.5,1.1,WHITE,0xFFD77A,GLOW,true,26,18,64,2.6,2.4,0),
            PhotonFx.Stage.of(PhotonFx.RING,8.2,.9,GOLD,0xFFD77A,GLOW,true,26,16,70,9,0,2),
            PhotonFx.Stage.of(PhotonFx.RING,5.0,.5,WHITE,0xFFD77A,STREAK,true,30,14,50,5,-3.0,4),
            PhotonFx.Stage.of(PhotonFx.SHELL,9.0,.5,GOLD,0,DUST,false,46,24,26,1.4,.8,8));
    }
    // 8 破魔重击：向内收束 → 命中爆点 → 扩散余波
    private static PhotonFx.Handle execute(Vec3 at){
        return PhotonFx.staged(at,
            PhotonFx.Stage.of(PhotonFx.SHELL,3.6,.6,WHITE,0xFFD77A,GLOW,true,10,6,46,-3.0,0,0),
            PhotonFx.Stage.of(PhotonFx.DOT,.5,2.2,WHITE,0xFFD77A,GLOW,true,8,5,30,0,0,6),
            PhotonFx.Stage.of(PhotonFx.RING,3.0,.8,GOLD,0xFFD77A,GLOW,true,18,12,50,7,0,8),
            PhotonFx.Stage.of(PhotonFx.SHELL,4.2,.4,GOLD,0,DUST,false,34,20,20,1.0,.9,12));
    }
    // 9 邪去悔天梯：号角声波环 → 天裂光柱 → 羽毛经带 → 余晖
    private static PhotonFx.Handle horn(Vec3 at){
        return PhotonFx.staged(at,
            PhotonFx.Stage.of(PhotonFx.RING,2.0,.5,WHITE,0xFFD77A,GLOW,true,20,10,50,8,0,0),
            PhotonFx.Stage.of(PhotonFx.RING,3.2,.4,GOLD,0xFFD77A,GLOW,true,20,10,40,11,0,6),
            PhotonFx.Stage.of(PhotonFx.CYLINDER,4.8,1.6,WHITE,0xFFD77A,GLOW,true,64,12,70,0,0,4).along(new Vec3(0,-1,0)).beam(70),
            PhotonFx.Stage.of(PhotonFx.CYLINDER,4.4,.5,GOLD,0xFFD77A,STREAK,true,64,10,40,-2.0,2.8,10),
            PhotonFx.Stage.of(PhotonFx.SHELL,5.6,.4,GOLD,0,DUST,false,90,24,22,1.2,.8,16));
    }
    // 10 最后的晚餐：蓄力收束 → 三道天罚光柱 → 全场余波
    private static PhotonFx.Handle supper(Vec3 at){
        List<PhotonFx.Stage> list=new ArrayList<>();
        list.add(PhotonFx.Stage.of(PhotonFx.SHELL,6.0,.9,WHITE,0xFFD77A,GLOW,true,60,26,50,-1.8,.4,0));
        for(int i=0;i<3;i++){
            double a=i*Math.PI*2/3, r=6;
            Vec3 off=new Vec3(Math.cos(a)*r,0,Math.sin(a)*r);
            list.add(PhotonFx.Stage.of(PhotonFx.CYLINDER,.8,2.0,WHITE,0xFFD77A,GLOW,true,14,8,40,0,0,60+i*6).at(off).along(new Vec3(0,-1,0)).beam(60));
            list.add(PhotonFx.Stage.of(PhotonFx.RING,2.4,.6,GOLD,0xFFD77A,GLOW,true,20,12,40,6,0,62+i*6).at(off));
        }
        list.add(PhotonFx.Stage.of(PhotonFx.SHELL,10.0,.6,GOLD,0,DUST,false,120,30,26,1.0,.6,70));
        return PhotonFx.staged(at,list.toArray(new PhotonFx.Stage[0]));
    }
    // 11 天国之门领域：纯白穹顶 + 反向旋转十字光环 + 缓慢光尘
    private static PhotonFx.Handle domain(Vec3 at,int life){
        int dur=Math.max(40,life);
        return PhotonFx.staged(at,
            PhotonFx.Stage.of(PhotonFx.SHELL,30,.8,FAINT,0,DUST,false,dur,26,20,.4,.2,0),
            PhotonFx.Stage.of(PhotonFx.RING,28,.6,WHITE,0xFFF6D8,GLOW,true,dur,18,40,0,1.2,6),
            PhotonFx.Stage.of(PhotonFx.RING,24,.4,GOLD,0xFFD77A,GLOW,true,dur,16,32,0,-1.6,12),
            PhotonFx.Stage.of(PhotonFx.CYLINDER,26,.7,WHITE,0xFFFFF6D8,GLOW,true,dur,20,24,0,0,18));
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
    /** 每帧清理到期句柄；玩家换维度/死亡时由 KurisuVisuals 调 stopAll。 */
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
