package character.kurisu;

import cn.blockforge.generated.nnnnr1201forge2.CombatSettings;
import cn.blockforge.generated.nnnnr1201forge2.Network;
import cn.blockforge.generated.nnnnr1201forge2.SkillSounds;
import cn.blockforge.generated.nnnnr1201forge2.Skills;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import java.util.*;

/**
 * 邪去悔天梯 · 号角（短按 / 长按两条路，共用一个技能号与冷却）。
 *
 * <table>
 *   <tr><th>短按</th><th>长按（≥0.5 秒）</th></tr>
 *   <tr>
 *     <td>举角 → 松开即射出一道**持续 3 秒的黄色光柱**（最远 28 格，柱内符文飘飞），
 *         每 0.5 秒结算一次。</td>
 *     <td>按住期间**背后浮现巨大天使羽翼虚影、自身被金光包裹**：移速 -50%、其他术式全部封锁、不能普攻；
 *         松手才真正吹响号角：天空裂开，**半径 5 格的通天金色光柱**砸在准星落点 6 秒，
 *         柱内金色羽毛与经文流转，被照到的敌人术式化作黑烟蒸发，地面铺满金色法阵。</td>
 *   </tr>
 * </table>
 *
 * 服务端只负责状态与结算；画面由 {@code KurisuPhoton}（粒子）与 {@code KurisuHornRender}（羽翼/法阵/天裂）表现。
 */
public final class KurisuHorn {
    /** 按住超过 10 刻（0.5 秒）判定为长按；低于这个值松手 = 短按光柱。 */
    public static final int HOLD_TICKS = 10;
    /** 短按光柱持续 60 刻（3 秒）。 */
    public static final int TAP_TICKS = 60;
    /** 长按圣光柱持续 120 刻（6 秒）。 */
    public static final int PILLAR_TICKS = 120;
    public static final double TAP_LENGTH = 28, TAP_RADIUS = 1.15;
    /** 圣光柱：半径 5 格（视觉半径 4.8 / 伤害半径 5），高度 16 格覆盖地面生物。 */
    public static final double PILLAR_RADIUS = 5.0, PILLAR_HEIGHT = 16;
    /** 结算节拍：每 10 刻一次（0.5 秒）。 */
    public static final int TICK_STEP = 10;
    /** 短按：每跳伤害 = max(10, 最大生命 6%)；长按：max(12, 最大生命 7%)。 */
    public static final float TAP_DAMAGE_BASE = 10, TAP_DAMAGE_RATIO = 0.06f;
    public static final float PILLAR_DAMAGE_BASE = 12, PILLAR_DAMAGE_RATIO = 0.07f;
    private static final UUID SLOW_ID = UUID.fromString("7b1c4c1e-1d4a-4f7e-9d2a-0f3a6f0f7a11");
    private static final String SLOW_NAME = "kurisu_horn_channel";

    /** 按住阶段：按下时刻 + 是否已经越过长按阈值。 */
    private static final class Hold {
        final ServerPlayer p; final ServerLevel level; final long start;
        boolean committed; long nextFx;
        Hold(ServerPlayer p){this.p=p;this.level=p.serverLevel();this.start=this.nextFx=p.level().getGameTime();}
    }
    private static final Map<UUID,Hold> HOLDS=new HashMap<>();
    /** 松手后的持续光柱（短按）。 */
    private static final class Tap {
        final ServerPlayer p; final ServerLevel level; long end; long nextFx, nextHit;
        Tap(ServerPlayer p,long end){this.p=p;this.level=p.serverLevel();this.end=end;this.nextFx=this.nextHit=p.level().getGameTime();}
    }
    /** 松手后的圣光柱（长按）。 */
    private static final class Pillar {
        final ServerPlayer p; final ServerLevel level; final Vec3 center; long end;
        long nextHit; int totalHits; final Map<UUID,Long> burned=new HashMap<>();
        Pillar(ServerPlayer p,Vec3 center,long end){this.p=p;this.level=p.serverLevel();this.center=center;this.end=end;this.nextHit=p.level().getGameTime();}
    }
    private static final List<Tap> TAPS=new ArrayList<>();
    private static final List<Pillar> PILLARS=new ArrayList<>();
    /** 松手时告诉 {@link KurisuCombat#cast} 这次是短按(0)还是长按(1)。施法在同一刻同步执行，读完即清。 */
    private static int pendingKind;
    private static boolean pending;

    public static int consumeReleaseKind(){int v=pendingKind;pendingKind=0;pending=false;return v;}
    public static boolean channeling(net.minecraft.world.entity.player.Player p){
        return p!=null&&HOLDS.containsKey(p.getUUID());
    }
    /** 正在长按咏唱（已经越过阈值、羽翼已展开）。 */
    public static boolean channelCommitted(net.minecraft.world.entity.player.Player p){
        Hold h=p==null?null:HOLDS.get(p.getUUID());
        return h!=null&&h.committed;
    }

    // ------------------------------------------------------------ 输入：按下 / 心跳 / 松手 / 取消
    /** phase 0：按下。只记状态与起手画面，不扣咒力（短按长按共用一次扣费，交给 CharacterCasting）。 */
    public static void press(ServerPlayer p){
        if(HOLDS.containsKey(p.getUUID()))return;
        if(!(p.level() instanceof ServerLevel level))return;
        HOLDS.put(p.getUUID(),new Hold(p));
        Vec3 eye=p.getEyePosition(),dir=p.getLookAngle();
        Network.send(level,new Network.Effect(KurisuFx.PLAY,p.getId(),eye,dir,KurisuSkills.HORN,16,KurisuHorn.V_READY));
        level.playSound(null,p.blockPosition(),SoundEvents.RAID_HORN.get(),SoundSource.PLAYERS,0.6f,1.45f);
        p.displayClientMessage(Component.literal("邪去悔天梯 · 举角：松开=光柱，按住=圣光降临")
            .withStyle(net.minecraft.ChatFormatting.GOLD),true);
    }
    /** phase 1：按住心跳（客户端每 10 刻一次）。越过阈值那一刻展开羽翼并锁技能。 */
    public static void heartbeat(ServerPlayer p){
        Hold h=HOLDS.get(p.getUUID());
        if(h==null)return;
        long now=p.level().getGameTime();
        if(!h.committed){
            if(now-h.start>=HOLD_TICKS)commit(h,now);
            else if(now>=h.nextFx){
                h.nextFx=now+8;
                Network.send(h.level,new Network.Effect(KurisuFx.PLAY,p.getId(),p.getEyePosition(),p.getLookAngle(),KurisuSkills.HORN,14,KurisuHorn.V_READY));
            }
            return;
        }
        // 咏唱中：羽翼画面每 10 刻续一次（句柄跟随玩家，客户端不重建）
        if(now>=h.nextFx){
            h.nextFx=now+8;
            Network.send(h.level,new Network.Effect(KurisuFx.WINGS,p.getId(),p.position(),Vec3.ZERO,0,18,0));
            Network.send(h.level,new Network.Effect(KurisuFx.PLAY,p.getId(),p.position(),Vec3.ZERO,KurisuSkills.HORN,18,KurisuHorn.V_WINGS));
        }
    }
    private static void commit(Hold h,long now){
        ServerPlayer p=h.p;
        h.committed=true;h.nextFx=now;
        slow(p,true);
        Network.send(h.level,new Network.Effect(KurisuFx.WINGS,p.getId(),p.position(),Vec3.ZERO,0,18,0));
        Network.send(h.level,new Network.Effect(KurisuFx.PLAY,p.getId(),p.position(),Vec3.ZERO,KurisuSkills.HORN,18,KurisuHorn.V_WINGS));
        h.level.playSound(null,p.blockPosition(),SoundEvents.BEACON_ACTIVATE,SoundSource.PLAYERS,1.0f,1.35f);
        p.displayClientMessage(Component.literal("天使羽翼展开 · 移速 -50% · 其他术式封锁（松手吹响号角）")
            .withStyle(net.minecraft.ChatFormatting.GOLD),true);
    }
    /** phase 2：松手。短按 → 光柱；长按 → 圣光柱（真正走 CharacterCasting 扣费/冷却/门禁）。 */
    public static void release(ServerPlayer p){
        Hold h=HOLDS.remove(p.getUUID());
        if(h==null)return;
        boolean committed=h.committed;
        slow(p,false);
        if(!(p.level() instanceof ServerLevel level))return;
        Network.send(level,new Network.Effect(KurisuFx.STOP,p.getId(),Vec3.ZERO,Vec3.ZERO,KurisuSkills.HORN,0,0));
        pendingKind=committed?1:0;pending=true;
        try{
            character.CharacterCasting.cast(p,KurisuSkills.HORN);
        }finally{
            pending=false;pendingKind=0;
        }
    }
    /** phase 3 / 下线 / 死亡：什么都不放，只收画面与减速。 */
    public static void cancel(ServerPlayer p,String why){
        Hold h=HOLDS.remove(p.getUUID());
        if(h==null)return;
        slow(p,false);
        if(p.level() instanceof ServerLevel level)
            Network.send(level,new Network.Effect(KurisuFx.STOP,p.getId(),Vec3.ZERO,Vec3.ZERO,KurisuSkills.HORN,0,0));
        p.displayClientMessage(Component.literal("邪去悔天梯 · 已收招（"+why+"）").withStyle(net.minecraft.ChatFormatting.GRAY),true);
    }

    // ------------------------------------------------------------ 施法结算（由 KurisuCombat.cast 调用）
    /** 短按：一道沿准星的持续黄色光柱，3 秒内每半秒结算一次。 */
    public static void tap(ServerPlayer p){
        if(!(p.level() instanceof ServerLevel level))return;
        TAPS.add(new Tap(p,level.getGameTime()+TAP_TICKS));
        level.playSound(null,p.blockPosition(),SoundEvents.RAID_HORN.get(),SoundSource.PLAYERS,1.5f,0.95f);
        level.playSound(null,p.blockPosition(),SoundEvents.BEACON_ACTIVATE,SoundSource.PLAYERS,0.8f,1.7f);
        p.displayClientMessage(Component.literal("邪去悔天梯 · 号角光柱（3 秒）").withStyle(net.minecraft.ChatFormatting.GOLD),true);
    }
    /** 长按松手：天空裂开 → 半径 5 格通天光柱 → 法阵 + 圣歌，6 秒内每半秒结算。 */
    public static void pillar(ServerPlayer p){
        if(!(p.level() instanceof ServerLevel level))return;
        Vec3 center=groundPoint(p);
        PILLARS.add(new Pillar(p,center,level.getGameTime()+PILLAR_TICKS));
        if(spawnDummies){
            spawnDummies=false;
            for(int i=0;i<2;i++){
                var z=new net.minecraft.world.entity.monster.Zombie(level);
                z.moveTo(center.x+(i==0?1.4:-1.4),center.y+0.1,center.z+0.8,0,0);
                z.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,200,1));
                level.addFreshEntity(z);
            }
            log("（自检）已把 2 只带速度增益的测试僵尸放到柱心：用来验证伤害与术式蒸发");
        }
        Network.send(level,new Network.Effect(KurisuFx.PILLAR,p.getId(),center,Vec3.ZERO,0,PILLAR_TICKS,0));
        Network.send(level,new Network.Effect(KurisuFx.PLAY,p.getId(),center,Vec3.ZERO,KurisuSkills.HORN,PILLAR_TICKS,KurisuHorn.V_PILLAR));
        level.playSound(null,net.minecraft.core.BlockPos.containing(center),SoundEvents.RAID_HORN.get(),SoundSource.PLAYERS,2.4f,0.7f);
        level.playSound(null,net.minecraft.core.BlockPos.containing(center),SoundEvents.BEACON_ACTIVATE,SoundSource.PLAYERS,2.5f,1.1f);
        level.playSound(null,net.minecraft.core.BlockPos.containing(center),SoundEvents.LIGHTNING_BOLT_THUNDER,SoundSource.PLAYERS,1.4f,1.6f);
        SkillSounds.music(level,center,"kurisu_hymn",1.3f);   // 宏大圣歌咏唱
        p.displayClientMessage(Component.literal("邪去悔天梯 · 圣光降临（半径 5 · 6 秒 · 术式蒸发）").withStyle(net.minecraft.ChatFormatting.GOLD),false);
    }
    private static Vec3 groundPoint(ServerPlayer p){
        Vec3 eye=p.getEyePosition(),end=eye.add(p.getLookAngle().scale(20));
        var hit=p.level().clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        if(hit.getType()!=HitResult.Type.MISS)return hit.getLocation();
        // 打空时落到准星正下方的地面，光柱才不会悬在半空
        Vec3 flat=new Vec3(end.x,eye.y,end.z);
        var down=p.level().clip(new ClipContext(flat,flat.add(0,-24,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        return down.getType()==HitResult.Type.MISS?end:down.getLocation();
    }

    // ------------------------------------------------------------ 每刻推进
    public static void tick(ServerPlayer p){
        long now=p.level().getGameTime();
        autoTick(p,now);
        // 按住阶段：心跳丢了也要自己判超时（切界面/失焦时客户端会发 phase 3，这里只兜底）
        Hold h=HOLDS.get(p.getUUID());
        if(h!=null){
            if(!p.isAlive()||p.level()!=h.level){cancel(p,"状态失效");}
            else if(!h.committed&&now-h.start>=HOLD_TICKS+4)commit(h,now);
            else if(!h.committed&&(p.isUsingItem()||p.isSpectator()))cancel(p,"动作被打断");
        }
        TAPS.removeIf(t->{
            if(t.p!=p)return false;
            if(t.end<=now||!p.isAlive())return true;
            if(now>=t.nextFx){
                t.nextFx=now+8;
                Network.send(t.level,new Network.Effect(KurisuFx.PLAY,p.getId(),p.getEyePosition(),p.getLookAngle(),KurisuSkills.HORN,14,KurisuHorn.V_TAP));
            }
            if(now>=t.nextHit){
                t.nextHit=now+TICK_STEP;
                tapHit(t);
            }
            return false;
        });
        PILLARS.removeIf(c->{
            if(c.p!=p)return false;
            long t=c.end-now;
            if(t<=0||!p.isAlive())return true;
            // 天裂的余响：末段补一次扩散冲击
            if(now>=c.nextHit){
                c.nextHit=now+TICK_STEP;
                pillarHit(c,now,t<=TICK_STEP);
            }
            return false;
        });
    }

    private static void tapHit(Tap t){
        ServerPlayer p=t.p;
        Vec3 eye=p.getEyePosition(),dir=p.getLookAngle().normalize();
        Vec3 end=eye.add(dir.scale(TAP_LENGTH));
        for(LivingEntity target:t.level.getEntitiesOfClass(LivingEntity.class,new AABB(eye,end).inflate(TAP_RADIUS+2),
                e->e!=p&&e.isAlive()&&CombatSettings.canHit(p,e))){
            Vec3 delta=target.getBoundingBox().getCenter().subtract(eye);
            double along=delta.dot(dir);
            if(along<-0.5||along>eye.distanceTo(end))continue;
            if(delta.subtract(dir.scale(along)).length()>TAP_RADIUS+Math.max(.5,target.getBbWidth()*.5))continue;
            target.invulnerableTime=0;
            CombatSettings.hurt(p,target,p.damageSources().indirectMagic(p,p),
                Math.max(TAP_DAMAGE_BASE,target.getMaxHealth()*TAP_DAMAGE_RATIO));
            t.level.sendParticles(ParticleTypes.END_ROD,target.getX(),target.getY()+target.getBbHeight()*.6,target.getZ(),6,.3,.4,.3,.02);
        }
        // 柱体本身在地面/方块上的落点也烧一圈
        Vec3 ground=end;
        var hit=t.level.clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        if(hit.getType()!=HitResult.Type.MISS)ground=hit.getLocation();
        t.level.sendParticles(ParticleTypes.END_ROD,ground.x,ground.y+.2,ground.z,4,.5,.1,.5,.01);
    }

    private static void pillarHit(Pillar c,long now,boolean last){
        ServerPlayer p=c.p;
        int hits=0;
        AABB box=new AABB(c.center.x-PILLAR_RADIUS,c.center.y-1,c.center.z-PILLAR_RADIUS,
            c.center.x+PILLAR_RADIUS,c.center.y+PILLAR_HEIGHT,c.center.z+PILLAR_RADIUS);
        for(LivingEntity target:c.level.getEntitiesOfClass(LivingEntity.class,box,
                e->e!=p&&e.isAlive()&&CombatSettings.canHit(p,e))){
            target.invulnerableTime=0;
            CombatSettings.hurt(p,target,p.damageSources().indirectMagic(p,p),
                Math.max(PILLAR_DAMAGE_BASE,target.getMaxHealth()*PILLAR_DAMAGE_RATIO));
            hits++;
            c.totalHits++;
            // 术式蒸发：每 40 刻最多一次，避免黑烟刷屏
            Long until=c.burned.get(target.getUUID());
            if(until==null||until<=now){
                c.burned.put(target.getUUID(),now+40);
                evaporate(c,target);
            }
        }
        if(last){
            log("圣光柱结束：累计命中 "+c.totalHits+" 次（半径 "+PILLAR_RADIUS+" 持续 "+PILLAR_TICKS+" 刻）");
            c.level.sendParticles(ParticleTypes.FLASH,c.center.x,c.center.y+1.2,c.center.z,3,.6,.6,.6,0);
            c.level.playSound(null,net.minecraft.core.BlockPos.containing(c.center),SoundEvents.BEACON_DEACTIVATE,SoundSource.PLAYERS,1.6f,1.3f);
        }
    }

    /** 被圣光照到的敌人：生得术式与身上效果化作黑烟蒸发（中断施术 + 清空状态效果）。 */
    private static void evaporate(Pillar c,LivingEntity target){
        boolean casting=target.getPersistentData().getLong("NpcChannelUntil")>target.level().getGameTime()
            ||target.getPersistentData().getLong("FinaleCastUntil")>target.level().getGameTime()
            ||target instanceof ServerPlayer sp&&(Skills.sealing(sp)||cn.blockforge.generated.nnnnr1201forge2.BloodArts.casting(sp));
        boolean buffed=!target.getActiveEffects().isEmpty();
        if(!casting&&!buffed)return;
        target.getPersistentData().putLong("NpcChannelUntil",0);
        target.getPersistentData().putLong("FinaleCastUntil",0);
        if(target instanceof ServerPlayer sp){
            Skills.cancelCharge(sp);
            cn.blockforge.generated.nnnnr1201forge2.BloodArts.cancel(sp);
            cn.blockforge.generated.nnnnr1201forge2.TechniqueInput.cancel(sp);
        }
        for(var eff:List.copyOf(target.getActiveEffects()))target.removeEffect(eff.getEffect());
        ServerLevel level=c.level;
        for(int i=0;i<20;i++){
            double a=level.random.nextDouble()*Math.PI*2,r=.2+level.random.nextDouble()*.7;
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                target.getX()+Math.cos(a)*r,target.getY()+.2+level.random.nextDouble()*target.getBbHeight(),
                target.getZ()+Math.sin(a)*r,1,0,.06,0,.01);
        }
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,target.getX(),target.getY()+target.getBbHeight()*.7,target.getZ(),8,.35,.5,.35,.02);
        level.playSound(null,target.blockPosition(),SoundEvents.FIRE_EXTINGUISH,SoundSource.PLAYERS,1.2f,.65f);
        if(target instanceof ServerPlayer sp)
            sp.displayClientMessage(Component.literal("圣光蒸发：术式与强化全部化为黑烟").withStyle(net.minecraft.ChatFormatting.GOLD),true);
    }

    // ------------------------------------------------------------ 移速 -50%（属性修饰符，精确 50%）
    private static void slow(ServerPlayer p,boolean on){
        try{
            var body=cn.blockforge.generated.nnnnr1201forge2.MaidBridge.body(p);
            var attr=body.getAttribute(Attributes.MOVEMENT_SPEED);
            if(attr==null)return;
            if(on){
                if(attr.getModifier(SLOW_ID)!=null)return;
                attr.addTransientModifier(new AttributeModifier(SLOW_ID,SLOW_NAME,-0.5,AttributeModifier.Operation.MULTIPLY_TOTAL));
            }else{
                if(attr.getModifier(SLOW_ID)!=null)attr.removeModifier(SLOW_ID);
            }
        }catch(Throwable ignored){}
    }
    public static boolean hasted(ServerPlayer p){
        var attr=cn.blockforge.generated.nnnnr1201forge2.MaidBridge.body(p).getAttribute(Attributes.MOVEMENT_SPEED);
        return attr!=null&&attr.getModifier(SLOW_ID)!=null;
    }

    // ------------------------------------------------------------ 开发自检：完整链路（/jjksp kurisuhorn auto）
    /** UUID → {按下刻, 心跳刻, 松手刻, 阶段}。仅开发自检用。 */
    private static final Map<UUID,long[]> AUTO=new HashMap<>();
    /** 自检开关：下一次圣光柱落地时在柱心放测试僵尸。 */
    private static boolean spawnDummies;
    private static void log(String msg){com.mojang.logging.LogUtils.getLogger().info("[号角自检] "+msg);}
    /**
     * 走**真实的按键链路**：直接把玩家切成来栖华、补满咒力，然后依次执行
     * 按下 → 8 刻后心跳（越过阈值、展开羽翼）→ 30 刻后松手（吹响号角）。
     * 三个入口与客户端 phase 0/1/2 完全等价，所以扣费/冷却/封技/结算都是线上那套。
     */
    public static void autoTest(ServerPlayer p){
        var data=cn.blockforge.generated.nnnnr1201forge2.Curse.data(p);
        data.putString("SelectedCharacter","kurisu");
        cn.blockforge.generated.nnnnr1201forge2.Curse.apply(p);
        cn.blockforge.generated.nnnnr1201forge2.Curse.add(p,3000);
        cn.blockforge.generated.nnnnr1201forge2.Curse.sync(p);
        spawnDummies=true;   // 圣光柱落地那一刻在柱心放 2 只带增益的测试僵尸（见 pillar()）
        AUTO.put(p.getUUID(),new long[]{p.level().getGameTime(),0,0,0});
        log("开始：已把玩家切成来栖华并补满咒力（"+p.getScoreboardName()+"）");
    }
    private static void autoTick(ServerPlayer p,long now){
        long[] a=AUTO.get(p.getUUID());
        if(a==null)return;
        if(a[1]==0){a[1]=now+12;a[2]=now+44;press(p);log("phase0 按下（举角）");return;}
        if(a[3]==0&&now>=a[1]){a[3]=1;heartbeat(p);log("phase1 心跳：展开羽翼 移速-50%="+hasted(p));return;}
        if(a[3]==1&&now>=a[2]){
            a[3]=2;release(p);AUTO.remove(p.getUUID());
            log("phase2 松手：号角已释放 冷却中="
                +cn.blockforge.generated.nnnnr1201forge2.Curse.active(p,"CharacterSkillCooldown_"+KurisuSkills.HORN)
                +" 移速-50%="+hasted(p)+" 持续光柱="+(TAPS.size()+PILLARS.size()));
        }
    }

    public static void clear(){
        HOLDS.clear();TAPS.clear();PILLARS.clear();AUTO.clear();pending=false;pendingKind=0;
    }

    private KurisuHorn(){}
    /** 视觉变体号：与 {@code KurisuPhoton.V_*} 一一对应，避免两个包互相 import 常量表。 */
    public static final int V_READY=0, V_TAP=1, V_WINGS=2, V_PILLAR=3;
}
