package character.kurisu;

import cn.blockforge.generated.nnnnr1201forge2.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/**
 * 来栖华 · 邪去悔天梯。11 式 + 理智值系统。
 * 理智：净化/剥夺类技能会下降；<50 误伤队友；=0 失控 10 秒并无差别攻击（身边女仆被净化）。
 * 吃甜食恢复理智。
 */
@Mod.EventBusSubscriber(modid = "jujutsu_kaisen_cursed_paths")
public final class KurisuCombat {
    public static final String K_SANITY="KurisuSanity", K_BERSERK="KurisuBerserkUntil",
        K_NOHEAL="KurisuNoHealUntil", K_SWORD="KurisuSwordUntil", K_GATE="KurisuGateUntil",
        K_HORN="KurisuHornUntil";
    public static final int MAX_SANITY=100, DOMAIN_RADIUS=30, DOMAIN_TICKS=600, HORN_TICKS=60;

    private record Flame(ServerLevel level, Vec3 center, long end, ServerPlayer owner) {}
    private record Gate(ServerLevel level, Vec3 center, long end, ServerPlayer owner) {}
    private record Domain(ServerPlayer owner, ServerLevel level, Vec3 center, long end) {}
    private static final List<Flame> FLAMES=new ArrayList<>();
    private static final List<Gate> GATES=new ArrayList<>();
    private static final List<Domain> DOMAINS=new ArrayList<>();
    private static final Map<UUID,Long> HORNS=new HashMap<>();

    public static int sanity(Player p){var d=Curse.data(p);if(!d.contains(K_SANITY))return MAX_SANITY;return Math.max(0,Math.min(MAX_SANITY,d.getInt(K_SANITY)));}
    public static void addSanity(Player p,int delta){
        int v=Math.max(0,Math.min(MAX_SANITY,sanity(p)+delta));
        Curse.data(p).putInt(K_SANITY,v);
        if(delta<0)p.displayClientMessage(Component.literal("理智 -"+(-delta)+" · 当前 "+v).withStyle(net.minecraft.ChatFormatting.GOLD),true);
        else if(delta>0)p.displayClientMessage(Component.literal("理智 +"+delta+" · 当前 "+v).withStyle(net.minecraft.ChatFormatting.YELLOW),true);
        if(v<=0&&p instanceof ServerPlayer sp&&Curse.data(sp).getLong(K_BERSERK)<=sp.level().getGameTime()){
            Curse.data(sp).putLong(K_BERSERK,sp.level().getGameTime()+200);
            sp.displayClientMessage(Component.literal("理智归零：天使失控 10 秒！").withStyle(net.minecraft.ChatFormatting.RED),false);
        }
    }
    public static boolean berserk(Player p){return Curse.data(p).getLong(K_BERSERK)>p.level().getGameTime();}

    private static Vec3 aimPoint(ServerPlayer p,double dist){return p.getEyePosition().add(p.getLookAngle().scale(dist));}
    private static LivingEntity aimTarget(ServerPlayer p,double range){
        Vec3 eye=p.getEyePosition(),look=p.getLookAngle();
        LivingEntity best=null;double bestScore=range;
        for(var t:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(eye,eye).inflate(range),x->x!=p&&x.isAlive()&&canHurt(p,x))){
            Vec3 to=t.getBoundingBox().getCenter().subtract(eye);double d=to.length();
            if(d>range||d<.01)continue;
            double dot=to.normalize().dot(look);
            if(dot<.55)continue;
            double score=d*(2-dot);
            if(score<bestScore){bestScore=score;best=t;}
        }
        return best;
    }
    private static boolean canHurt(ServerPlayer p,LivingEntity t){
        if(t==null||t==p||!t.isAlive())return false;
        if(sanity(p)<50)return true;                     // 理智过低：开始误伤
        return CombatSettings.canHit(p,t);
    }
    private static List<LivingEntity> around(ServerPlayer p,Vec3 center,double radius){
        return p.level().getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(radius),
            t->t!=p&&t.isAlive()&&t.distanceToSqr(center)<=radius*radius&&canHurt(p,t));
    }
    private static void hurt(ServerPlayer p,LivingEntity t,float amount){
        if(t==null||!canHurt(p,t))return;
        t.invulnerableTime=0;
        CombatSettings.hurt(p,t,p.damageSources().indirectMagic(p,p),amount);
    }
    private static void fx(ServerPlayer p,int kind,Vec3 at,Vec3 dir,float size,int life){
        Network.send(p.serverLevel(),new Network.Effect(kind,p.getId(),at,dir,size,life,0));
    }
    private static boolean cursedSpirit(LivingEntity t){
        String id=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(t.getType()).getPath();
        return id.contains("curse")||id.contains("spirit")||id.contains("mahito")||id.contains("choso")
            ||t instanceof cn.blockforge.generated.nnnnr1201forge2.LowCurseEntity;
    }
    private static boolean casting(LivingEntity t){
        if(t instanceof ServerPlayer sp)return Skills.sealing(sp)||character.mainline.Mainline.domainSkill(sp,0)&&false;
        return t.getPersistentData().getLong("NpcChannelUntil")>t.level().getGameTime();
    }

    // ------------------------------------------------------------ 施法入口
    public static void cast(ServerPlayer p,int skill){
        if(!(p.level() instanceof ServerLevel level))return;
        switch(skill){
            case KurisuSkills.BAPTISM -> {addSanity(p,-6);baptism(p);}
            case KurisuSkills.FLAME -> {addSanity(p,-2);flame(p,level);}
            case KurisuSkills.SPEARS -> {addSanity(p,-2);spears(p);}
            case KurisuSkills.GATE -> {addSanity(p,-1);gate(p,level);}
            case KurisuSkills.SWORD -> sword(p);
            case KurisuSkills.THUNDER -> thunder(p);
            case KurisuSkills.STORM -> {addSanity(p,-10);storm(p);}
            case KurisuSkills.EXECUTE -> execute(p);
            case KurisuSkills.HORN -> {addSanity(p,-5);horn(p);}
            case KurisuSkills.SUPPER -> {addSanity(p,-15);supper(p);}
            case KurisuSkills.DOMAIN -> {addSanity(p,-8);domain(p,level);}
            default -> {}
        }
    }

    // 1 剥夺洗礼：金色细线，解除全部增益 + 15% 最大生命真伤
    private static void baptism(ServerPlayer p){
        LivingEntity t=aimTarget(p,28);
        Vec3 from=p.getEyePosition(),to=t==null?aimPoint(p,28):t.getBoundingBox().getCenter();
        fx(p,KurisuFx.BEAM,from,to,1.6f,8);
        p.level().playSound(null,p.blockPosition(),SoundEvents.BEACON_DEACTIVATE,SoundSource.PLAYERS,1.2f,2f);
        if(t==null)return;
        for(var eff:List.copyOf(t.getActiveEffects()))t.removeEffect(eff.getEffect());
        if(t instanceof ServerPlayer sp){character.gojo.Infinity.sever(sp);DomainAmplification.stop(sp);}
        t.invulnerableTime=0;
        CombatSettings.hurt(p,t,p.damageSources().indirectMagic(p,p),Math.max(1,t.getMaxHealth()*.15f));
        fx(p,KurisuFx.MARK,t.getBoundingBox().getCenter(),Vec3.ZERO,1.2f,10);
        p.displayClientMessage(Component.literal("剥夺洗礼：增益已解除").withStyle(net.minecraft.ChatFormatting.GOLD),true);
    }

    // 2 赎罪之炎：金色火海，持续掉血 + 禁疗
    private static void flame(ServerPlayer p,ServerLevel level){
        Vec3 at=aimPoint(p,6);
        FLAMES.add(new Flame(level,at,level.getGameTime()+200,p));
        fx(p,KurisuFx.FLAME,at,Vec3.ZERO,6f,40);
        level.playSound(null,net.minecraft.core.BlockPos.containing(at),SoundEvents.FIRECHARGE_USE,SoundSource.PLAYERS,1.4f,.7f);
    }

    // 3 圣光穿刺：追踪空中目标，打落
    private static void spears(ServerPlayer p){
        int n=0;
        for(var t:around(p,p.position(),24)){
            boolean air=!t.onGround()||t.isNoGravity()||t.getY()>p.getY()+2;
            if(!air)continue;
            fx(p,KurisuFx.SPEAR,t.position(),Vec3.ZERO,1.2f,10);
            t.setNoGravity(false);t.setDeltaMovement(t.getDeltaMovement().add(0,-1.6,0));t.hurtMarked=true;
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100,4));
            hurt(p,t,Math.max(8,t.getMaxHealth()*.08f));
            n++;
        }
        p.displayClientMessage(Component.literal("圣光穿刺 ×"+n).withStyle(net.minecraft.ChatFormatting.GOLD),true);
    }

    // 4 天国之门·断章：金色法阵吞弹道
    private static void gate(ServerPlayer p,ServerLevel level){
        Vec3 at=p.getEyePosition().add(p.getLookAngle().scale(2.4));
        GATES.add(new Gate(level,at,level.getGameTime()+160,p));
        fx(p,KurisuFx.GATE,at,Vec3.ZERO,2.2f,40);
        level.playSound(null,p.blockPosition(),SoundEvents.BELL_BLOCK,SoundSource.PLAYERS,1.6f,.8f);
    }

    // 5 裁决光剑
    private static void sword(ServerPlayer p){
        Curse.status(p,K_SWORD,300);
        fx(p,KurisuFx.SWORD,p.getEyePosition(),Vec3.ZERO,1.4f,20);
        p.level().playSound(null,p.blockPosition(),SoundEvents.ANVIL_USE,SoundSource.PLAYERS,1.0f,1.4f);
        p.displayClientMessage(Component.literal("裁决光剑：15 秒（无视护甲 · 对咒灵/受肉双倍）").withStyle(net.minecraft.ChatFormatting.GOLD),true);
    }

    // 6 神罚·落雷
    private static void thunder(ServerPlayer p){
        Vec3 at=aimPoint(p,30);
        fx(p,KurisuFx.THUNDER,at,Vec3.ZERO,3f,14);
        p.level().playSound(null,net.minecraft.core.BlockPos.containing(at),SoundEvents.LIGHTNING_BOLT_THUNDER,SoundSource.PLAYERS,2f,.9f);
        for(var t:around(p,at,3.5)){
            hurt(p,t,Math.max(30,t.getMaxHealth()*.12f));
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40,5));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,40,2));
        }
    }

    // 7 净化风暴：击退 + 清空全部效果
    private static void storm(ServerPlayer p){
        fx(p,KurisuFx.STORM,p.position(),Vec3.ZERO,8f,20);
        p.level().playSound(null,p.blockPosition(),SoundEvents.BEACON_ACTIVATE,SoundSource.PLAYERS,1.6f,1.2f);
        for(var t:around(p,p.position(),8)){
            for(var eff:List.copyOf(t.getActiveEffects()))t.removeEffect(eff.getEffect());
            hurt(p,t,Math.max(12,t.getMaxHealth()*.1f));
            Vec3 push=t.position().subtract(p.position()).multiply(1,0,1);
            if(push.lengthSqr()>.01)t.push(push.normalize().x*1.4,.5,push.normalize().z*1.4);
        }
    }

    // 8 破魔·重击
    private static void execute(ServerPlayer p){
        LivingEntity t=aimTarget(p,3.5);
        if(t==null){p.displayClientMessage(Component.literal("破魔：前方 3 格内没有目标").withStyle(net.minecraft.ChatFormatting.GRAY),true);return;}
        fx(p,KurisuFx.EXECUTE,t.getBoundingBox().getCenter(),Vec3.ZERO,2.4f,14);
        p.level().playSound(null,t.blockPosition(),SoundEvents.PLAYER_ATTACK_CRIT,SoundSource.PLAYERS,1.6f,.7f);
        boolean open=!casting(t);
        CombatSettings.hurt(p,t,p.damageSources().indirectMagic(p,p),open?Math.max(40,t.getMaxHealth()*.25f):Math.max(10,t.getMaxHealth()*.08f));
        p.displayClientMessage(Component.literal(open?"破魔·重击：处决！":"破魔：目标正在施术，伤害削减").withStyle(net.minecraft.ChatFormatting.GOLD),true);
    }

    // 9 邪去悔天梯（号角）：3 秒圣光柱
    private static void horn(ServerPlayer p){
        HORNS.put(p.getUUID(),p.level().getGameTime()+HORN_TICKS);
        fx(p,KurisuFx.HORN,p.getEyePosition(),Vec3.ZERO,6f,20);
        p.level().playSound(null,p.blockPosition(),SoundEvents.RAID_HORN.get(),SoundSource.PLAYERS,1.4f,.9f);
        p.displayClientMessage(Component.literal("邪去悔天梯：吹奏中（移速 -50%）").withStyle(net.minecraft.ChatFormatting.GOLD),true);
    }

    // 10 极之番·最后的晚餐
    private static void supper(ServerPlayer p){
        Curse.status(p,"KurisuSupperCharge",60);
        p.displayClientMessage(Component.literal("最后的晚餐：蓄力 3 秒…").withStyle(net.minecraft.ChatFormatting.GOLD),true);
        fx(p,KurisuFx.SIGIL,p.position(),Vec3.ZERO,6f,60);
    }

    // 11 领域展开·天国之门
    private static void domain(ServerPlayer p,ServerLevel level){
        Vec3 center=p.position();
        long end=level.getGameTime()+DOMAIN_TICKS;
        final Domain d=new Domain(p,level,center,end);
        DOMAINS.add(d);
        var life=new DomainClash.Lifecycle(){
            public boolean active(){return DOMAINS.contains(d);}
            public void extend(int ticks){}
            public void close(){DOMAINS.remove(d);}
        };
        DomainClash.opened(p,center,DOMAIN_RADIUS,life);
        fx(p,KurisuFx.DOMAIN,center,Vec3.ZERO,DOMAIN_RADIUS,DOMAIN_TICKS);
        p.displayClientMessage(Component.literal("领域展开 · 天国之门（半径 "+DOMAIN_RADIUS+" · 封印生得术式）").withStyle(net.minecraft.ChatFormatting.GOLD),false);
    }

    /** 领域内敌人的生得术式被封印（CharacterCasting.cast 会先查这里）。 */
    public static boolean sealed(Player p){
        if(p==null)return false;
        long now=p.level().getGameTime();
        for(var d:DOMAINS)if(d.end()>now&&d.level()==p.level()&&d.owner()!=p&&p.position().distanceToSqr(d.center())<=DOMAIN_RADIUS*DOMAIN_RADIUS)return true;
        return false;
    }

    // ------------------------------------------------------------ 事件
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;
        long now=p.level().getGameTime();
        var data=Curse.data(p);
        // 失控：无差别攻击 + 女仆净化
        if(data.getLong(K_BERSERK)>now){
            if(now%10==0){
                for(var t:around(p,p.position(),6)){
                    if(MaidBridge.isMaid(t)&&!t.getPersistentData().getBoolean("KurisuPurified")){
                        t.getPersistentData().putBoolean("KurisuPurified",true);
                        t.hurt(t.damageSources().magic(),100000f);
                        p.displayClientMessage(Component.literal("失控：身边的女仆被净化，契约解除").withStyle(net.minecraft.ChatFormatting.RED),false);
                        continue;
                    }
                    CombatSettings.hurt(p,t,p.damageSources().indirectMagic(p,p),10);
                }
            }
        }
        // 号角持续
        Long until=HORNS.get(p.getUUID());
        if(until!=null){
            if(until<=now||!p.isAlive()){HORNS.remove(p.getUUID());}
            else{
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,10,1,false,false));
                if(now%4==0){
                    Vec3 at=aimPoint(p,20);
                    fx(p,KurisuFx.HORN,at,Vec3.ZERO,4f,6);
                    for(var t:around(p,at,4))hurt(p,t,Math.max(6,t.getMaxHealth()*.03f));
                }
            }
        }
        // 最后的晚餐引爆
        if(data.contains("KurisuSupperAt")&&data.getLong("KurisuSupperAt")<=now){
            data.remove("KurisuSupperAt");
            fx(p,KurisuFx.SUPPER,p.position(),Vec3.ZERO,8f,30);
            for(var t:around(p,p.position(),64)){
                if(t instanceof Mob m&&(m.getPersistentData().getBoolean("CullingSummon")||m.getPersistentData().getBoolean("TenShadows")||m.getPersistentData().getBoolean("ShadowSummon"))){m.discard();continue;}
                fx(p,KurisuFx.SUPPER,t.position(),Vec3.ZERO,2.2f,12);
                CombatSettings.hurt(p,t,p.damageSources().indirectMagic(p,p),Math.max(10,t.getMaxHealth()*.2f));
            }
        }
        // 火海
        FLAMES.removeIf(f->{
            if(f.level()!=p.level()||f.end()<=now)return true;
            if(now%20==0){
                fx(p,KurisuFx.FLAME,f.center(),Vec3.ZERO,6f,20);
                for(var t:around(p,f.center(),6)){
                    CombatSettings.hurt(p,t,p.damageSources().indirectMagic(p,p),Math.max(3,t.getMaxHealth()*.03f));
                    t.getPersistentData().putLong(K_NOHEAL,t.level().getGameTime()+20);
                }
            }
            return false;
        });
        GATES.removeIf(g->g.end()<=now||g.level()!=p.level());
        DOMAINS.removeIf(d->d.end()<=now||!d.owner().isAlive()||d.level()!=p.level());
        if(now%20==0)for(var d:DOMAINS)fx(p,KurisuFx.DOMAIN,d.center(),Vec3.ZERO,DOMAIN_RADIUS,20);
        if(Curse.active(p,"KurisuSupperCharge")&&!data.contains("KurisuSupperAt"))data.putLong("KurisuSupperAt",now+60);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST) public static void hurtEvent(LivingHurtEvent e){
        var t=e.getEntity();if(t.level().isClientSide)return;
        if(e.getSource().getEntity() instanceof ServerPlayer p&&character.CharacterService.selected(p)!=null
            &&"kurisu".equals(character.CharacterService.selected(p).id())){
            boolean sword=Curse.active(p,K_SWORD);
            boolean dom=inOwnDomain(p,t)&&p.distanceToSqr(t)<=9;
            if(sword||dom){
                float mul=sword?(cursedSpirit(t)||t instanceof Mob?2f:1.6f):2f;
                e.setAmount(e.getAmount()*mul);
            }
        }
    }
    private static boolean inOwnDomain(ServerPlayer p,LivingEntity t){
        for(var d:DOMAINS)if(d.owner()==p&&d.end()>p.level().getGameTime()&&t.position().distanceToSqr(d.center())<=DOMAIN_RADIUS*DOMAIN_RADIUS)return true;
        return false;
    }

    @SubscribeEvent public static void heal(LivingHealEvent e){
        if(e.getEntity().level().isClientSide)return;
        if(e.getEntity().getPersistentData().getLong(K_NOHEAL)>e.getEntity().level().getGameTime())e.setCanceled(true);
    }

    @SubscribeEvent public static void sweets(LivingEntityUseItemEvent.Finish e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        String id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(e.getItem().getItem()).getPath();
        if(id.equals("cake")||id.equals("cookie")||id.equals("sweet_berries")||id.contains("candy")||id.equals("honey_bottle"))
            addSanity(p,25);
    }

    @SubscribeEvent public static void impact(ProjectileImpactEvent e){
        if(!(e.getProjectile().level() instanceof ServerLevel level)||level.isClientSide)return;
        for(var g:GATES)if(g.level()==level&&g.end()>level.getGameTime()
            &&e.getProjectile().position().distanceToSqr(g.center())<=9){
            e.setCanceled(true);e.getProjectile().discard();
            Network.send(level,new Network.Effect(KurisuFx.GATE,g.owner().getId(),g.center(),Vec3.ZERO,2.2f,10,0));
            return;
        }
    }

    @SubscribeEvent public static void attack(AttackEntityEvent e){
        if(e.getEntity() instanceof ServerPlayer p&&HORNS.getOrDefault(p.getUUID(),0L)>p.level().getGameTime()){
            e.setCanceled(true);
            p.displayClientMessage(Component.literal("吹奏号角时无法普通攻击"),true);
        }
    }

    public static void clear(){
        FLAMES.clear();GATES.clear();DOMAINS.clear();HORNS.clear();
    }
    private KurisuCombat(){}
}
