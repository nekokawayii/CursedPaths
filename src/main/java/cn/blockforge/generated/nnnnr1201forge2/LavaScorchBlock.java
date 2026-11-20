package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 「熔岩焦土」：漏壶极之番·陨 砸出来的坑底/坑缘方块。
 * 踩上去持续扣血并被点燃（同一生物 20 刻内只结算一次），偶尔冒火星。
 */
public final class LavaScorchBlock extends Block {
    /** 每次踩踏的伤害（普通 4，整合包模式下按 30 倍算，和首领血量口径一致）。 */
    private static final float STEP_DAMAGE=4f;

    public LavaScorchBlock(Properties properties){super(properties);}

    @Override public void stepOn(Level level,BlockPos pos,BlockState state,Entity entity){
        super.stepOn(level,pos,state,entity);
        if(level.isClientSide||!(entity instanceof LivingEntity living))return;
        long now=level.getGameTime();
        var data=living.getPersistentData();
        if(now<data.getLong("LavaScorchNext"))return;
        data.putLong("LavaScorchNext",now+20);
        float damage=BalanceRules.modpackMode()?STEP_DAMAGE*30f:STEP_DAMAGE;
        if(living instanceof net.minecraft.world.entity.player.Player p&&p.isCreative())return;
        living.hurt(level.damageSources().hotFloor(),damage);
        living.setSecondsOnFire(3);
    }

    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(random.nextInt(6)==0)level.sendParticles(ParticleTypes.LAVA,pos.getX()+0.5,pos.getY()+1.02,pos.getZ()+0.5,
            1,random.nextDouble()*.4,0,random.nextDouble()*.4,0);
    }

}
