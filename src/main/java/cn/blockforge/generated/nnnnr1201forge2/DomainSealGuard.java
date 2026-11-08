package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** 御厨子的结印是"双手解印"，结印期间不能出手：取消命中实体的攻击（空挥在客户端 {@link CastHands#interaction} 里取消）。 */
@net.neoforged.fml.common.EventBusSubscriber(modid="jujutsu_kaisen_cursed_paths")
public final class DomainSealGuard {
    @SubscribeEvent public static void attack(AttackEntityEvent event){
        if(event.getEntity() instanceof ServerPlayer p&&Skills.sealing(p))event.setCanceled(true);
    }
    private DomainSealGuard(){}
}
