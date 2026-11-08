package cn.blockforge.generated.nnnnr1201forge2;

import java.util.List;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;

public final class TechniqueEffects {
    public static final DeferredRegister<Holder<MobEffect>> EFFECTS=DeferredRegister.create(BuiltInRegistries.MOB_EFFECTS,"jujutsu_kaisen_cursed_paths");
    public static final DeferredHolder<Holder<MobEffect>, Holder<MobEffect>> BURNOUT=EFFECTS.register("technique_burnout",()->new Holder<MobEffect>(MobEffectCategory.HARMFUL,0x853343){
        @Override public List<ItemStack> getCurativeItems(){return List.of();}
    });
    /** 天逆蛑造成的 3 秒「术式封印」：只负责显示与说明，真正的封锁判定在 {@link Curse#canUseTechnique}。 */
    public static final DeferredHolder<Holder<MobEffect>, Holder<MobEffect>> TECHNIQUE_SEAL=EFFECTS.register("technique_seal",()->new Holder<MobEffect>(MobEffectCategory.HARMFUL,0xCFF3FF){
        @Override public List<ItemStack> getCurativeItems(){return List.of();}
    });
    private TechniqueEffects(){}
}
