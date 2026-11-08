package cn.blockforge.generated.nnnnr1201forge2;

import java.util.List;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;

public final class TechniqueEffects {
    public static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(BuiltInRegistries.MOB_EFFECT,"jujutsu_kaisen_cursed_paths");
    public static final DeferredHolder<MobEffect, MobEffect> BURNOUT=EFFECTS.register("technique_burnout",()->new SimpleEffect(MobEffectCategory.HARMFUL,0x853343));
    /** 天逆蛑造成的 3 秒「术式封印」：只负责显示与说明，真正的封锁判定在 {@link Curse#canUseTechnique}。 */
    public static final DeferredHolder<MobEffect, MobEffect> TECHNIQUE_SEAL=EFFECTS.register("technique_seal",()->new SimpleEffect(MobEffectCategory.HARMFUL,0xCFF3FF));
    /** 1.21：MobEffect 构造是 protected，需要子类。 */
    private static final class SimpleEffect extends MobEffect {
        SimpleEffect(MobEffectCategory category,int color){super(category,color);}
    }
    private TechniqueEffects(){}
}
