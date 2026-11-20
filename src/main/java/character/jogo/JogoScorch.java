package character.jogo;

import cn.blockforge.generated.nnnnr1201forge2.BalanceRules;
import cn.blockforge.generated.nnnnr1201forge2.CombatSettings;
import cn.blockforge.generated.nnnnr1201forge2.ProgressionContent;
import cn.blockforge.generated.nnnnr1201forge2.SkillDestruction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/**
 * 极之番·陨 的地形：
 * ①砸出碗状陨石坑（半径 15，中心最深 9 格），坑底与坑缘铺「熔岩焦土」；
 * ②坑心（半径 10）灌一层岩浆海，持续 15 秒，期间任何进入的敌人被重度灼烧；
 * ③15 秒后岩浆冷却成熔岩焦土（永久保留）。
 * <p>准入复用 {@link SkillDestruction#canMove}（领地/保护方块/领域/方块实体全部跳过，但不看 desblock 开关，
 * 因为这是极之番的招牌演出，和「断流」同类处理）。逐帧分批施工，避免单刻改几千方块卡服。
 */
public final class JogoScorch {
    private static final int PER_TICK=420;
    private static final int SEA_BURN_TICKS=10;
    private static final float SEA_BURN_DAMAGE=10f;

    private record Job(ServerPlayer owner,BlockPos pos,BlockState state,boolean placing) {}
    private record Pool(ServerLevel level,ServerPlayer owner,Vec3 center,double radius,long endAt,List<BlockPos> lava) {}
    private static final ArrayDeque<Job> JOBS=new ArrayDeque<>();
    private static final List<Pool> POOLS=new ArrayList<>();

    /** 陨石落地：排入全部地形改动 + 建立岩浆海。 */
    public static void impact(ServerPlayer owner,ServerLevel level,Vec3 target,int radius,int seaRadius,int seaTicks){
        if(owner==null||level==null||radius<1)return;
        int cx=(int)Math.floor(target.x),cy=(int)Math.floor(target.y),cz=(int)Math.floor(target.z);
        BlockState scorch=ProgressionContent.LAVA_SCORCH.get().defaultBlockState();
        double rr=radius*radius;
        List<BlockPos> lava=new ArrayList<>();
        for(int dx=-radius;dx<=radius;dx++){
            for(int dz=-radius;dz<=radius;dz++){
                double d2=dx*dx+dz*dz;
                if(d2>rr)continue;
                int x=cx+dx,z=cz+dz;
                if(!level.hasChunkAt(new BlockPos(x,cy,z)))continue;
                double d=Math.sqrt(d2);
                int depth=(int)Math.round(9.0*(1.0-d/radius));
                int bottom=cy-depth;
                if(depth<=0){
                    // 坑缘：把表层烧成焦土，形成一个明显的焦黑圈
                    queue(owner,level,new BlockPos(x,cy,z),scorch,true);
                    continue;
                }
                for(int y=cy;y>bottom;y--){
                    BlockPos pos=new BlockPos(x,y,z);
                    if(!level.getBlockState(pos).isAir())queue(owner,level,pos,Blocks.AIR.defaultBlockState(),false);
                }
                BlockPos floor=new BlockPos(x,bottom,z);
                queue(owner,level,floor,scorch,true);
                if(d<=seaRadius){
                    // 坑底上一层灌岩浆：这块此刻还是实心（挖坑任务还没执行），所以不能在这里判空气
                    BlockPos pool=new BlockPos(x,bottom+1,z);
                    JOBS.add(new Job(owner,pool,Blocks.LAVA.defaultBlockState(),true));
                    lava.add(pool);
                }
            }
        }
        if(!lava.isEmpty()){
            long now=level.getGameTime();
            POOLS.add(new Pool(level,owner,new Vec3(cx+0.5,cy,cz+0.5),seaRadius,now+Math.max(20,seaTicks),lava));
        }
    }

    private static void queue(ServerPlayer owner,ServerLevel level,BlockPos pos,BlockState state,boolean placing){
        if(state.isAir()&&level.getBlockState(pos).isAir())return;
        JOBS.add(new Job(owner,pos,state,placing));
    }

    /** 每刻：先分批把地形改动铺完，再结算岩浆海的灼烧/冷却。 */
    public static void tick(){
        int budget=PER_TICK;
        while(budget-->0&&!JOBS.isEmpty()){
            Job job=JOBS.poll();
            ServerPlayer owner=job.owner();
            if(owner==null||!owner.isAlive())continue;
            BlockPos pos=job.pos();
            ServerLevel level=owner.serverLevel();
            if(!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos))continue;
            BlockState current=level.getBlockState(pos);
            boolean allowed=job.placing()&&current.isAir()
                ? level.mayInteract(owner,pos)
                : SkillDestruction.canMove(owner,pos);
            if(!allowed)continue;
            level.setBlock(pos,job.state(),2);
        }
        if(POOLS.isEmpty())return;
        var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if(server==null)return;
        for(Iterator<Pool> it=POOLS.iterator();it.hasNext();){
            Pool pool=it.next();
            ServerLevel level=pool.level();
            long now=level.getGameTime();
            if(now>=pool.endAt()){
                // 岩浆冷却：整片池子凝成熔岩焦土
                BlockState scorch=ProgressionContent.LAVA_SCORCH.get().defaultBlockState();
                for(BlockPos pos:pool.lava()){
                    if(level.hasChunkAt(pos)&&level.getBlockState(pos).is(Blocks.LAVA))level.setBlock(pos,scorch,2);
                }
                it.remove();
                continue;
            }
            if(now%SEA_BURN_TICKS!=0)continue;
            ServerPlayer owner=pool.owner();
            if(owner==null||!owner.isAlive()||owner.level()!=level)continue;
            float damage=BalanceRules.modpackMode()?SEA_BURN_DAMAGE*30f:SEA_BURN_DAMAGE;
            for(var target:level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(pool.center(),pool.center()).inflate(pool.radius()+1,7,pool.radius()+1),
                    t->t!=owner&&t.isAlive()&&CombatSettings.canHit(owner,t))){
                double dx=target.getX()-pool.center().x,dz=target.getZ()-pool.center().z;
                if(dx*dx+dz*dz>pool.radius()*pool.radius())continue;
                if(Math.abs(target.getY()-pool.center().y)>6)continue;
                cn.blockforge.generated.nnnnr1201forge2.AttackTypes.mark(cn.blockforge.generated.nnnnr1201forge2.AttackTypes.MAGIC);
                try{CombatSettings.hurt(owner,target,owner.damageSources().indirectMagic(owner,owner),damage);}
                finally{cn.blockforge.generated.nnnnr1201forge2.AttackTypes.clear();}
                target.setSecondsOnFire(5);
            }
        }
    }

    /** 关服兜底：清掉未铺完的队列与岩浆池记录（已改了的地形保持原样，属于终局技能的正常结果）。 */
    public static void reset(){
        JOBS.clear();POOLS.clear();
    }
    private JogoScorch() {}
}
