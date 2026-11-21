package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 咒力附拳期间的黑闪标记（用户要求）：
 *  <ul>
 *    <li>附拳开启时，**每 0.5 秒**都有 **1%** 概率，在周围 **5 格**内所有敌人的**随机位置**闪一个
 *        **半径 0.3 格、持续 0.7 秒的红圈**，且**始终画在生物之上**（关深度测试，最高优先级）。</li>
 *    <li>如果玩家在有效时间内**准星对准红圈 + 按左键**：**瞬间跳到敌人身前**打出黑闪，
 *        伤害 = **本次普攻伤害的 3 次方**（封顶 {@link #MAX_DAMAGE}）。</li>
 *  </ul>
 *  服务端负责掷骰、发标记与结算瞬移黑闪；客户端只管画红圈、瞄准判定与发按键包。 */
@Mod.EventBusSubscriber(modid="jujutsu_kaisen_cursed_paths")
public final class BlackFlashMarks {
    /** 特效号：客户端据此在指定点画红圈（position=点，owner=敌人 id，life=持续时间刻）。 */
    public static final int FX_MARK=237;
    public static final double RANGE=5.0,MARK_RADIUS=0.3;
    public static final int MARK_TICKS=20;      // 1 秒（用户指定）
    public static final int ROLL_TICKS=10;      // 0.5 秒
    public static final double CHANCE=0.05;     // 5%（用户指定）
    public static final double MAX_DAMAGE=1.0e7;
    /** 瞬移到敌人身前后的无敌帧（刻）。 */
    public static final int INVULN_TICKS=20;

    /** 服务端：玩家 -> 有效标记（敌人 id -> 到期刻）。 */
    private static final Map<UUID,Map<Integer,Long>> MARKS=new HashMap<>();

    private record ServerMark(UUID entity,Vec3 offset,long until,boolean extended){}
    public static void extendBloodVision(ServerPlayer p){var points=POINTS.get(p.getUUID());var mine=MARKS.get(p.getUUID());if(points==null||mine==null)return;long now=p.level().getGameTime();for(var entry:List.copyOf(points.entrySet())){ServerMark m=entry.getValue();if(m.extended||m.until<now)continue;if(!(p.level().getEntity(entry.getKey()) instanceof LivingEntity target)||!m.entity.equals(target.getUUID())||!CombatSafety.canTarget(p,target))continue;long until=m.until+40;points.put(entry.getKey(),new ServerMark(m.entity,m.offset,until,true));mine.put(entry.getKey(),until);Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->p),new Network.Effect(FX_MARK,target.getId(),target.position().add(m.offset),Vec3.ZERO,1,(int)(until-now),0));}}
    private static final Map<UUID,Map<Integer,ServerMark>> POINTS=new HashMap<>();
    /** 穿血使用服务端生成的红圈位置，不能仅因目标有红圈就算弱点。 */
    public static boolean bloodWeakPoint(ServerPlayer p,LivingEntity target,Vec3 start,Vec3 end){
        var marks=POINTS.get(p.getUUID());ServerMark mark=marks==null?null:marks.get(target.getId());
        if(mark==null||!mark.entity.equals(target.getUUID())||mark.until<p.level().getGameTime())return false;
        Vec3 point=target.position().add(mark.offset),line=end.subtract(start);double length=line.lengthSqr();
        if(length<1e-8)return false;double t=point.subtract(start).dot(line)/length;
        return t>=0&&t<=1&&point.distanceToSqr(start.add(line.scale(t)))<=MARK_RADIUS*MARK_RADIUS;
    }
    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event){MARKS.remove(event.getEntity().getUUID());POINTS.remove(event.getEntity().getUUID());}
    @SubscribeEvent public static void dimension(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event){MARKS.remove(event.getEntity().getUUID());POINTS.remove(event.getEntity().getUUID());}
    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event){MARKS.clear();POINTS.clear();}
    // ------------------------------------------------------------------ 服务端
    /** 独立的服务端每刻钩子：**每刻都跑**，内部再按 10 刻（0.5 秒）掷骰。
     *  （之前挂在附拳每 15 刻的广播循环里，和 10 刻取最小公倍数 → 实际 30 刻才掷一次，看起来像只出现一次。） */
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if(server==null)return;
        for(ServerPlayer p:server.getPlayerList().getPlayers())tick(p);
    }
    /** 每 0.5 秒掷一次骰子：附拳开启 + 半径 5 格内有敌人时，1% 概率给**每个**敌人一个随机点标记。 */
    public static void tick(ServerPlayer p){
        if(p.level().getGameTime()%ROLL_TICKS!=0)return;
        if(!Curse.data(p).getBoolean("Fist"))return;
        MARKS.values().forEach(m->m.entrySet().removeIf(e->p.level().getGameTime()>e.getValue()));
        if(p.getRandom().nextDouble()>=CHANCE*character.culling.CullingRules.flashChanceMul(p))return;
        AABB box=p.getBoundingBox().inflate(RANGE);
        List<LivingEntity> enemies=((ServerLevel)p.level()).getEntitiesOfClass(LivingEntity.class,box,
            e->e!=p&&e.isAlive()&&e.distanceToSqr(p)<=RANGE*RANGE&&CombatSettings.canHit(p,e));
        if(enemies.isEmpty())return;
        Map<Integer,Long> mine=MARKS.computeIfAbsent(p.getUUID(),k->new HashMap<>());
        int duration=BloodRules.markTicks(BloodArts.vision(p));long until=p.level().getGameTime()+duration;
        for(LivingEntity e:enemies){
            AABB b=e.getBoundingBox();
            Vec3 at=new Vec3(b.minX+p.getRandom().nextDouble()*b.getXsize(),
                             b.minY+p.getRandom().nextDouble()*b.getYsize(),
                             b.minZ+p.getRandom().nextDouble()*b.getZsize());
            mine.put(e.getId(),until);
            var points=POINTS.computeIfAbsent(p.getUUID(),k->new HashMap<>());points.entrySet().removeIf(entry->entry.getValue().until<p.level().getGameTime());
            points.put(e.getId(),new ServerMark(e.getUUID(),at.subtract(e.position()),until,BloodArts.vision(p)));
            Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->p),
                new Network.Effect(FX_MARK,e.getId(),at,Vec3.ZERO,1,duration,0));
            SkillSounds.play(p,"domainshatter",3.5f);   // 红圈出现音效（声音要大）
        }
    }

    /** 服务端：玩家在有效时间内瞄准并左键 → 瞬移到敌人身前，打出"普攻伤害³"的黑闪。 */
    public static void strike(ServerPlayer p,int targetId){
        if(ThousandMileChainItem.isChain(p.getMainHandItem())||ThousandMileChain.bound(p)||Curse.severed(p))return;
        var log=com.mojang.logging.LogUtils.getLogger();
        log.info("[黑闪] 服务端收到 strike target={} marks={}",targetId,MARKS.containsKey(p.getUUID())?MARKS.get(p.getUUID()).size():-1);
        Map<Integer,Long> mine=MARKS.get(p.getUUID());
        if(mine==null||!mine.containsKey(targetId)){log.info("[黑闪] 服务端拒绝：没有该标记 target={}",targetId);return;}
        if(p.level().getGameTime()>mine.getOrDefault(targetId,0L)){log.info("[黑闪] 服务端拒绝：标记已过期 target={}",targetId);mine.remove(targetId);return;}
        if(!(p.level().getEntity(targetId) instanceof LivingEntity target)||!target.isAlive()){mine.remove(targetId);return;}
        if(!CombatSafety.canAffect(p,target))return;
        if(target.distanceToSqr(p)>RANGE*RANGE*4)return;                       // 距离兜底
        mine.remove(targetId);
        log.info("[黑闪] 结算 target={} 伤害={}",targetId,Math.min(MAX_DAMAGE,Math.pow(Math.max(1,p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)),3)));
        // 跳到敌人身前：沿"玩家→敌人"方向退 1.2 格，朝敌人面朝
        Vec3 dir=target.position().subtract(p.position());
        if(dir.lengthSqr()<1e-4)dir=p.getLookAngle();
        dir=dir.normalize();
        Vec3 at=target.position().subtract(dir.scale(1.2));
        p.connection.teleport(at.x,target.getY(),at.z,p.getYRot(),p.getXRot());
        float yaw=(float)(Math.toDegrees(Math.atan2(-dir.x,dir.z)));
        p.connection.teleport(at.x,target.getY(),at.z,yaw,0f);
        p.invulnerableTime=INVULN_TICKS;                          // 无敌帧（用户要求）
        // 伤害 = 本次普攻伤害的 3 次方（封顶）
        double base=Math.max(1,p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
        double damage=Math.min(MAX_DAMAGE,Math.pow(base,3+character.culling.CullingRules.flashPowerAdd(p)));
        AttackTypes.mark(AttackTypes.LIGHTNING);
        CombatSettings.hurt(p,target,target.damageSources().playerAttack(p),(float)damage);
        fx(p,target.getEyePosition(),dir);
    }
    private static void fx(ServerPlayer p,Vec3 at,Vec3 dir){
        Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->p),
            new Network.Effect(85,p.getId(),at,dir,1,16,p.getId()));
        SkillSounds.play(p,"black_flash",3.5f);   // 打击感：音效更响
    }

    // ------------------------------------------------------------------ 客户端
    // 客户端部分已拆到 BlackFlashClient（专用服务端不加载）

    private BlackFlashMarks(){}
}
