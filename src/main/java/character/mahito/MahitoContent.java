package character.mahito;

import cn.blockforge.generated.nnnnr1201forge2.Content;
import cn.blockforge.generated.nnnnr1201forge2.ExpeditionContent;
import cn.blockforge.generated.nnnnr1201forge2.ProgressionContent;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;

public final class MahitoContent {
    public static final DeferredHolder<EntityType<?>, EntityType<MahitoPuppetEntity>> PUPPET=Content.ENTITIES.register("mahito_puppet",()->EntityType.Builder.of(MahitoPuppetEntity::new,MobCategory.MONSTER).sized(.8f,1.9f).clientTrackingRange(8).updateInterval(3).build("jujutsu_kaisen_cursed_paths:mahito_puppet"));
    public static final DeferredHolder<Holder<MobEffect>, Holder<MobEffect>> BLEEDING=ExpeditionContent.EFFECTS.register("mahito_bleeding",()->new Holder<MobEffect>(MobEffectCategory.HARMFUL,0x9c3545){
        @Override public boolean isDurationEffectTick(int duration,int amplifier){return duration%20==0;}
        @Override public void applyEffectTick(LivingEntity entity,int amplifier){entity.hurt(entity.damageSources().magic(),1+amplifier);}
    });
    public static final DeferredHolder<Block, Block> FLESH=ProgressionContent.BLOCKS.register("mahito_flesh",()->new Block(BlockBehaviour.Properties.copy(Blocks.SLIME_BLOCK).strength(3).speedFactor(.85f).lightLevel(s->4)));
    public static final DeferredHolder<Block, Block> FACES=ProgressionContent.BLOCKS.register("mahito_faces",()->new Block(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN).strength(50,3600000).lightLevel(s->7)));
    public static void init(net.neoforged.bus.api.IEventBus bus){bus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event)->event.put(PUPPET.get(),MahitoPuppetEntity.attributes().build()));}
    private MahitoContent(){}
}
